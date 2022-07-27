package bgu.spl.net.impl.BGSServer.Messages;

import bgu.spl.net.api.bidi.Connections;
import bgu.spl.net.impl.BGSServer.DataBase;
import bgu.spl.net.impl.BGSServer.MessageCode;

import java.util.ArrayList;

public abstract class Message {

    protected MessageCode codeMap = MessageCode.getInstance();
    protected short op_code;
    public static String[] structure;
    public Object[] content; // The data the message is built of

    //Returns a static array that specifies the message data types and their order
    public abstract String[] getStructure();
    //Initialize the Message fields with the values of 'content'
    public abstract void init();
    //Executes the appropriate action from the client's('connectionId') point of view
    public abstract void execute(DataBase db, Connections<Message> connections, int connectionId);
}
