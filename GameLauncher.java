import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class GameLauncher {
    public static void main(String[] args) {
        Runtime rt = Runtime.instance();
        Profile profile = new ProfileImpl();
        AgentContainer mainContainer = rt.createMainContainer(profile);

        try {
            AgentController gameAgent = mainContainer.createNewAgent("game", "GameAgent", null);
            gameAgent.start();

            AgentController player1 = mainContainer.createNewAgent("player1", "PlayerAgent", null);
            player1.start();

            AgentController player2 = mainContainer.createNewAgent("player2", "PlayerAgent", null);
            player2.start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}
