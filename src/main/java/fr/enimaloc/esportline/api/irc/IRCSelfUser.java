package fr.enimaloc.esportline.api.irc;

import fr.enimaloc.esportline.api.irc.twitch.TwitchIRCClient;

public class IRCSelfUser extends IRCUser {
    public IRCSelfUser(TwitchIRCClient client, String nickname, String username, String hostname) {
        super(client, nickname, username, hostname);
    }

    public IRCSelfUser(IRCUser user) {
        super(user.client, user.getNickname(), user.getUsername(), user.getHostname());
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
}
