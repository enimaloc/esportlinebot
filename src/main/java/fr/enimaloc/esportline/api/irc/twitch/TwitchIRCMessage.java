package fr.enimaloc.esportline.api.irc.twitch;

import fr.enimaloc.esportline.api.irc.IRCMessageRO;

public class TwitchIRCMessage extends IRCMessageRO {
    private String msgId;

    public TwitchIRCMessage(TwitchIRCClient client, String raw) {
        super(client, raw);
        if (tags != null) {
            for (String tag : tags) {
                String[] split = tag.split("=", 2);
                if (split[0].equals("msg-id")) {
                    msgId = split[1];
                    break;
                }
            }
        }
    }

    public String getMessageId() {
        return msgId;
    }
}
