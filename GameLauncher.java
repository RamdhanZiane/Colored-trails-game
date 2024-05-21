import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

import java.util.HashMap;
import java.util.Map;

public class GameLauncher {
    public static void main(String[] args) {
        Runtime rt = Runtime.instance();
        Profile profile = new ProfileImpl();
        AgentContainer mainContainer = rt.createMainContainer(profile);

        try {
            AgentController gameAgent = mainContainer.createNewAgent("game", "GameAgent", null);
            gameAgent.start();

            char[][] grid = {
                {'A', 'B', 'C', 'D', 'E'},
                {'B', 'C', 'D', 'E', 'A'},
                {'C', 'D', 'E', 'A', 'B'},
                {'D', 'E', 'A', 'B', 'C'},
                {'E', 'A', 'B', 'C', 'D'}
            };

            Map<Character, Integer> player1Tickets = new HashMap<>();
            player1Tickets.put('A', 5);
            player1Tickets.put('B', 5);
            player1Tickets.put('C', 5);
            player1Tickets.put('D', 5);
            player1Tickets.put('E', 5);

            Map<Character, Integer> player2Tickets = new HashMap<>();
            player2Tickets.put('A', 5);
            player2Tickets.put('B', 5);
            player2Tickets.put('C', 5);
            player2Tickets.put('D', 5);
            player2Tickets.put('E', 5);

            
            Object[] player1Args = new Object[]{0, 0, 4, 4};
            AgentController player1 = mainContainer.createNewAgent("player1", "PlayerAgent", player1Args);
            player1.start();

            Object[] player2Args = new Object[]{4, 4, 0, 0};
            AgentController player2 = mainContainer.createNewAgent("player2", "PlayerAgent", player2Args);
            player2.start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}
