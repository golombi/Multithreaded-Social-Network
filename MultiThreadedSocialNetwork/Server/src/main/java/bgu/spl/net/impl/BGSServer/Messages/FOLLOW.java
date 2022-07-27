package bgu.spl.net.impl.BGSServer.Messages;

import bgu.spl.net.api.bidi.Connections;
import bgu.spl.net.impl.BGSServer.DataBase;

public class FOLLOW extends Message{

    private byte FOLLOW;
    private String username;

    public FOLLOW(){
        //TODO: Make sure the FOLLOW String ends with a 0-byte (According to the question in the forum)
        content = new Object[structure.length];
    }

    private static final String[] structure = {"short", "byte", "string"};
    @Override
    public String[] getStructure() {
        return structure;
    }

    @Override
    public void init(){
        op_code = (Short) content[0];
        FOLLOW = (Byte) content[1];
        username = (String) content[2];
    }

    @Override
    public void execute(DataBase db, Connections<Message> connections, int connectionId) {
        //0 for follow, 1 for unfollow
        if(db.follow(username, FOLLOW, connectionId)){
            connections.send(connectionId, new ACK(new Object[]{codeMap.getCode(ACK.class) ,op_code, username}));
        }else{
            connections.send(connectionId, new ERROR(op_code));
        }
    }
}
