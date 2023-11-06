package fr.enimaloc.esportline.api.irc.listener;

import fr.enimaloc.esportline.api.irc.IRCListener;
import fr.enimaloc.esportline.api.irc.IRCMessageRO;

public class PingPongIRC implements IRCListener<IRCMessageRO> {
    @Override
    public void sentMessage(IRCMessageRO message) {

    }

    @Override
    public void receivedMessage(IRCMessageRO message) {
        if (message.getCommand().equals("PING")) {
            message.getClient().send("PONG " + message.getTrailing());
        }
    }
}
