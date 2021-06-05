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
    private int numberPlayers;
    private final String chatFileName = "chats.txt";

    public Server(int port){
        this.port = port;
        players = new ArrayList<>();
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
        numberPlayers = scanner.nextInt();

        while (currentPlayers < numberPlayers){
            try{
                Socket player = serverSocket.accept();
                PlayerHandler newPlayer = new PlayerHandler(this, player);
                players.add(newPlayer);
                new Thread(newPlayer).start();

                currentPlayers++;
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }

    }

    public synchronized void incrementPlayer(){
        currentPlayers++;
        if (currentPlayers == numberPlayers)
            notifyPlayers();

    }

    public void notifyPlayers(){
        for (PlayerHandler handler: players)
            handler.notify();
    }

    public boolean isValidUserName(String userName){
        return !players.contains(userName);
    }

    public void addUserName(String userName){
        userNames.add(userName);
    }
}
