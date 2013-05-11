import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Server {
	public static void main(String[] args) {
		new Server(8080, args.length > 0 ? args[0] : ".");
	}

	private Map<AJAXConnection, Player> players = new HashMap<>();

	public Server(int port, String wwwRoot) {
		AJAXServer server = new AJAXServer(this, port, wwwRoot);
		while (true) {
			synchronized (players) { for (AJAXConnection connection : players.keySet()) connection.update(); }
			try { Thread.sleep(10); } catch (InterruptedException e) {}
		}
	}

	void handleJoin(AJAXConnection connection, String name) {
		synchronized (players) {
			if (!players.containsKey(connection)) {
				Player player = new Player(name, 250, 250, 0, 0);
				for (Map.Entry<AJAXConnection, Player> other : players.entrySet()) {
					Player otherPlayer = other.getValue();
					connection.sendJoin(otherPlayer.name, otherPlayer.x, otherPlayer.y, otherPlayer.vx, otherPlayer.vy);
					other.getKey().sendJoin(player.name, player.x, player.y, player.vx, player.vy);
				}
				players.put(connection, player);
				connection.sendJoin(player.name, player.x, player.y, player.vx, player.vy);
			}
		}
	}

	void handleChat(AJAXConnection connection, String message) {
		synchronized (players) {
			if (!players.containsKey(connection)) return;
			for (AJAXConnection connection_ : players.keySet()) connection_.sendMessage(players.get(connection).name + ": " + message);
		}
	}

	// TODO: Have the server be authorative; accept key presses as updates.
	void handleState(AJAXConnection connection, int x, int y, int vx, int vy) {
		synchronized (players) {
			if (!players.containsKey(connection)) return;
			Player player = players.get(connection);
			player.x = x;
			player.y = y;
			player.vx = vx;
			player.vy = vy;
			for (AJAXConnection connection_ : players.keySet())
				if (connection != connection_)
					connection_.sendState(player.name, player.x, player.y, player.vx, player.vy);
		}
	}
}
