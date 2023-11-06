package fr.enimaloc.esportline.api.irc;

import fr.enimaloc.esportline.api.irc.twitch.TwitchIRCClient;

import java.util.Arrays;

public class IRCMessageRO {
    protected final TwitchIRCClient client;
    protected IRCChannel channel;
    protected String[] tags;
    protected IRCUser sender;
    protected String command;
    protected String[] params;
    protected String trailing;
    protected String raw;

    public IRCMessageRO(TwitchIRCClient client, IRCChannel channel, IRCUser sender, String command, String[] params, String trailing, String raw) {
        this.client = client;
        this.channel = channel;
        this.sender = sender;
        this.command = command;
        this.params = params;
        this.trailing = trailing;
        this.raw = raw;
    }

    public IRCMessageRO(TwitchIRCClient client, IRCChannel channel, String raw) {
        this.client = client;
        this.channel = channel;
        this.raw = raw;
        if (!raw.endsWith("\r\n")) this.raw += "\r\n";
        else raw = raw.substring(0, raw.length() - 2);
        this.tags = raw.startsWith("@") ? raw.substring(1, raw.indexOf(' ')).split(";") : null;
        raw = raw.startsWith("@") ? raw.substring(raw.indexOf(' ') + 1) : raw;
        this.sender = raw.startsWith(":") ? new IRCUser(client, raw.substring(1, raw.indexOf(' '))) : null;
        raw = raw.startsWith(":") ? raw.substring(raw.indexOf(' ') + 1) : raw;
        this.command = raw.substring(0, raw.indexOf(' '));
        raw = raw.substring(raw.indexOf(' ') + 1);
        this.params = raw.contains(" :") ? raw.substring(0, raw.indexOf(" :")).split(" ") : raw.split(" ");
        this.trailing = raw.contains(" :") ? raw.substring(raw.indexOf(" :") + 2) : null;
    }

    public IRCMessageRO(TwitchIRCClient client, String raw) {
        this.client = client;
        this.raw = raw;
        if (!raw.endsWith("\r\n")) this.raw += "\r\n";
        else raw = raw.substring(0, raw.length() - 2);
        this.tags = raw.startsWith("@") ? raw.substring(1, raw.indexOf(' ')).split(";") : null;
        raw = raw.startsWith("@") ? raw.substring(raw.indexOf(' ') + 1) : raw;
        this.sender = raw.startsWith(":") ? new IRCUser(client, raw.substring(1, raw.indexOf(' '))) : null;
        raw = raw.startsWith(":") ? raw.substring(raw.indexOf(' ') + 1) : raw;
        this.command = raw.substring(0, raw.indexOf(' '));
        raw = raw.substring(raw.indexOf(' ') + 1);
        this.params = raw.contains(" :") ? raw.substring(0, raw.indexOf(" :")).split(" ") : raw.split(" ");
        this.trailing = raw.contains(" :") ? raw.substring(raw.indexOf(" :") + 2) : null;
        this.channel = switch (this.command) {
            case "PRIVMSG", "JOIN", "PART", "NOTICE" -> client.getChannel(params[0]).orElse(null);
            default -> null;
        };
    }

    public TwitchIRCClient getClient() {
        return client;
    }

    public String[] getTags() {
        return tags;
    }

    public IRCUser getSender() {
        return sender;
    }

    public String getCommand() {
        return command;
    }

    public String[] getParams() {
        return params;
    }

    public String getTrailing() {
        return trailing;
    }

    public String getRaw() {
        return raw;
    }

    public IRCChannel getChannel() {
        return channel;
    }

    @Override
    public String toString() {
        return raw;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IRCMessageRO ircMessage)) return false;

        if (getTags() != null ? !Arrays.equals(getTags(), ircMessage.getTags()) : ircMessage.getTags() != null)
            return false;
        if (getSender() != null ? !getSender().equals(ircMessage.getSender()) : ircMessage.getSender() != null)
            return false;
        if (getCommand() != null ? !getCommand().equals(ircMessage.getCommand()) : ircMessage.getCommand() != null)
            return false;
        if (getParams() != null ? !Arrays.equals(getParams(), ircMessage.getParams()) : ircMessage.getParams() != null)
            return false;
        return getTrailing() != null ? getTrailing().equals(ircMessage.getTrailing()) : ircMessage.getTrailing() == null;
    }
}
