import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract class ServerPacket {
	public abstract String toJavaScript();

	protected String toJSCall(String function, Object... args) {
		boolean first = true;
		StringBuilder sb = new StringBuilder();

		sb.append(function);
		sb.append('(');
		for (Object arg : args) {
			if (!first) sb.append(',');
			if (arg instanceof Number) {
				sb.append(arg.toString());
			} else if (arg instanceof String) {
				sb.append('\'');
				// TODO: Escape special characters (', \ etc).
				sb.append((String)arg);
				sb.append('\'');
			} else {
				throw new IllegalArgumentException(arg.toString() + " is not a Number or String.");
			}
			first = false;
		}
		sb.append(");");

		return sb.toString();
	}
}

class MessageSPacket extends ServerPacket {
	private String message;

	public MessageSPacket(String message) {
		this.message = message;
	}

	public String toJavaScript() {
		return toJSCall("handleMessage", message);
	}
}

class JoinSPacket extends ServerPacket {
	private String name;
	private double x, y, vx, vy;
	int health;

	public JoinSPacket(Player player) {
		this.name = player.name;
		this.x = player.x;
		this.y = player.y;
		this.vx = player.vx;
		this.vy = player.vy;
		this.health = player.health;
	}

	public String toJavaScript() {
		return toJSCall("handleJoin", name, x, y, vx, vy, health);
	}
}

class WallSPacket extends ServerPacket {
	private double x, y, w, h;

	public WallSPacket(Wall wall) {
		this.x = wall.x;
		this.y = wall.y;
		this.w = wall.w;
		this.h = wall.h;
	}

	public String toJavaScript() {
		return toJSCall("handleWall", x, y, w, h);
	}
}

class EnemySPacket extends ServerPacket {
	private int id, health;
	private double x, y, vx, vy;

	public EnemySPacket(int id, Player enemy) {
		this.id = id;
		this.x = enemy.x;
		this.y = enemy.y;
		this.vx = enemy.vx;
		this.vy = enemy.vy;
		this.health = enemy.health;
	}

	public String toJavaScript() {
		return toJSCall("handleEnemy", id, x, y, vx, vy, health);
	}
}

class ShotSPacket extends ServerPacket {
	private int id;
	private double x, y, vx, vy;

	public ShotSPacket(int id, Shot shot) {
		this.id = id;
		this.x = shot.x;
		this.y = shot.y;
		this.vx = shot.vx;
		this.vy = shot.vy;
	}

	public String toJavaScript() {
		return toJSCall("handleShot", id, x, y, vx, vy);
	}
}

class StateSPacket extends ServerPacket {
	private String name;
	private double x, y, vx, vy;
	private int health;

	public StateSPacket(Player player) {
		this.name = player.name;
		this.x = player.x;
		this.y = player.y;
		this.vx = player.vx;
		this.vy = player.vy;
		this.health = player.health;
	}

	public String toJavaScript() {
		return toJSCall("handleState", name, x, y, vx, vy, health);
	}
}

class EnemyDeathSPacket extends ServerPacket {
	private int id;

	public EnemyDeathSPacket(int id) {
		this.id = id;
	}

	public String toJavaScript() {
		return toJSCall("handleEnemyDeath", id);
	}
}

class ShotDeathSPacket extends ServerPacket {
	private int id;

	public ShotDeathSPacket(int id) {
		this.id = id;
	}

	public String toJavaScript() {
		return toJSCall("handleShotDeath", id);
	}
}

class PingSPacket extends ServerPacket {
	private int ping;

	public PingSPacket(int ping) {
		this.ping = ping;
	}

	public String toJavaScript() {
		return toJSCall("handlePing", ping);
	}
}

abstract class ClientPacket {
	public abstract void accept(Server server, AJAXConnection connection);
}

class JoinCPacket extends ClientPacket {
	private String name;

	public JoinCPacket(String name) {
		this.name = name;
	}

	public void accept(Server server, AJAXConnection connection) {
		server.handleJoin(connection, name);
	}
}

class StateCPacket extends ClientPacket {
	private double vx, vy;
	private boolean rangedAttack;

	public StateCPacket(double vx, double vy, boolean rangedAttack) {
		this.vx = vx;
		this.vy = vy;
		this.rangedAttack = rangedAttack;
	}

