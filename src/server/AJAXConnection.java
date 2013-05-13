import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

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

	public JoinSPacket(Player player) {
		this.name = player.name;
		this.x = player.x;
		this.y = player.y;
		this.vx = player.vx;
		this.vy = player.vy;
	}

	public String toJavaScript() {
		return toJSCall("handleJoin", name, x, y, vx, vy);
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
	private int id;
	private double x, y, vx, vy;

	public EnemySPacket(int id, Player enemy) {
		this.id = id;
		this.x = enemy.x;
		this.y = enemy.y;
		this.vx = enemy.vx;
		this.vy = enemy.vy;
	}

	public String toJavaScript() {
		return toJSCall("handleEnemy", id, x, y, vx, vy);
	}
}

class StateSPacket extends ServerPacket {
	private String name;
	private double x, y, vx, vy;

	public StateSPacket(Player player) {
		this.name = player.name;
		this.x = player.x;
		this.y = player.y;
		this.vx = player.vx;
		this.vy = player.vy;
	}

	public String toJavaScript() {
		return toJSCall("handleState", name, x, y, vx, vy);
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

	public StateCPacket(double vx, double vy) {
		this.vx = vx;
		this.vy = vy;
	}

	public void accept(Server server, AJAXConnection connection) {
		server.handleState(connection, vx, vy);
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
		sendQueue.add(new StateSPacket(player));
	}

	public void sendWall(Wall wall) {
		sendQueue.add(new WallSPacket(wall));
	}

	public void sendEnemy(int id, Player enemy) {
		sendQueue.add(new EnemySPacket(id, enemy));
	}

	void recvJoin(String name) {
		synchronized (recvQueue) { recvQueue.add(new JoinCPacket(name)); }
	}

	void recvState(double vx, double vy) {
		synchronized (recvQueue) { recvQueue.add(new StateCPacket(vx, vy)); }
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
