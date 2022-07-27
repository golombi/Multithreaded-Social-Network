package bgu.spl.net.impl.BGSServer.Messages;

import bgu.spl.net.api.bidi.Connections;
import bgu.spl.net.impl.BGSServer.DataBase;

public class LOGOUT extends Message{

    public LOGOUT(){
        content = new Object[structure.length];
    }

    private static final String[] structure = {"short"};
    @Override
    public String[] getStructure() {
        return structure;
    }

    @Override
    public void init(){
        op_code = (Short) content[0];
    }

    @Override
    public void execute(DataBase db, Connections<Message> connections, int connectionId) {
        if(db.logout(connectionId)){
            connections.send(connectionId, new ACK(op_code));
            connections.disconnect(connectionId);
        }else{
            connections.send(connectionId, new ERROR(op_code));
        }
    }
}
