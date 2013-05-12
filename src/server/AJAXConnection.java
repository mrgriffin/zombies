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

class MessagePacket extends ServerPacket {
	private String message;

	public MessagePacket(String message) {
		this.message = message;
	}

	public String toJavaScript() {
		return toJSCall("handleMessage", message);
	}
}

class JoinPacket extends ServerPacket {
	private String name;
	private double x, y, vx, vy;

	public JoinPacket(Player player) {
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

class StatePacket extends ServerPacket {
	private String name;
	private double x, y, vx, vy;

	public StatePacket(Player player) {
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

class PingPacket extends ServerPacket {
	private int ping;

	public PingPacket(int ping) {
		this.ping = ping;
	}

	public String toJavaScript() {
		return toJSCall("handlePing", ping);
	}
}

public class AJAXConnection {
	private Socket socket;

	private int id;
	public int getID() { return id; }

	private long lastUpdate;
	private long lastReopen;

	public int getPing() { return (int)(lastReopen - lastUpdate); }

	private List<ServerPacket> packets = new ArrayList<>();

	public AJAXConnection(Socket socket, int id) {
		this.socket = socket;
		this.id = id;
	}

	public void sendMessage(String message) {
		// TODO: Escape message.
		packets.add(new MessagePacket(message));
	}

	public void sendJoin(Player player) {
		packets.add(new JoinPacket(player));
	}

	public void sendState(Player player) {
		packets.add(new StatePacket(player));
	}

	public void update() {
		if (!packets.isEmpty() && !socket.isClosed()) {
			try {
				PrintStream ps = new PrintStream(socket.getOutputStream());
				ps.print("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n\r\n");
				for (ServerPacket packet : packets) ps.print(packet.toJavaScript());
				ps.print(new PingPacket(getPing()).toJavaScript());
				packets.clear();
				ps.print("\r\n");
				ps.flush();
				socket.close();
				lastUpdate = System.currentTimeMillis();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	void setSocket(Socket socket) {
		this.socket = socket;
		this.lastReopen = System.currentTimeMillis();
	}
}
