package org.stu.server;

import org.stu.elements.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
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

    private HashMap<PlayerHandler, Integer> playerVotes;
    private PlayerHandler votedPlayer;

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
            pool.awaitTermination(1, TimeUnit.DAYS);
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

        voting();
        showFirstResultVoting();
        showFinalResultVoting();

        while (!gameOver()){
            day();
            if (gameOver())
                break;
            voting();
            showFirstResultVoting();
            showFinalResultVoting();

        }
        scoreBoard();
        disconnectPlayers();
    }

    public void disconnectPlayers(){
        for (PlayerHandler player: players){
            Socket socket = player.getSocket();
            if (!socket.isClosed()){
                player.receiveMessage("!exit");
                try {
                    socket.close();
                }
                catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    public void showFirstResultVoting(){
        Set<PlayerHandler> votingPlayers = playerVotes.keySet();
        for (PlayerHandler player: votingPlayers){
            String message = player.getUserName() + " " + playerVotes.get(player) + " votes.";
            sendMessageToAll(null, message, false);
        }
        Iterator<PlayerHandler> iterator = votingPlayers.iterator();
        PlayerHandler maxVoted = iterator.next();
        while (iterator.hasNext()){
            PlayerHandler temp = iterator.next();
            if (playerVotes.get(temp) > playerVotes.get(maxVoted))
                maxVoted = temp;
        }
        votedPlayer = maxVoted;

    }

    public void showFinalResultVoting(){
        ExecutorService pool = Executors.newCachedThreadPool();
//        sendMessageToAll(findRolePlayer(new Mayor()), "wait for mayor choice...", false);
        for (PlayerHandler player: players){
            player.setTask(Task.MAYOR_ACT);
            pool.execute(player);
        }
        pool.shutdown();
        try {
            if (!pool.awaitTermination(20, TimeUnit.SECONDS))
                pool.shutdownNow();
            sendMessageToAll(null, "Time over and voting ended.", false);

        }
        catch (InterruptedException e){
            e.printStackTrace();
        }
        if (votedPlayer == null)
            sendMessageToAll(null, "no one removed from game.", false);
        else {
            sendMessageToAll(null, votedPlayer.getUserName() + " removed from game.", false);
            votedPlayer.kill();
        }
    }

    public synchronized void vote(PlayerHandler vote){
        int value = playerVotes.get(vote);
        playerVotes.replace(vote, value, value + 1);
    }

    public synchronized void showHistory(PrintStream out){
        try (FileInputStream file = new FileInputStream(chatFileName) ) {
            Scanner scanner = new Scanner(file);
            while (scanner.hasNext()){
                String temp = scanner.nextLine();
                out.println(temp);
            }
        } catch (FileNotFoundException e) {
            System.err.println("file not found");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    public void incrementVote(PlayerHandler )

    public void voting(){
        playerVotes = new HashMap<>();
        for (PlayerHandler player: players){
            if (player.isAlive())
                playerVotes.put(player, 0);
        }

        ExecutorService pool = Executors.newCachedThreadPool();
        for (PlayerHandler player: players){
            player.setTask(Task.VOTING);
            pool.execute(player);
        }
        pool.shutdown();
        try {
            if (!pool.awaitTermination(40, TimeUnit.SECONDS))
                pool.shutdownNow();
            sendMessageToAll(null, "Time over and voting ended.", false);

        }
        catch (InterruptedException e){
            e.printStackTrace();
        }

    }

    public void cancelVoting(){
        votedPlayer = null;
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
            if (player.getRole() instanceof Mafia && player.isAlive())
                count++;
        }
        return count;
    }
    public int citizenNumber(){
        int count = 0;
        for (PlayerHandler player: players){
            if (player.getRole() instanceof Citizen && player.isAlive())
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
            if (!pool.awaitTermination(1, TimeUnit.DAYS))
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

        roles.add(new GodFather());
//        roles.add(new DrLecter());
//        roles.add(new OrdinaryMafia());

//        roles.add(new Detective());
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
            player.notifyRoles();
        }

        for (PlayerHandler player: players){
            player.familiarRoles();
        }


    }


    public synchronized void sendMessageToAll(PlayerHandler except, String message, boolean savedInFile){
        for (PlayerHandler player: players){
            if (!player.equals(except) && !player.getSocket().isClosed())
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

    public PlayerHandler findRolePlayer(Role role){

        for (PlayerHandler player: players){
            if (player.getRole().getClass().equals(role.getClass()))
                return player;

        }
        return null;
    }

    public ArrayList<PlayerHandler> getMafiaTeam(){
        ArrayList<PlayerHandler> mafiaTeam = new ArrayList<>();
        for (PlayerHandler player: players){
            if (player.getRole() instanceof Mafia)
                mafiaTeam.add(player);
        }
        return mafiaTeam;
    }

    public void scoreBoard(){
        if (mafiaNumber() == 0)
            sendMessageToAll(null, "game ended\nCitizens won the game.", false);
        else
            sendMessageToAll(null, "game ended\nMafias won the game.", false);
        sendMessageToAll(null, "role names:", false);
        for (PlayerHandler player: players){
            sendMessageToAll(null, player.getRole().getClass().getSimpleName() + ": " + player.getUserName(), false);
        }

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

    public ArrayList<PlayerHandler> getPlayersInVote(PlayerHandler except){
        ArrayList<PlayerHandler> options = new ArrayList<>();
        for (PlayerHandler player: players){
            if (!player.equals(except))
                options.add(player);
        }
        return options;
    }

    public synchronized void addUserName(String userName){
        userNames.add(userName);
    }

    public synchronized void incrementReadyPlayers(){
        readyPlayers++;

    }
}
