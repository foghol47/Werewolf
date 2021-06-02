package org.stu.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Server {
    private int port;
    private ArrayList<String> userNames;
    private ArrayList<PlayerHandler> players;
    private ServerSocket serverSocket;
    private int currentPlayers;
    private final String chatFileName = "chats.txt";

    public Server(int port){
        this.port = port;
        currentPlayers = 0;
        userNames = new ArrayList<>();

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("server started.");
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    public void startServer(){
        System.out.println("enter number of players:");
        Scanner scanner = new Scanner(System.in);
        int playersNumber = scanner.nextInt();

        while (currentPlayers < playersNumber){
            try(Socket player = serverSocket.accept()){
                players.add(new PlayerHandler(this, player));
                currentPlayers++;
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }

    }
}
