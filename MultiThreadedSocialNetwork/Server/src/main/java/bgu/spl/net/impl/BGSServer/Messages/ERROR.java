package bgu.spl.net.impl.BGSServer.Messages;

import bgu.spl.net.api.bidi.Connections;
import bgu.spl.net.impl.BGSServer.DataBase;

public class ERROR extends Message{
    private short message_op_code;

    // Additional data will be added through subclasses of this class
    public ERROR(){
        throw new RuntimeException("Can't create an empty ERROR message!");
    }

    public ERROR(short message_op_code){
        content = new Object[2];
        content[0] = codeMap.getCode(this);;
        content[1] = message_op_code;
    }

    public ERROR(Object[] content){
        this.content = content;
    }

    private static final String[] structure = {"short", "short"};
    @Override
    public String[] getStructure() {
        throw new RuntimeException("Structure isn't available for server-to-client messages");
    }

    @Override
    public void init() {
        throw new RuntimeException("ERROR messages can not be initialized");
    }

    @Override
    public void execute(DataBase db, Connections<Message> connections, int connectionId) {
        throw new RuntimeException("a non-executable message type");
    }
}
