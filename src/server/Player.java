public class Player {
	// TODO: Make the instance variables private.
	public String name;
	public double x, y;
	public double vx, vy;
	public int health;

	private long lastAttack;
	private static int ATTACK_DAMAGE = 10;
	private static int ATTACK_INTERVAL = 1000;

	public Player(String name, double x, double y, double vx, double vy, int health) {
		this.name = name;
		this.x = x;
		this.y = y;
		this.vx = vx;
		this.vy = vy;
		this.health = health;
		this.lastAttack = System.currentTimeMillis();
	}

	public void update(double dt) {
		x += vx * dt;
		y += vy * dt;
	}

	public void setInputs(double x, double y) {
		vx = x * 100;
		vy = y * 100;
	}

	public void attack(Player player) {
		if (lastAttack + ATTACK_INTERVAL < System.currentTimeMillis()) {
			player.health -= ATTACK_DAMAGE;
			lastAttack = System.currentTimeMillis();
		}
	}
}
