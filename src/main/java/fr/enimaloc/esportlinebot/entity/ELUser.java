package fr.enimaloc.esportlinebot.entity;

public class ELUser {
    private final long id;
    private final long guildId;
    private int level;
    private double xp;
    private double totalXp;

    public ELUser(long id, long guildId) {
        this(id, guildId, 0, 0, 0);
    }

    public ELUser(long id, long guildId, int level, double xp) {
        this(id, guildId, level, xp, xp);
    }

    public ELUser(long id, long guildId, int level, double xp, double totalXp) {
        this.id = id;
        this.guildId = guildId;
        this.level = level;
        this.xp = xp;
        this.totalXp = totalXp;
    }

    public long getId() {
        return id;
    }

    public long getGuildId() {
        return guildId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public double getXp() {
        return xp;
    }

    public void setXp(double xp) {
        this.xp = xp;
        this.totalXp += xp;
    }

    public void setExp(int level, double xp) {
        this.setLevel(level);
        this.setXp(xp);
    }

    public double getTotalXp() {
        return totalXp;
    }

    public void levelUp() {
        this.level++;
        this.setXp(0);
    }
}
