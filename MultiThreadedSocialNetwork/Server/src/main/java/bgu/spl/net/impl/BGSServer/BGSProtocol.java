package bgu.spl.net.impl.BGSServer;

import bgu.spl.net.api.bidi.BidiMessagingProtocol;
import bgu.spl.net.api.bidi.Connections;
import bgu.spl.net.impl.BGSServer.Messages.Message;

public class BGSProtocol implements BidiMessagingProtocol<Message> {
    private DataBase db;
    private Connections<Message> connections;
    private int connectionId;

    @Override
    public void start(int connectionId, Connections<Message> connections) {
        this.db = DataBase.getInstance();
        this.connections = connections;
        this.connectionId = connectionId;
    }

    @Override
    public void process(Message message) {
        message.execute(db, connections, connectionId);
    }

    @Override
    public boolean shouldTerminate() {
        return false;
    }
}
