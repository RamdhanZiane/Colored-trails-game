import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

public class GameAgent extends Agent {
    @Override
    protected void setup() {
        System.out.println("Game agent " + getAID().getName() + " is ready.");

        addBehaviour(new TickerBehaviour(this, 1000) {
            @Override
            protected void onTick() {
                // Broadcast game state or requests to PlayerAgents
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setContent("Game update: " + System.currentTimeMillis());
                // Assuming players' local names are "player1", "player2", etc.
                msg.addReceiver(getAID("player1"));
                msg.addReceiver(getAID("player2"));
                send(msg);
            }
        });
    }

    @Override
    protected void takeDown() {
        System.out.println("Game agent " + getAID().getName() + " terminating.");
    }
}
