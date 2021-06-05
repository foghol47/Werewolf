package org.stu.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ReadMessage implements Runnable{
    private Socket socket;
    private BufferedReader in;
    private Client client;

    public ReadMessage(Socket socket, Client client){
        this.socket = socket;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }
        catch (IOException e){
            e.printStackTrace();
        }
        this.client = client;
    }

    @Override
    public void run() {
        String input = "";
        while (!input.equals("!gameover")){
            try {
                input = in.readLine();
            }
            catch (IOException e){
                e.printStackTrace();
                break;
            }
            System.out.println(input);
        }
        client.stopWriteMessage();
    }
}
