package bgu.spl.net.impl.BGSServer.Messages;

import bgu.spl.net.api.bidi.Connections;
import bgu.spl.net.impl.BGSServer.DataBase;

public class POST extends Message{

    private String Content;

    public POST(){
        content = new Object[structure.length];
    }

    private static final String[] structure = {"short", "string"};
    @Override
    public String[] getStructure() {
        return structure;
    }

    @Override
    public void init(){
        op_code = (Short) content[0];
        Content = (String) content[1];
    }

    @Override
    public void execute(DataBase db, Connections<Message> connections, int connectionId) {
        if(db.post(Content, connections, connectionId)){
            connections.send(connectionId, new ACK(op_code));
        }else{
            connections.send(connectionId, new ERROR(op_code));
        }
    }
}
