package bgu.spl.net.api.bidi;
import bgu.spl.net.srv.bidi.ConnectionHandler;
import bgu.spl.net.srv.Server;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;
import bgu.spl.net.srv.BaseServer;
import bgu.spl.net.srv.BlockingConnectionHandler;
import bgu.spl.net.srv.Server;

import java.io.IOException;
import java.util.Map;
import java.util.function.Supplier;

public interface Connections<T> {

    boolean send(int connectionId, T msg);

    void broadcast(T msg);

    void disconnect(int connectionId);

}
