package org.stu.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class PlayerHandler implements Runnable{
    private Server server;
    private Socket socket;
    private BufferedReader in;
    private PrintStream out;
    private String userName;

    public PlayerHandler(Server server, Socket socket){
        this.server = server;
        this.socket = socket;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintStream(socket.getOutputStream());
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }



    @Override
    public void run() {
        initUserName();

    }

    public void initUserName(){
        System.out.println("Yoodada");
        String userNameTemp = "";
        while (true){
            out.println("enter your username:");
            try {
                userNameTemp = in.readLine();
            }
            catch (IOException e){
                e.printStackTrace();
            }
            if (server.isValidUserName(userNameTemp))
                break;
            else
                out.println("this username exists.");
        }
        userName = userNameTemp;
        server.addUserName(userName);

    }
}
