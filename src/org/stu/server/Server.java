package org.stu.server;

import org.stu.elements.*;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;
import java.util.concurrent.*;

public class Server {
    private int port;
    private ArrayList<String> userNames;
    private ArrayList<PlayerHandler> players;
    private ServerSocket serverSocket;
    private int currentPlayers;
    private int numberPlayers;
    private int readyPlayers;
    private final String chatFileName = "chats.txt";

    public Server(int port){
        this.port = port;
        players = new ArrayList<>();
        currentPlayers = 0;
        readyPlayers = 0;
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
        ExecutorService pool = Executors.newCachedThreadPool();

        while (currentPlayers < numberPlayers){
            try{
                Socket player = serverSocket.accept();
                PlayerHandler newPlayer = new PlayerHandler(this, player);
                players.add(newPlayer);
//                Thread t = new Thread(newPlayer);
                newPlayer.setTask(Task.INIT_USERNAME);
//                t.start();
//                Thread.sleep(1000);
//                t.stop();
//                pool.schedule(newPlayer, 5, TimeUnit.SECONDS);
//                pool.submit(newPlayer);
//                pool.shutdownNow();
                pool.execute(newPlayer);
//                pool.shutdownNow();
//                pool.awaitTermination(1, TimeUnit.SECONDS);



                currentPlayers++;
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
        pool.shutdown();
        try {
            pool.awaitTermination(30, TimeUnit.SECONDS);
        }
        catch (InterruptedException e){
            e.printStackTrace();
        }
        //pool.shutdownNow();
        gameLoop();


    }

    public void gameLoop(){
        gettingReady();
        createRoles();
        while (!gameOver()){
            day();
            if (gameOver())
                break;
            voting();

        }
    }

    public void voting(){

    }
//    public void setTask(Task task){
//        for (PlayerHandler player: players){
//            player.setTask(task);
//        }
//    }

    public boolean gameOver(){
        return mafiaNumber() == 0 || mafiaNumber() >= citizenNumber();
    }

    public int mafiaNumber(){
        int count = 0;
        for (PlayerHandler player: players){
            if (player.getRole() instanceof Mafia)
                count++;
        }
        return count;
    }
    public int citizenNumber(){
        int count = 0;
        for (PlayerHandler player: players){
            if (player.getRole() instanceof Citizen)
                count++;
        }
        return count;
    }

    public void day(){
        ExecutorService pool = Executors.newCachedThreadPool();
        for (PlayerHandler player: players){
            player.setTask(Task.DAY);
            pool.execute(player);
        }
        pool.shutdown();
        try {
            if (!pool.awaitTermination(40, TimeUnit.SECONDS))
                pool.shutdownNow();
            sendMessageToAll(null, "Time over and day ended.", false);

        }
        catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    public void gettingReady(){
        ExecutorService pool = Executors.newCachedThreadPool();
        for (PlayerHandler player: players){
            player.setTask(Task.GETTING_READY);
            pool.execute(player);
        }
        pool.shutdown();
        try {
            if (!pool.awaitTermination(10, TimeUnit.SECONDS))
                pool.shutdownNow();
        }
        catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    public void createRoles(){
        int mafiaNumber = numberPlayers / 3;
        int citizenNumber = numberPlayers - mafiaNumber;
        ArrayList<Role> roles = new ArrayList<>();

//        roles.add(new GodFather());
//        roles.add(new DrLecter());
//        roles.add(new OrdinaryMafia());

        roles.add(new Detective());
        roles.add(new Doctor());
//        roles.add(new Sniper());
//        roles.add(new DieHard());
//        roles.add(new Psychologist());
        roles.add(new Mayor());
        roles.add(new OrdinaryCitizen());

        Collections.shuffle(roles);

        for (PlayerHandler player: players){
            player.setRole(roles.get(0));
            roles.remove(0);
            player.getRoles();
        }

    }


    public synchronized void sendMessageToAll(PlayerHandler except, String message, boolean savedInFile){
        for (PlayerHandler player: players){
            if (!player.equals(except))
                player.receiveMessage(message);
        }
        if (savedInFile) {
            try (FileOutputStream file = new FileOutputStream(chatFileName, true)) {
                PrintStream writer = new PrintStream(file);
                writer.println(message);
                writer.close();
            } catch (FileNotFoundException e) {
                System.err.println("file not found");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String findRoleUserName(Role role){
        String string = "";
        for (PlayerHandler player: players){
            if (player.getRole().getClass().equals(role.getClass()))
                string = player.getUserName();

        }
        return string;
    }

//    public synchronized void incrementPlayer(){
//        currentPlayers++;
//        if (currentPlayers == numberPlayers)
//            notifyPlayers();
//
//    }

//    public void notifyPlayers(){
//        for (PlayerHandler handler: players)
//            new Thread(handler).notify();
//    }


//    public boolean allPlayersFinished(PlayerHandler except){
//        if (currentPlayers != numberPlayers)
//            return false;
//        for (PlayerHandler handler: players){
//            if (handler.equals(except))
//                continue;
//            if (!handler.getTaskFinished())
//                return false;
//        }
//        return true;
//    }

    public synchronized boolean isValidUserName(String userName){
        for (String users: userNames){
            if (users.equals(userName))
                return false;
        }
        return true;
    }

    public synchronized void addUserName(String userName){
        userNames.add(userName);
    }

    public synchronized void incrementReadyPlayers(){
        readyPlayers++;

    }
}
