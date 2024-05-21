import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class PlayerAgent extends Agent {
    private class MyCyclicBehaviour extends CyclicBehaviour {
        public MyCyclicBehaviour(Agent a) {
            super(a);
        }

        @Override
        public void action() {
            ACLMessage msg = receive();
            if (msg != null) {
                System.out.println(getAID().getName() + " received message: " + msg.getContent());
                // Handle game logic here
            } else {
                block();
            }
        }
    }

    @Override
    protected void setup() {
        System.out.println("Player agent " + getAID().getName() + " is ready.");

        addBehaviour(new MyCyclicBehaviour(this));
    }

    @Override
    protected void takeDown() {
        System.out.println("Player agent " + getAID().getName() + " terminating.");
    }
}
