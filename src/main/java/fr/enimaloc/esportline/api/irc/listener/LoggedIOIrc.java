package fr.enimaloc.esportline.api.irc.listener;

import fr.enimaloc.esportline.api.irc.IRCListener;
import fr.enimaloc.esportline.api.irc.IRCMessageRO;

public class LoggedIOIrc implements IRCListener<IRCMessageRO> {
    @Override
    public void sentMessage(IRCMessageRO message) {
        System.out.printf("<- %s%n", message.getRaw().replace("\r", "\\r").replace("\n", "\\n"));
    }

    @Override
    public void receivedMessage(IRCMessageRO message) {
        System.out.printf("-> %s%n", message.getRaw().replace("\r", "\\r").replace("\n", "\\n"));
    }
}
