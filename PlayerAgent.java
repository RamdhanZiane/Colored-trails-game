import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

public class PlayerAgent extends Agent {
    private int posX, posY;
    private int destX, destY;
    private int stuckCount = 0;
    private int consecutiveStuckTurns = 0;
    private boolean reachedDestination = false;
    private static final int GRID_SIZE = 5;
    private char[][] grid = {
        {'A', 'B', 'C', 'D', 'E'},
        {'B', 'C', 'D', 'E', 'A'},
        {'C', 'D', 'E', 'A', 'B'},
        {'D', 'E', 'A', 'B', 'C'},
        {'E', 'A', 'B', 'C', 'D'}
    };
    private Map<Character, Integer> tickets = new HashMap<>();
    private Random rand = new Random();
    private String[] playersList;

    @Override
    protected void setup() {
        if(getAID().getLocalName().equals("player1")) {
            tickets = new HashMap<>();
            tickets.put('A', 12);
            tickets.put('B', 0);
            tickets.put('C', 0);
            tickets.put('D', 0);
            tickets.put('E', 8);
            posX = 0;
            posY = 0;
            destX = 4;
            destY = 4;
            playersList = new String[]{"player2"};
        } else {
            tickets = new HashMap<>();
            tickets.put('A', 5);
            tickets.put('B', 2);
            tickets.put('C', 5);
            tickets.put('D', 2);
            tickets.put('E', 11);
            posX = 0;
            posY = 4;
            destX = 3;
            destY = 0;
            playersList = new String[]{"player1"};
        }
        Object[] args = getArguments();
        if (args != null && args.length == 6) {
            posX = (int) args[0];
            posY = (int) args[1];
            destX = (int) args[2];
            destY = (int) args[3];
            grid = (char[][]) args[4];
            tickets = (Map<Character, Integer>) args[5];
        }
        System.out.println("Player " + getAID().getName() + " is ready at position (" + posX + "," + posY + ")");

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    String content = msg.getContent();
                    if (content.startsWith("MOVE")) {
                        handleMove();
                    } else if (content.startsWith("OFFER")) {
                        handleOffer(msg);
                    } else if (content.startsWith("RESPONSE")) {
                        handleResponse(msg);
                    } else if (content.startsWith("UPDATE")) {
                        handleUpdate(msg);
                    } else if (content.startsWith("TERMINATE")) {
                        System.out.println(getAID().getName() + " received termination message.");
                        doDelete();
                    }
                } else {
                    // block();
                }
            }
        });
    }

    private void handleMove() {
        boolean moved = attemptStrategicMove();
        if (!moved) {
            consecutiveStuckTurns++;
            stuckCount++;
            System.out.println(getAID().getName() + " is stuck at (" + posX + "," + posY + ")");
        } else {
            consecutiveStuckTurns = 0;
        }

        if (posX == destX && posY == destY) {
            reachedDestination = true;
            System.out.println(getAID().getName() + " reached the destination!");
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setContent("TERMINATE," + getAID().getLocalName());
            msg.addReceiver(new AID("game", AID.ISLOCALNAME));
            send(msg);
        } else if (consecutiveStuckTurns >= 3) {
            System.out.println(getAID().getName() + " is stuck multiple times and will be terminated.");
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setContent("TERMINATE," + getAID().getLocalName());
            msg.addReceiver(new AID("game", AID.ISLOCALNAME));
            send(msg);
        } else {
            ACLMessage offerMsg = new ACLMessage(ACLMessage.PROPOSE);
            // choose one of the players randomly
            if (stuckCount > 0) {
                // check if i have any tickets
                boolean hasTickets = false;
                for (int count : tickets.values()) {
                    if (count > 0) {
                        hasTickets = true;
                        break;
                    }
                }
                if (hasTickets) {
                    offerMsg.setContent("OFFER," + generateRandomOffer());
                    offerMsg.addReceiver(new AID(playersList[rand.nextInt(playersList.length)], AID.ISLOCALNAME));
                    send(offerMsg);
                }
            }
        }
    }

    // private boolean attemptMove() {
    //     int[] dx = {-1, 1, 0, 0, -1, -1, 1, 1};
    //     int[] dy = {0, 0, -1, 1, -1, 1, -1, 1};
    //     for (int i = 0; i < dx.length; i++) {
    //         int newX = posX + dx[i];
    //         int newY = posY + dy[i];
    //         if (isValidMove(newX, newY)) {
    //             posX = newX;
    //             posY = newY;
    //             char color = grid[posX][posY];
    //             tickets.put(color, tickets.get(color) - 1);
    //             System.out.println(getAID().getName() + " moved to (" + posX + "," + posY + ")");
    //             return true;
    //         }
    //     }
    //     return false;
    // }

    private boolean attemptStrategicMove() {
        List<int[]> path = findPathToDestination();
        if (path.size() > 1) {
            int[] nextMove = path.get(1);
            int newX = nextMove[0];
            int newY = nextMove[1];
            if (isValidMove(newX, newY)) {
                posX = newX;
                posY = newY;
                char color = grid[posX][posY];
                tickets.put(color, tickets.get(color) - 1);
                System.out.println(getAID().getName() + " moved to (" + posX + "," + posY + ")");
                return true;
            }
        }
        return false;
    }

    private List<int[]> findPathToDestination() {
        PriorityQueue<Node> openList = new PriorityQueue<>(Comparator.comparingInt(node -> node.f));
        Set<Node> closedList = new HashSet<>();
        Node startNode = new Node(posX, posY, null, 0, heuristic(posX, posY));
        openList.add(startNode);

        while (!openList.isEmpty()) {
            Node currentNode = openList.poll();
            if (currentNode.x == destX && currentNode.y == destY) {
                return reconstructPath(currentNode);
            }

            closedList.add(currentNode);

            for (int[] direction : getDirections()) {
                int newX = currentNode.x + direction[0];
                int newY = currentNode.y + direction[1];
                if (isValidPosition(newX, newY) && !isInList(closedList, newX, newY)) {
                    char color = grid[newX][newY];
                    if (tickets.getOrDefault(color, 0) > 0) {
                        Node neighbor = new Node(newX, newY, currentNode, currentNode.g + 1, heuristic(newX, newY));
                        if (!isInList(openList, neighbor)) {
                            openList.add(neighbor);
                        }
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    private int heuristic(int x, int y) {
        return Math.abs(x - destX) + Math.abs(y - destY);
    }

    private List<int[]> reconstructPath(Node node) {
        List<int[]> path = new ArrayList<>();
        while (node != null) {
            path.add(new int[]{node.x, node.y});
            node = node.parent;
        }
        Collections.reverse(path);
        return path;
    }

    private int[][] getDirections() {
        return new int[][]{
            {-1, 0}, {1, 0}, {0, -1}, {0, 1},
            {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
        };
    }

    private boolean isValidPosition(int x, int y) {
        return x >= 0 && x < GRID_SIZE && y >= 0 && y < GRID_SIZE;
    }

    private boolean isInList(Collection<Node> list, int x, int y) {
        return list.stream().anyMatch(node -> node.x == x && node.y == y);
    }

    private boolean isInList(PriorityQueue<Node> list, Node node) {
        return list.stream().anyMatch(n -> n.x == node.x && n.y == node.y);
    }

    private boolean isValidMove(int x, int y) {
        if (x >= 0 && x < GRID_SIZE && y >= 0 && y < GRID_SIZE) {
            char color = grid[x][y];
            return tickets.getOrDefault(color, 0) > 0;
        }
        return false;
    }

    private String generateTradeProposal() {
        // Determine the needed tickets and their shortages
        Map<Character, Integer> neededTickets = new HashMap<>();
        List<int[]> path = findPathToDestination();
        for (int[] move : path) {
            char color = grid[move[0]][move[1]];
            neededTickets.put(color, neededTickets.getOrDefault(color, 0) + 1);
        }
    
        // Calculate the shortage for each needed ticket
        Map<Character, Integer> ticketShortages = new HashMap<>();
        for (Map.Entry<Character, Integer> entry : neededTickets.entrySet()) {
            char color = entry.getKey();
            int shortage = entry.getValue() - tickets.getOrDefault(color, 0);
            if (shortage > 0) {
                ticketShortages.put(color, shortage);
            }
        }
    
        // Find the most needed ticket color
        List<Map.Entry<Character, Integer>> sortedNeededTickets = new ArrayList<>(ticketShortages.entrySet());
        sortedNeededTickets.sort((entry1, entry2) -> entry2.getValue() - entry1.getValue());
        char colorToGet = sortedNeededTickets.isEmpty() ? getRandomColor() : sortedNeededTickets.get(0).getKey();
    
        // Determine the surplus tickets
        Map<Character, Integer> ticketSurpluses = new HashMap<>();
        for (Map.Entry<Character, Integer> entry : tickets.entrySet()) {
            char color = entry.getKey();
            int surplus = entry.getValue() - neededTickets.getOrDefault(color, 0);
            if (surplus > 0) {
                ticketSurpluses.put(color, surplus);
            }
        }
    
        // Find the most useless ticket color
        List<Map.Entry<Character, Integer>> sortedSurplusTickets = new ArrayList<>(ticketSurpluses.entrySet());
        sortedSurplusTickets.sort(Map.Entry.comparingByValue());
        char colorToGive = sortedSurplusTickets.isEmpty() ? getRandomColor() : sortedSurplusTickets.get(0).getKey();
    
        // Determine the quantity to trade (minimum of the shortage or the surplus)
        int quantity = Math.min(ticketShortages.getOrDefault(colorToGet, 1), ticketSurpluses.getOrDefault(colorToGive, 1));
    
        return "" + colorToGet + "," + colorToGive + "," + quantity;
    }
    
    private char getRandomColor() {
        return (char) ('A' + rand.nextInt(5));
    }

    private String generateRandomOffer() {
        // generate offer from my tickets in exchange for some of the other player's tickets
        char colorToGive = 'A';
        while(true){
            colorToGive = (char) ('A' + rand.nextInt(5));
            if(tickets.getOrDefault(colorToGive, 0) > 0){
                break;
            }
        }
        int quantity = rand.nextInt(tickets.getOrDefault(colorToGive,0)) + 1;
        char colorToGet = (char) ('A' + rand.nextInt(5));
        while(colorToGet == colorToGive){
            colorToGet = (char) ('A' + rand.nextInt(5));
        }
        return "" + colorToGet + "," + colorToGive + "," + quantity;
    }

    private void handleOffer(ACLMessage msg) {
        System.out.println(msg.getContent());
        String[] parts = msg.getContent().split(",");
        char colorToGet = parts[1].charAt(0);
        char colorToGive = parts[2].charAt(0);
        int quantity = Integer.parseInt(parts[3]);

        // Randomly decide to accept or reject the offer
        boolean accept = rand.nextBoolean();
        if (tickets.getOrDefault(colorToGet, 0) < quantity) {
            accept = false;
        }
        ACLMessage reply = msg.createReply();
        if (accept) {
            reply.setContent("RESPONSE,ACCEPT," + colorToGet + "," + colorToGive + "," + quantity);
            // update tickets
            tickets.put(colorToGive, tickets.getOrDefault(colorToGive, 0) + quantity);
            tickets.put(colorToGet, tickets.getOrDefault(colorToGet, 0) - quantity);
        } else {
            reply.setContent("RESPONSE,REJECT," + colorToGet + "," + colorToGive + "," + quantity);
        }
        send(reply);
    }

    private void handleResponse(ACLMessage msg) {
        String[] parts = msg.getContent().split(",");
        String response = parts[1];
        char colorToGet = parts[2].charAt(0);
        char colorToGive = parts[3].charAt(0);
        int quantity = Integer.parseInt(parts[4]);

        if ("ACCEPT".equals(response)) {
            tickets.put(colorToGet, tickets.getOrDefault(colorToGet, 0) + quantity);
            tickets.put(colorToGive, tickets.getOrDefault(colorToGive, 0) - quantity);
        }
    }

    private void handleUpdate(ACLMessage msg) {
        // Implement any additional update logic if needed
    }

    @Override
    protected void takeDown() {
        int score = calculateScore();
        System.out.println("Player " + getAID().getName() + " at final position: " + "(" + posX + "," + posY + ")" + " terminating with score: " + score);
    }

    private int calculateScore() {
        int score = 0;
        // System.out.println(tickets);
        // System.out.println((Math.abs(posX - destX) + Math.abs(posY - destY)));
        for (int count : tickets.values()) {
            score += count * 5;
        }
        if (reachedDestination) {
            score += 100;
        }
        else{
            // for each square away from the destination, reduce 10 points
            score -= 10 * (Math.abs(posX - destX) + Math.abs(posY - destY));
        }
        return score;
    }

    private void sendTerminationMessage() {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setContent("TERMINATE," + getAID().getLocalName());
        msg.addReceiver(new AID("game", AID.ISLOCALNAME));
        send(msg);
    }

    private static class Node {
        int x, y;
        Node parent;
        int g, f;

        Node(int x, int y, Node parent, int g, int h) {
            this.x = x;
            this.y = y;
            this.parent = parent;
            this.g = g;
            this.f = g + h;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Node node = (Node) obj;
            return x == node.x && y == node.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }
}
