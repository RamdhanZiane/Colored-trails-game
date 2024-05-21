javac GameAgent.java GameLauncher.java PlayerAgent.java
jar cf game.jar \*.class
java -cp "$CLASSPATH;game.jar" jade.Boot -gui -agents "game:GameAgent;player1:PlayerAgent;player2:PlayerAgent"