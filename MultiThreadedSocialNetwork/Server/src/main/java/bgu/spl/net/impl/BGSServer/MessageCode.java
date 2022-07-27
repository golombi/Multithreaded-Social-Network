package bgu.spl.net.impl.BGSServer;

import bgu.spl.net.impl.BGSServer.Messages.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageCode {


    private static class SingletonHolder{
        private static final MessageCode instance = new MessageCode();
    }
    public static MessageCode getInstance(){
        return SingletonHolder.instance;
    }

    private final Map<Short, Class<? extends Message>> operationCodeToMessageType;
    private final Map<Class<? extends Message>, Short> messageTypeToOperationCode;

    private MessageCode(){
        operationCodeToMessageType = new ConcurrentHashMap<>();
        operationCodeToMessageType.put((short)1, REGISTER.class);
        operationCodeToMessageType.put((short)2, LOGIN.class);
        operationCodeToMessageType.put((short)3, LOGOUT.class);
        operationCodeToMessageType.put((short)4, FOLLOW.class);
        operationCodeToMessageType.put((short)5, POST.class);
        operationCodeToMessageType.put((short)6, PM.class);
        operationCodeToMessageType.put((short)7, LOGSTAT.class);
        operationCodeToMessageType.put((short)8, STAT.class);
        operationCodeToMessageType.put((short)9, NOTIFICATION.class);
        operationCodeToMessageType.put((short)10, ACK.class);
        operationCodeToMessageType.put((short)11, ERROR.class);
        operationCodeToMessageType.put((short)12, BLOCK.class);

        messageTypeToOperationCode = new ConcurrentHashMap<>();
        for(Short code : operationCodeToMessageType.keySet()){
            messageTypeToOperationCode.put(getMessageType(code), code);
        }
    }

    public Class<? extends Message> getMessageType(short op_code){
        return operationCodeToMessageType.get(op_code);
    }

    public short getCode(Class<? extends Message> messageClass){
        return messageTypeToOperationCode.get(messageClass);
    }

    public short getCode(Message msg){
        return getCode(msg.getClass());
    }
}
