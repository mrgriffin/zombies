public class Player {
	// TODO: Make the instance variables private.
	public String name;
	public double x, y;   // Position.
	public double vx, vy; // Velocity.
	public double ox, oy; // Orientation.
	public int health;

	private boolean rangedAttack;
	private long lastAttack;
	private static int ATTACK_DAMAGE = 10;
	private static int ATTACK_INTERVAL = 1000;

	public Player(String name, double x, double y, double vx, double vy, int health) {
		this.name = name;
		this.x = x;
		this.y = y;
		this.vx = vx;
		this.vy = vy;
		this.ox = 1;
		this.oy = 0;
		this.health = health;
		this.rangedAttack = false;
		this.lastAttack = System.currentTimeMillis();
	}

	public void update(double dt) {
		x += vx * dt;
		y += vy * dt;
	}

	public void setInputs(double x, double y, boolean rangedAttack) {
		this.vx = x * 100;
		this.vy = y * 100;
		if (x != 0 || y != 0) { this.ox = x; this.oy = y; }
		this.rangedAttack = rangedAttack;
	}

	public void meleeAttack(Player player) {
		if (lastAttack + ATTACK_INTERVAL < System.currentTimeMillis()) {
			player.health -= ATTACK_DAMAGE;
			lastAttack = System.currentTimeMillis();
		}
	}

	// WARNING: Despite the name, this actually mutates the object!
	public boolean isRangedAttacking() {
		if (rangedAttack && lastAttack + ATTACK_INTERVAL < System.currentTimeMillis()) {
			lastAttack = System.currentTimeMillis();
			return true;
		} else {
			return false;
		}
	}
}
