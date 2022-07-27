package bgu.spl.net.impl.BGSServer;

import bgu.spl.net.impl.BGSServer.Messages.Message;
import bgu.spl.net.srv.Server;

public class TPCMain {
    public static void main(String[] args){
        //args[0] port
        Server<Message> srv =
                Server.threadPerClient(Integer.parseInt(args[0]), BGSProtocol::new, BGSEncoderDecoder::new);
        srv.serve();
    }
}
