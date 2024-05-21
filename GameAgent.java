import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class GameAgent extends Agent {
    private char[][] grid = {
            { 'A', 'B', 'C', 'D', 'E' },
            { 'B', 'C', 'D', 'E', 'A' },
            { 'C', 'D', 'E', 'A', 'B' },
            { 'D', 'E', 'A', 'B', 'C' },
            { 'E', 'A', 'B', 'C', 'D' }
    };

    @Override
    protected void setup() {
        System.out.println("Game agent " + getAID().getName() + " is ready.");

        addBehaviour(new TickerBehaviour(this, 3000) {
            @Override
            protected void onTick() {
                // Movement phase
                sendMoveRequests();

                // Offer phase
                // sendOffers();
            }
        });

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null && msg.getContent().startsWith("TERMINATE")) {
                    terminateGame(msg.getContent().split(",")[1]);
                } else {
                    block();
                }
            }
        });
    }

    private void sendMoveRequests() {
        ACLMessage moveMsg = new ACLMessage(ACLMessage.REQUEST);
        moveMsg.setContent("MOVE");
        moveMsg.addReceiver(new AID("player1", AID.ISLOCALNAME));
        moveMsg.addReceiver(new AID("player2", AID.ISLOCALNAME));
        send(moveMsg);
    }

    private void sendOffers() {
        ACLMessage offerMsg = new ACLMessage(ACLMessage.REQUEST);
        offerMsg.setContent("OFFER");
        offerMsg.addReceiver(new AID("player1", AID.ISLOCALNAME));
        offerMsg.addReceiver(new AID("player2", AID.ISLOCALNAME));
        send(offerMsg);
    }

    private void terminateGame(String terminatingAgent) {
        ACLMessage terminateMsg = new ACLMessage(ACLMessage.INFORM);
        terminateMsg.setContent("TERMINATE");
        terminateMsg.addReceiver(new AID("player1", AID.ISLOCALNAME));
        terminateMsg.addReceiver(new AID("player2", AID.ISLOCALNAME));
        send(terminateMsg);
        System.out.println("Game terminated by " + terminatingAgent);
        doDelete();
    }

    @Override
    protected void takeDown() {
        System.out.println("Game agent " + getAID().getName() + " terminating.");
    }
}
