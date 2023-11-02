package fr.enimaloc.esportline.commands.slash;

import fr.enimaloc.enutils.jda.register.annotation.Slash;
import fr.enimaloc.esportline.commands.slash.game.wakfu.Wakfu;
import fr.enimaloc.esportline.commands.slash.game.wakfu.WakfuAdmin;

@Slash(name = "game", description = "Get information about a game")
public class GameCommand {
//     TODO: 02/11/23 Add games below (maybe filter)
//    @Slash.Sub.GroupProvider
//    public Minecraft minecraft = new Minecraft();
//    @Slash.Sub.GroupProvider
//    public LeagueOfLegends leagueOfLegends = new LeagueOfLegends();
//    @Slash.Sub.GroupProvider
//    public Valorant valorant = new Valorant();
//    @Slash.Sub.GroupProvider
//    public Fortnite fortnite = new Fortnite();
//    @Slash.Sub.GroupProvider
//    public RocketLeague rocketLeague = new RocketLeague();
//    @Slash.Sub.GroupProvider
//    public ApexLegends apexLegends = new ApexLegends();
//    @Slash.Sub.GroupProvider
//    public RainbowSixSiege rainbowSixSiege = new RainbowSixSiege();
//    @Slash.Sub.GroupProvider
//    public CallOfDuty callOfDuty = new CallOfDuty();
//    @Slash.Sub.GroupProvider
//    public CounterStrike counterStrike = new CounterStrike();
//    @Slash.Sub.GroupProvider
//    public Overwatch overwatch = new Overwatch();
//    @Slash.Sub.GroupProvider
//    public Hearthstone hearthstone = new Hearthstone();
//    @Slash.Sub.GroupProvider
//    public Starcraft starcraft = new Starcraft();
//    @Slash.Sub.GroupProvider
//    public Dota dota = new Dota();
//    @Slash.Sub.GroupProvider
//    public WorldOfWarcraft worldOfWarcraft = new WorldOfWarcraft();
//    @Slash.Sub.GroupProvider
//    public Trackmania trackmania = new Trackmania();
//    @Slash.Sub.GroupProvider
//    public Fifa fifa = new Fifa();
//    @Slash.Sub.GroupProvider
//    public SuperSmashBros superSmashBros = new SuperSmashBros();
    @Slash.Sub.GroupProvider
    public Wakfu wakfu;
    @Slash.Sub.GroupProvider
    public WakfuAdmin wakfuAdmin;

    public GameCommand(Wakfu wakfu, WakfuAdmin wakfuAdmin) {
        this.wakfu = wakfu;
        this.wakfuAdmin = wakfuAdmin;
    }
}
