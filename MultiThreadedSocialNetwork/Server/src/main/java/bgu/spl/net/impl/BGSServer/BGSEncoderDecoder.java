package bgu.spl.net.impl.BGSServer;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.impl.BGSServer.Messages.*;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class BGSEncoderDecoder implements MessageEncoderDecoder<Message> {

    public static void main(String[] args){

         System.out.println("Start");
        BGSEncoderDecoder encoderDecoder = new BGSEncoderDecoder();
        byte zero = 0;
        byte one = 1;
        String username = "Itamar";
        String password = "Itamar2005";
        String birthday = "23/04/2005";
        encoderDecoder.decodeNextByte(zero);
        encoderDecoder.decodeNextByte(one);
        byte[] bytes;
        bytes = username.getBytes(StandardCharsets.UTF_8);
        for(byte b : bytes){
            encoderDecoder.decodeNextByte(b);
        }
        encoderDecoder.decodeNextByte(zero);
        bytes = password.getBytes(StandardCharsets.UTF_8);
        for(byte b : bytes){
            encoderDecoder.decodeNextByte(b);
        }
        encoderDecoder.decodeNextByte(zero);
        bytes = birthday.getBytes(StandardCharsets.UTF_8);
        for(byte b : bytes){
            encoderDecoder.decodeNextByte(b);
        }
        encoderDecoder.decodeNextByte(zero);


        Message msg = encoderDecoder.decodeNextByte(";".getBytes()[0]);
        for(Object o : msg.content){
            System.out.println(o);
        }
        bytes = encoderDecoder.encode(msg);
        Message org = null;
        for(byte b : bytes){
            org = encoderDecoder.decodeNextByte(b);
        }
        for(Object o: org.content){
            System.out.println(o);
        }

        /*
            byte first = 0;
       byte second = 3;
       BGSEncoderDecoder encoderDecoder = new BGSEncoderDecoder();
       encoderDecoder.decodeNextByte(first);
       Message msg = encoderDecoder.decodeNextByte(second);
       byte[] bytes = encoderDecoder.encode(msg);
        for(byte b : bytes){
            System.out.print(b);
        }
         */

    }

    private final MessageCode codeMap;
    private byte byte1; // A byte or the Most Significant Byte in a short
    private short op_code;
    private int messageIndex;
    private Message msg;
    private LineMessageEncoderDecoder lineMessageEncoderDecoder;


    public BGSEncoderDecoder(){
        codeMap = MessageCode.getInstance();

        op_code = -1;
        messageIndex = 0;
    }

    @Override
    public Message decodeNextByte(byte nextByte) {

        if (nextByte == ';') {
            messageIndex = 0;
            op_code = -1;
            msg.init();
            return msg;
        }

        if(op_code == -1){
            byte1 = nextByte;
            op_code++;
        }else if(op_code == 0){
            op_code = bytesToShort(byte1, nextByte);
            if(op_code <= 0 || op_code > 12){throw new RuntimeException("Illegal operation code.");}
            try {
                msg = codeMap.getMessageType(op_code).newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
                System.out.println("Failed to create a message instance");
            }
            msg.content[0] = op_code;
            messageIndex = 1;
            byte1 = -1;
            lineMessageEncoderDecoder = null;
        }else if(messageIndex < msg.content.length){
            String type = msg.getStructure()[messageIndex];
            if(type.equals("string")){
                if(lineMessageEncoderDecoder == null) {
                    lineMessageEncoderDecoder = new LineMessageEncoderDecoder();
                }
                msg.content[messageIndex] = lineMessageEncoderDecoder.decodeNextByte(nextByte);
            }else if(type.equals("short")){
                if(byte1 == -1){
                    byte1 = nextByte;
                }else{
                    msg.content[messageIndex] = bytesToShort(byte1, nextByte);
                    byte1 = -1;
                }
            }else if(type.equals("byte")){
                msg.content[messageIndex] = nextByte;
            }else{
                throw new RuntimeException("No such type");
            }
            if (msg.content[messageIndex] != null) {
                lineMessageEncoderDecoder = null;
                messageIndex++;
            }
        }
        /*
        We don't need this bunch of code as ';' handles the return of the message
          if(op_code > 0 && messageIndex == msg.content.length){...}
         */
        return null;
    }

    @Override
    public byte[] encode(Message message){
        lineMessageEncoderDecoder = new LineMessageEncoderDecoder();
        int len = 0;
        byte[] bytes = new byte[1 << 10]; // 1kb
        byte[] bytesToAdd;
        for(Object o : message.content){
            if(o instanceof String){
                bytesToAdd = lineMessageEncoderDecoder.encode((String) o);
            }else if(o instanceof Short){
                bytesToAdd = shortToBytes((Short) o);
            }else if(o instanceof Byte){
                bytesToAdd = new byte[1];
                bytesToAdd[0] = (Byte)(o);
            }else{
                StringBuilder prevFields = new StringBuilder();
                for(Object prv : msg.content){
                    if(o != null){
                        prevFields.append(" ").append(prv);
                    }
                }
                throw new RuntimeException("Illegal message component: " + prevFields);
            }

            //Push new bytes
            len += bytesToAdd.length;
            if(len >= bytes.length){
                bytes = Arrays.copyOf(bytes, 2*len);
            }
            System.arraycopy(bytesToAdd, 0, bytes, len - bytesToAdd.length, bytesToAdd.length);
        }
        byte[] ans = new byte[len+1];
        System.arraycopy(bytes, 0, ans, 0, len);
        ans[len] = ";".getBytes(StandardCharsets.UTF_8)[0];
        return ans;
    }

    private short bytesToShort(byte n1, byte n2){
        short result = (short)((n1 & 0xff) << 8);
        result += (short)(n2 & 0xff);
        return result;
    }

    private byte[] shortToBytes(short num) {
        byte[] bytesArr = new byte[2];
        bytesArr[0] = (byte)((num >> 8) & 0xFF);
        bytesArr[1] = (byte)(num & 0xFF);
        return bytesArr;
    }

}
