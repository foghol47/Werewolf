package org.stu.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

import org.stu.elements.Role;

public class Client {
    private String username;
    private int port;
    private BufferedReader in;
    private PrintStream out;
    private Role role;
    private Socket socket1;
    private Thread readMessageThread;
    private Thread writeMessageThread;

    public Client(int port){
        this.port = port;

        try {
            Socket socket = new Socket("127.0.0.1", port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintStream(socket.getOutputStream());
            socket1 = socket;
        }
        catch (IOException e){
            e.printStackTrace();
        }
        readMessageThread = new Thread(new ReadMessage(socket1, this));
        writeMessageThread = new Thread(new WriteMessage(socket1));
    }

    public void execute(){
        readMessageThread.start();
        writeMessageThread.start();

    }

    public void stopWriteMessage(){
        writeMessageThread.stop();
    }
}
