package bgu.spl.net.impl.BGSServer.Messages;

import bgu.spl.net.api.bidi.Connections;
import bgu.spl.net.impl.BGSServer.DataBase;

public class PM extends Message {

    private String username;
    private String Content;
    private String sendingDateAndTime;

    public PM(){
        content = new Object[structure.length];
    }

    private static final String[] structure = {"short", "string", "string", "string"};
    @Override
    public String[] getStructure() {
        return structure;
    }


    @Override
    public void init(){
        op_code = (Short) content[0];
        username = (String) content[1];
        Content = (String) content[2];
        sendingDateAndTime = (String) content[3];
    }

    @Override
    public void execute(DataBase db, Connections<Message> connections, int connectionId) {
        if(db.PM(username, Content, sendingDateAndTime, connections, connectionId)){
            connections.send(connectionId, new ACK(op_code));
        }else{
            connections.send(connectionId, new ERROR(op_code));
        }
    }
}
