package fr.enimaloc.esportlinebot.entity;

public class ELUser {
    private final long id;
    private final long guildId;
    private int level;
    private double xp;

    public ELUser(long id, long guildId) {
        this(id, guildId, 0, 0);
    }

    public ELUser(long id, long guildId, int level, double xp) {
        this.id = id;
        this.guildId = guildId;
        this.level = level;
        this.xp = xp;
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

    public double getXp() {
        return xp;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setXp(double xp) {
        this.xp = xp;
    }

    public void setExp(int level, double xp) {
        this.level = level;
        this.xp = xp;
    }
}
