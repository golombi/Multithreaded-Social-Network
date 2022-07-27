package bgu.spl.net.api.bidi;

import bgu.spl.net.impl.BGSServer.DataBase;
import bgu.spl.net.srv.bidi.ConnectionHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionsImpl<T> implements Connections<T> {
    private Map<Integer, ConnectionHandler<T>> hashMap;

    public ConnectionsImpl(){
        hashMap = new ConcurrentHashMap<>();
    }

    @Override
    public boolean send(int connectionId, T msg) {
        ConnectionHandler<T> handler = hashMap.get(connectionId);
        if(handler == null){return false;}
        // Todo: optimize synchronization
        //  (We wait until the whole message is sent to the same client by multiple threads)
        handler.send(msg);
        return true;
    }

    @Override
    public void broadcast(T msg) {
        for(Integer i : hashMap.keySet()){
            send(i, msg);
        }
    }

    @Override
    public void disconnect(int connectionId) {
        hashMap.remove(connectionId);
    }

    public void add(int connectionId, ConnectionHandler<T> handler){
        hashMap.put(connectionId, handler);
    }
}
