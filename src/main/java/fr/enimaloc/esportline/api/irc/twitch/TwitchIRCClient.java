package fr.enimaloc.esportline.api.irc.twitch;

import fr.enimaloc.esportline.api.irc.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class TwitchIRCClient {
    private final SocketAddress address;
    private final InternalListener internalListener = new InternalListener();
    private final List<IRCListener> listeners;
    private final List<TwitchIRCChannel> channels = new ArrayList<>();
    private final Map<String, CompletableFuture<?>> futures = new HashMap<>();
    private boolean connected;
    private Thread listener;
    private Socket socket;
    private OutputStream output;
    private IRCSelfUser self;

    public TwitchIRCClient(IRCListener<? extends IRCMessageRO>... listeners) {
        this.address = new InetSocketAddress("irc.chat.twitch.tv", 6667);
        this.listeners = new ArrayList<>(Arrays.asList(listeners));
    }

    public void connect(String password, String nickname) throws IOException {
        connect();
        if (!password.startsWith("oauth:")) {
            password = "oauth:" + password;
        }
        send("PASS " + password);
        send("NICK " + nickname);
        self = new IRCSelfUser(this, nickname, nickname, nickname + ".tmi.twitch.tv");
    }

    public void connect(String password, String nickname, IRCCapabilities... capabilities) throws IOException {
        connect();
        if (!password.startsWith("oauth:")) {
            password = "oauth:" + password;
        }
        send("PASS " + password);
        send("NICK " + nickname);
        send("CAP REQ :" + String.join(" ", Arrays.stream(capabilities).map(IRCCapabilities::toString).toArray(String[]::new)));
        send("CAP END");
        self = new IRCSelfUser(this, nickname, nickname, nickname + ".tmi.twitch.tv");
    }

    public void connect() throws IOException {
        if (connected) {
            throw new IllegalStateException("Already connected");
        }
        socket = new Socket();
        socket.connect(address);
        listener = new Thread(new Listener(socket.getInputStream()));
        listener.start();
        output = socket.getOutputStream();
    }

    public void send(String message) {
        TwitchIRCMessage msg = new TwitchIRCMessage(this, message);
        internalListener.sentMessage(msg);
        listeners.forEach(listener -> listener.sentMessage(msg));
        try {
            output.write(msg.getRaw().getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void send(IRCMessageReceiver receiver, String message) {
        send("PRIVMSG " + receiver + " :" + message);
    }

    public CompletableFuture<TwitchIRCChannel> join(String channel) {
        send("JOIN " + channel);
        CompletableFuture<TwitchIRCChannel> future = new CompletableFuture<>();
        futures.put(channel, future);
        return future;
    }

    public Optional<TwitchIRCChannel> getChannel(String channel) {
        return channels.stream()
                .filter(c -> c.getName().equals(channel))
                .findFirst();
    }

    public IRCSelfUser getSelfUser() {
        return self;
    }

    private class Listener implements Runnable {

        private final InputStream input;

        public Listener(InputStream inputStream) {
            this.input = inputStream;
        }

        @Override
        public void run() {
            StringBuilder builder = new StringBuilder();
            while (!Thread.interrupted()) {
                try {
                    int read = input.read();
                    if (read == -1) {
                        break;
                    }
                    char c = (char) read;
                    if (c == '\n') {
                        String message = builder.toString();
                        message = message.replace("\r", "");
                        TwitchIRCMessage msg = new TwitchIRCMessage(TwitchIRCClient.this, message);
                        internalListener.receivedMessage(msg);
                        listeners.forEach(listener -> listener.receivedMessage(msg));
                        builder = new StringBuilder();
                    } else {
                        builder.append(c);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private class InternalListener implements IRCListener<TwitchIRCMessage> {
        @Override
        public void sentMessage(TwitchIRCMessage message) {
        }

        @Override
        public void receivedMessage(TwitchIRCMessage message) {
            switch (message.getCommand()) {
                case "001":
                    connected = true;
                    break;
                case "JOIN":
                    if (message.getSender().equals(self)) {
                        TwitchIRCChannel channel = new TwitchIRCChannel(TwitchIRCClient.this, message.getParams()[0]);
                        channels.add(channel);
                        CompletableFuture<IRCChannel> future = (CompletableFuture<IRCChannel>) futures.remove(message.getParams()[0]);
                        if (future != null) {
                            future.complete(channel);
                        }
                    } else {
                        getChannel(message.getParams()[0]).ifPresent(channel -> channel.getUsers().add(message.getSender()));
                    }
                    break;
                case "353":
                    getChannel(message.getParams()[2]).ifPresent(channel -> Arrays.stream(message.getTrailing().split(" "))
                            .map(user -> IRCUser.parse(TwitchIRCClient.this, user))
                            .forEach(channel.getUsers()::add));
                    break;
                case "PART":
                    if (message.getSender().equals(self)) {
                        channels.removeIf(channel -> channel.getName().equals(message.getParams()[0]));
                    } else {
                        getChannel(message.getParams()[0]).ifPresent(channel -> channel.getUsers().remove(message.getSender()));
                    }
                    break;
                case "ROOMSTATE":
                    getChannel(message.getParams()[0]).ifPresent(channel -> channel.roomState(message.getTags()));
                    break;
            }
        }
    }
}
