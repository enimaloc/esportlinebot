package fr.enimaloc.esportlinebot.entity;

import fr.enimaloc.esportlinebot.module.stats.StatsModule;

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
        updatePrometheus();
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
        updatePrometheus();
    }

    public double getXp() {
        return xp;
    }

    public void setXp(double xp) {
        this.xp = xp;
        this.totalXp += xp;
        updatePrometheus();
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

    private void updatePrometheus() {
        StatsModule.LEVEL_COUNTER.labels(String.valueOf(this.guildId), String.valueOf(this.id)).set(this.level);
        StatsModule.XP_COUNTER.labels(String.valueOf(this.guildId), String.valueOf(this.id)).set(this.xp);
        StatsModule.TOTAL_XP_COUNTER.labels(String.valueOf(this.guildId), String.valueOf(this.id)).set(this.totalXp);
    }
}
