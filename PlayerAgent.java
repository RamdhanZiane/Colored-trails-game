import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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
            tickets.put('A', 5);
            tickets.put('B', 3);
            tickets.put('C', 5);
            tickets.put('D', 5);
            tickets.put('E', 5);
            posX = 3;
            posY = 3;
            destX = 1;
            destY = 1;
            playersList = new String[]{"player2"};
        } else {
            tickets = new HashMap<>();
            tickets.put('A', 5);
            tickets.put('B', 2);
            tickets.put('C', 5);
            tickets.put('D', 1);
            tickets.put('E', 0);
            posX = 4;
            posY = 1;
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
                        doDelete();
                    }
                } else {
                    block();
                }
            }
        });
    }

    private void handleMove() {
        boolean moved = attemptMove();
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
            offerMsg.setContent("OFFER," + generateRandomOffer());
            // offerMsg.addReceiver(new AID("player1", AID.ISLOCALNAME));
            // offerMsg.addReceiver(new AID("player2", AID.ISLOCALNAME));
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
                    offerMsg.addReceiver(new AID(playersList[rand.nextInt(playersList.length)], AID.ISLOCALNAME));
                    send(offerMsg);
                }
            }
        }
    }

    private boolean attemptMove() {
        int[] dx = {-1, 1, 0, 0, -1, -1, 1, 1};
        int[] dy = {0, 0, -1, 1, -1, 1, -1, 1};
        for (int i = 0; i < dx.length; i++) {
            int newX = posX + dx[i];
            int newY = posY + dy[i];
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

    private boolean isValidMove(int x, int y) {
        if (x >= 0 && x < GRID_SIZE && y >= 0 && y < GRID_SIZE) {
            char color = grid[x][y];
            return tickets.getOrDefault(color, 0) > 0;
        }
        return false;
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
        System.out.println("Player " + getAID().getName() + " terminating with score: " + score);
    }

    private int calculateScore() {
        int score = 0;
        for (int count : tickets.values()) {
            score += count * 5;
        }
        if (reachedDestination) {
            score += 100;
        }
        return score;
    }

    private void sendTerminationMessage() {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setContent("TERMINATE," + getAID().getLocalName());
        msg.addReceiver(new AID("game", AID.ISLOCALNAME));
        send(msg);
    }
}
