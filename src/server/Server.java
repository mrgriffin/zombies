import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Server {
	public static void main(String[] args) {
		new Server(8080, args.length > 0 ? args[0] : ".");
	}

	private Map<AJAXConnection, Player> players = new HashMap<>();
	private Game game = new Game();

	public Server(int port, String wwwRoot) {
		game.addWall(new Wall(250, 12, 500, 24));
		game.addWall(new Wall(250, 488, 500, 24));
		game.addWall(new Wall(12, 250, 24, 500));
		game.addWall(new Wall(488, 250, 24, 500));

		AJAXServer server = new AJAXServer(port, wwwRoot);
		double dt = 1.0 / 60.0;
		while (true) {
			game.update(dt);

			for (Player player : players.values()) {
				for (AJAXConnection connection : players.keySet()) connection.sendState(player);
			}

			server.update(this);
			try { Thread.sleep((int)(dt * 1000)); } catch (InterruptedException e) {}
		}
	}

	void handleJoin(AJAXConnection connection, String name) {
		if (!players.containsKey(connection)) {
			Player player = new Player(name, 250, 250, 0, 0);
			connection.sendJoin(player);
			game.send(connection);
			for (Map.Entry<AJAXConnection, Player> other : players.entrySet()) {
				Player otherPlayer = other.getValue();
				connection.sendJoin(otherPlayer);
				other.getKey().sendJoin(player);
			}
			players.put(connection, player);
			game.addPlayer(player);
		}
	}

	void handleChat(AJAXConnection connection, String message) {
		if (!players.containsKey(connection)) return;
		for (AJAXConnection connection_ : players.keySet()) connection_.sendMessage(players.get(connection).name + ": " + message);
	}

	// TODO: Have the server be authorative; accept key presses as updates.
	void handleState(AJAXConnection connection, double ix, double iy) {
		if (!players.containsKey(connection)) return;
		Player player = players.get(connection);
		player.setInputs(Math.max(-1, Math.min(1, ix)), Math.max(-1, Math.min(1, iy)));
	}
}
