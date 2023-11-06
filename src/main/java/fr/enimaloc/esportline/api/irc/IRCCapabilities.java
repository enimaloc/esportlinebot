package fr.enimaloc.esportline.api.irc;

import org.jetbrains.annotations.Nullable;

public record IRCCapabilities(@Nullable String namespace, String name) {

    public static final IRCCapabilities ACCOUNT_NOTIFY = new IRCCapabilities("account-notify");
    public static final IRCCapabilities ACCOUNT_VERIFY = IRCCapabilities.draft("account-verify");
    public static final IRCCapabilities ACCOUNT_TAG = new IRCCapabilities("account-tag");
    public static final IRCCapabilities AWAY_NOTIFY = new IRCCapabilities("away-notify");
    public static final IRCCapabilities BATCH = new IRCCapabilities("batch");
    public static final IRCCapabilities CAP_NOTIFY = new IRCCapabilities("cap-notify");
    public static final IRCCapabilities CHANNEL_RENAME = IRCCapabilities.draft("channel-rename");
    public static final IRCCapabilities CHATHISTORY = IRCCapabilities.draft("chathistory");
    public static final IRCCapabilities CHGHOST = new IRCCapabilities("chghost");
    public static final IRCCapabilities ECHO_MESSAGE = new IRCCapabilities("echo-message");
    public static final IRCCapabilities EVENT_PLAYBACK = IRCCapabilities.draft("event-playback");
    public static final IRCCapabilities EXTENDED_JOIN = new IRCCapabilities("extended-join");
    public static final IRCCapabilities EXTENDED_MONITOR = new IRCCapabilities("extended-monitor");
    public static final IRCCapabilities INVITE_NOTIFY = new IRCCapabilities("invite-notify");
    public static final IRCCapabilities LABELLED_RESPONSE = new IRCCapabilities("labelled-response");
    public static final IRCCapabilities MESSAGE_TAGS = new IRCCapabilities("message-tags");
    public static final IRCCapabilities MONITOR = new IRCCapabilities("monitor");
    public static final IRCCapabilities MULTI_PREFIX = new IRCCapabilities("multi-prefix");
    public static final IRCCapabilities MULTILINE = IRCCapabilities.draft("multiline");
    public static final IRCCapabilities READ_MARKER = IRCCapabilities.draft("read-marker");
    public static final IRCCapabilities SASL = new IRCCapabilities("sasl");
    public static final IRCCapabilities SERVER_TIME = new IRCCapabilities("server-time");
    public static final IRCCapabilities SETNAME = new IRCCapabilities("setname");
    public static final IRCCapabilities STANDARD_REPLIES = new IRCCapabilities("standard-replies");
    @Deprecated
    public static final IRCCapabilities TLS = new IRCCapabilities("tls");
    public static final IRCCapabilities USERHOST_IN_NAMES = new IRCCapabilities("userhost-in-names");

    public static final IRCCapabilities TWITCH_COMMANDS = new IRCCapabilities("twitch.tv", "commands");
    public static final IRCCapabilities TWITCH_MEMBERSHIP = new IRCCapabilities("twitch.tv", "membership");
    public static final IRCCapabilities TWITCH_TAGS = new IRCCapabilities("twitch.tv", "tags");

    public IRCCapabilities(String name) {
        this(null, name);
    }

    public static IRCCapabilities parse(String capability) {
        String[] split = capability.split("/", 2);
        if (split.length == 1) {
            return new IRCCapabilities(split[0]);
        } else {
            return new IRCCapabilities(split[0], split[1]);
        }
    }

    public static IRCCapabilities draft(String name) {
        return new IRCCapabilities("draft", name);
    }

    @Override
    public String toString() {
        return (namespace == null ? "" : namespace + "/") + name;
    }

    public boolean isDraft() {
        return namespace != null && namespace.equals("draft");
    }
}
