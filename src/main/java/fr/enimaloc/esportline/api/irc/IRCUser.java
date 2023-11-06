package fr.enimaloc.esportline.api.irc;

import fr.enimaloc.esportline.api.irc.twitch.TwitchIRCClient;

public class IRCUser {
    protected final TwitchIRCClient client;
    protected String nickname;
    protected String username;
    protected String hostname;

    public IRCUser(TwitchIRCClient client, String nickname, String username, String hostname) {
        this.client = client;
        this.nickname = nickname;
        this.username = username;
        this.hostname = hostname;
    }

    public IRCUser(TwitchIRCClient client, String raw) {
        this.client = client;
        raw = raw.trim();
        if (raw.contains("@")) {
            String[] split = raw.split("@", 2);
            this.hostname = split[1];
            if (split[0].contains("!")) {
                String[] split1 = split[0].split("!", 2);
                this.nickname = split1[0];
                this.username = split1[1];
            } else {
                this.nickname = split[0];
            }
        } else {
            this.hostname = raw;
        }
    }

    public static IRCUser parse(TwitchIRCClient client, String user) {
        String[] split = user.split("!", 2);
        if (split.length == 1) {
            return new IRCUser(client, split[0], null, null);
        } else {
            String[] split1 = split[1].split("@", 2);
            if (split1.length == 1) {
                return new IRCUser(client, split[0], split1[0], null);
            } else {
                return new IRCUser(client, split[0], split1[0], split1[1]);
            }
        }
    }

    public String getNickname() {
        return nickname;
    }

    public String getUsername() {
        return username;
    }

    public String getHostname() {
        return hostname;
    }

    @Override
    public String toString() {
        return nickname + (username == null ? "" : "!" + username) + (hostname == null ? "" : "@" + hostname);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IRCUser ircUser)) return false;

        return ircUser.toString().equals(toString());
    }
}
