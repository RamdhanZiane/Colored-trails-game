To compile the project:

- Have the path for $CLASSPATH set to jade/bin/jade.jar
- javac GameAgent.java GameLauncher.java PlayerAgent.java GameConstants.java
- jar cf game.jar \*.class
- java -cp "$CLASSPATH;game.jar" jade.Boot -gui -agents "game:GameAgent;player1:PlayerAgent;player2:PlayerAgent"
