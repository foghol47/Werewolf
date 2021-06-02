package org.stu.server;

import java.net.Socket;

public class PlayerHandler implements Runnable{
    private Server server;
    private Socket socket;

    public PlayerHandler(Server server, Socket socket){
        this.server = server;
        this.socket = socket;
    }



    @Override
    public void run() {

    }
}