	public void accept(Server server, AJAXConnection connection) {
		server.handleState(connection, vx, vy, rangedAttack);
	}
}

class PlayerStateDiff {
	private double vx, vy;
	private int health;
	private long lastUpdate;
	private static long MAX_UPDATE_INTERVAL = 100;

	public PlayerStateDiff(double vx, double vy, int health) {
		this.vx = vx;
		this.vy = vy;
		this.health = health;
		this.lastUpdate = System.currentTimeMillis();
	}

	public boolean checkAndSet(double vx, double vy, int health) {
		if (vx != this.vx || vy != this.vy || health != this.health ||
		    lastUpdate + MAX_UPDATE_INTERVAL < System.currentTimeMillis()) {
			this.vx = vx;
			this.vy = vy;
			this.health = health;
			lastUpdate = System.currentTimeMillis();
			return true;
		} else {
			return false;
		}
	}
}

public class AJAXConnection {
	private Socket socket;

	private int id;
	public int getID() { return id; }

	private long lastUpdate;
	private long lastReopen;

	public int getPing() { return (int)(lastReopen - lastUpdate); }

	private List<ServerPacket> sendQueue = new ArrayList<>();
	private List<ClientPacket> recvQueue = new ArrayList<>();

	private Map<Player, PlayerStateDiff> playerEnemyDiffs = new HashMap<>();

	public AJAXConnection(Socket socket, int id) {
		this.socket = socket;
		this.id = id;
	}

	public void sendMessage(String message) {
		// TODO: Escape message.
		sendQueue.add(new MessageSPacket(message));
	}

	public void sendJoin(Player player) {
		sendQueue.add(new JoinSPacket(player));
	}

	public void sendState(Player player) {
		if (!playerEnemyDiffs.containsKey(player)) {
			playerEnemyDiffs.put(player, new PlayerStateDiff(player.vx, player.vy, player.health));
			sendQueue.add(new StateSPacket(player));
		} else if (playerEnemyDiffs.get(player).checkAndSet(player.vx, player.vy, player.health) || player.pushed) {
			sendQueue.add(new StateSPacket(player));
		}
	}

	public void sendWall(Wall wall) {
		sendQueue.add(new WallSPacket(wall));
	}

	public void sendEnemy(int id, Player enemy) {
		if (!playerEnemyDiffs.containsKey(enemy)) {
			playerEnemyDiffs.put(enemy, new PlayerStateDiff(enemy.vx, enemy.vy, enemy.health));
			sendQueue.add(new EnemySPacket(id, enemy));
		} else if (playerEnemyDiffs.get(enemy).checkAndSet(enemy.vx, enemy.vy, enemy.health) || enemy.pushed) {
			sendQueue.add(new EnemySPacket(id, enemy));
		}
	}

	public void sendEnemyDeath(int id) {
		sendQueue.add(new EnemyDeathSPacket(id));
	}

	public void sendShot(int id, Shot shot) {
		sendQueue.add(new ShotSPacket(id, shot));
	}

	public void sendShotDeath(int id) {
		sendQueue.add(new ShotDeathSPacket(id));
	}

	void recvJoin(String name) {
		synchronized (recvQueue) { recvQueue.add(new JoinCPacket(name)); }
	}

	void recvState(double vx, double vy, boolean rangedAttack) {
		synchronized (recvQueue) { recvQueue.add(new StateCPacket(vx, vy, rangedAttack)); }
	}

	public void update(Server server) {
		synchronized (socket) {
			if (!sendQueue.isEmpty() && !socket.isClosed()) {
				try {
					PrintStream ps = new PrintStream(socket.getOutputStream());
					ps.print("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n\r\n");
					for (ServerPacket packet : sendQueue) ps.print(packet.toJavaScript());
					ps.print(new PingSPacket(getPing()).toJavaScript());
					ps.print("\r\n");
					ps.flush();
					socket.close();
					sendQueue.clear();
					lastUpdate = System.currentTimeMillis();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}

		synchronized (recvQueue) {
			for (ClientPacket packet : recvQueue) packet.accept(server, this);
			recvQueue.clear();
		}
	}

	void setSocket(Socket socket) {
		synchronized (this.socket) {
			this.socket = socket;
			this.lastReopen = System.currentTimeMillis();
		}
	}
}
