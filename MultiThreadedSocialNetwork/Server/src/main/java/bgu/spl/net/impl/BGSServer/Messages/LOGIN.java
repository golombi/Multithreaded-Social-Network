package bgu.spl.net.impl.BGSServer.Messages;

import bgu.spl.net.api.bidi.Connections;
import bgu.spl.net.impl.BGSServer.DataBase;

public class LOGIN extends Message{

    private String username;
    private String password;
    private byte captcha;

    public LOGIN(){
        content = new Object[structure.length];
    }

    private static final String[] structure = {"short", "string", "string", "byte"};
    @Override
    public String[] getStructure() {
        return structure;
    }

    @Override
    public void init(){
        op_code = (Short) content[0];
        username = (String) content[1];
        password = (String) content[2];
        captcha = (Byte) content[3];
    }

    @Override
    public void execute(DataBase db, Connections<Message> connections, int connectionId) {
        if(db.login(username, password, captcha, connectionId, connections)){
            connections.send(connectionId, new ACK(op_code));
        }else{
            connections.send(connectionId, new ERROR(op_code));
        }
    }
}
