package bgu.spl.net.impl.BGSServer.Messages;

import bgu.spl.net.api.bidi.Connections;
import bgu.spl.net.impl.BGSServer.DataBase;

public class NOTIFICATION extends Message{

    private byte NotificationType; // PM/Public
    private String postingUser;
    private String Content;

    public NOTIFICATION(){
        throw new RuntimeException("Can't create an empty NOTIFICATION message!");
    }

    public NOTIFICATION(byte notificationType, String postingUser, String Content){
        content = new Object[4];
        content[0] = codeMap.getCode(this);
        content[1] = notificationType;
        content[2] = postingUser;
        content[3] = Content;
    }

    private static final String[] structure = {"short", "byte", "string", "string"};
    @Override
    public String[] getStructure() {
        throw new RuntimeException("Structure isn't available for server-to-client messages");
    }

    @Override
    public void init(){
        throw new RuntimeException("NOTIFICATION messages can not be initialized");
    }

    @Override
    public void execute(DataBase db, Connections<Message> connections, int connectionId)  {
        throw new RuntimeException("a non-executable message type");
    }
}
