package org.stu.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * this class used to read all messages from the server.
 *
 * @author Alireza Jabbari
 */
public class ReadMessage implements Runnable{
    private Socket socket;
    private BufferedReader in;
    private Client client;

    /**
     * Instantiates a new Read message.
     *
     * @param socket the socket
     * @param client the client
     */
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
        while (!input.equals("!exit")){
            try {
                input = in.readLine();
            }
            catch (IOException e){
                System.out.println("disconnected from server");
                break;
            }
            System.out.println(input);
        }
        client.stopWriteMessage();
    }
}
