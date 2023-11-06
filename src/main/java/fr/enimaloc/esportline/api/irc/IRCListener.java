package fr.enimaloc.esportline.api.irc;

public interface IRCListener<T extends IRCMessageRO> {
    default void sentMessage(T message) {
    }

    void receivedMessage(T message);
}
