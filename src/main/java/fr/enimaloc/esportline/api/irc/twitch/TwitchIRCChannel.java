package fr.enimaloc.esportline.api.irc.twitch;

import fr.enimaloc.esportline.api.irc.IRCChannel;

public class TwitchIRCChannel extends IRCChannel {
    protected boolean emoteOnly;
    protected boolean followersOnly;
    protected boolean r9k;
    protected int roomId;
    protected boolean slow;
    protected boolean subsOnly;

    public TwitchIRCChannel(TwitchIRCClient client, String name) {
        super(client, name);
    }

    public boolean isEmoteOnly() {
        return emoteOnly;
    }

    public boolean isFollowersOnly() {
        return followersOnly;
    }

    public boolean isR9k() {
        return r9k;
    }

    public int getRoomId() {
        return roomId;
    }

    public boolean isSlow() {
        return slow;
    }

    public boolean isSubsOnly() {
        return subsOnly;
    }

    public void roomState(String[] tags) {
        for (String tag : tags) {
            String[] split = tag.split("=", 2);
            switch (split[0]) {
                case "emote-only":
                    emoteOnly = split[1].equals("1");
                    break;
                case "followers-only":
                    followersOnly = split[1].equals("1");
                    break;
                case "r9k":
                    r9k = split[1].equals("1");
                    break;
                case "room-id":
                    roomId = Integer.parseInt(split[1]);
                    break;
                case "slow":
                    slow = split[1].equals("1");
                    break;
                case "subs-only":
                    subsOnly = split[1].equals("1");
                    break;
            }
        }
    }
}
