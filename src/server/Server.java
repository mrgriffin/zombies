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
		double dt = 1.0 / 60.0;
		while (true) {
			synchronized (players) {
				for (Map.Entry<AJAXConnection, Player> entry : players.entrySet()) {
					Player player = entry.getValue();
					player.update(dt);
					for (AJAXConnection other : players.keySet()) other.sendState(player.name, player.x, player.y, player.vx, player.vy);
					entry.getKey().update();
				}
			}
			try { Thread.sleep((int)(dt * 1000)); } catch (InterruptedException e) {}
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
	void handleState(AJAXConnection connection, double vx, double vy) {
		synchronized (players) {
			if (!players.containsKey(connection)) return;
			Player player = players.get(connection);
			player.vx = vx;
			player.vy = vy;
		}
	}
}
