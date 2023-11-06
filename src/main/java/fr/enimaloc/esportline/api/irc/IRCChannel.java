package fr.enimaloc.esportline.api.irc;

import fr.enimaloc.esportline.api.irc.twitch.TwitchIRCClient;

import java.util.ArrayList;
import java.util.List;

public class IRCChannel implements IRCMessageReceiver {
    private final TwitchIRCClient client;
    private final List<IRCUser> users = new ArrayList<>();
    private String name;

    public IRCChannel(TwitchIRCClient client, String name) {
        this.client = client;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<IRCUser> getUsers() {
        return users;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public void sendMessage(String message) {
        client.send(this, message);
    }
}
