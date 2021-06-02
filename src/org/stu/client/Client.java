package org.stu.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

import org.stu.elements.Role;

public class Client {
    private String username;
    private int port;
    private BufferedReader in;
    private PrintStream out;
    private Role role;

    public Client(int port){
        this.port = port;

        try(Socket socket = new Socket("127.0.0.1", port)) {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintStream(socket.getOutputStream());
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
}
