package bgu.spl.net.impl.BGSServer;

import bgu.spl.net.impl.BGSServer.Messages.Message;
import bgu.spl.net.srv.Server;

public class ReactorMain {
    public static void main(String[] args){
        //args[0] = port
        //args[1] = numOfThreads
        Server<Message> srv =
                Server.reactor(Integer.parseInt(args[1]), Integer.parseInt(args[0]), BGSProtocol::new, BGSEncoderDecoder::new);
        srv.serve();
    }
}
