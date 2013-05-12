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
		AJAXServer server = new AJAXServer(port, wwwRoot);
		double dt = 1.0 / 60.0;
		while (true) {
			List<Player> ps = new ArrayList<>(players.values());

			for (Player p : ps) p.update(dt);

			for (int i = 0; i < ps.size(); ++i) {
				Player pi = ps.get(i);
				for (int j = i + 1; j < ps.size(); ++j) {
					Player pj = ps.get(j);
					double dx = pi.x - pj.x;
					double dy = pi.y - pj.y;
					double d = Math.sqrt(dx * dx + dy * dy);
					if (d < 24) {
						double sx = (dx / d) * (24 - d);
						double sy = (dy / d) * (24 - d);
						pi.x += sx;
						pi.y += sy;
						pj.x -= sx;
						pj.y -= sy;
						// TODO: Remove the velocities?
					}
				}
			}

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
			for (Map.Entry<AJAXConnection, Player> other : players.entrySet()) {
				Player otherPlayer = other.getValue();
				connection.sendJoin(otherPlayer);
				other.getKey().sendJoin(player);
			}
			players.put(connection, player);
		}
	}

	void handleChat(AJAXConnection connection, String message) {
		if (!players.containsKey(connection)) return;
		for (AJAXConnection connection_ : players.keySet()) connection_.sendMessage(players.get(connection).name + ": " + message);
	}

	// TODO: Have the server be authorative; accept key presses as updates.
	void handleState(AJAXConnection connection, double vx, double vy) {
		if (!players.containsKey(connection)) return;
		Player player = players.get(connection);
		player.vx = vx;
		player.vy = vy;
	}
}
