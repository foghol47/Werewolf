package org.stu.server;

import org.stu.elements.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

/**
 * this class creates a game server
 *
 * @author Alireza Jabbari
 */
public class Server {
    private int port;
    private ArrayList<String> userNames;
    private ArrayList<PlayerHandler> players;
    private ArrayList<PlayerHandler> totalPlayerKicked;
    private ServerSocket serverSocket;
    private int currentPlayers;
    private int numberPlayers;
    private final String chatFileName = "chats.txt";
    //day
    private HashMap<PlayerHandler, Integer> playerVotes;
    private PlayerHandler votedPlayer = null;
    //night
    private ArrayList<PlayerHandler> kickedPlayersInNight;
    private PlayerHandler shootedPlayer = null;
    private PlayerHandler drLecterSaved = null;
    private PlayerHandler sniperTarget = null;
    private boolean dieHardInquiry = false;
    private PlayerHandler silencedPlayer = null;
    private PlayerHandler savedPlayer = null;

    /**
     * Instantiates a new Server with given port.
     *
     * @param port the port of socketServer
     */
    public Server(int port){
        this.port = port;
        players = new ArrayList<>();
        currentPlayers = 0;
        userNames = new ArrayList<>();
        totalPlayerKicked = new ArrayList<>();

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("server started.");
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * executes game server and accept players.
     */
    public void startServer(){
        System.out.println("enter number of players:");
        Scanner scanner = new Scanner(System.in);
        numberPlayers = 0;
        while (numberPlayers < 6) {
            numberPlayers = scanner.nextInt();
            if (numberPlayers < 6)
                System.out.println("wrong input");
        }
        ExecutorService pool = Executors.newCachedThreadPool();

        while (currentPlayers < numberPlayers){
            try{
                Socket player = serverSocket.accept();
                PlayerHandler newPlayer = new PlayerHandler(this, player);
                players.add(newPlayer);
                newPlayer.setTask(Task.INIT_USERNAME);
                pool.execute(newPlayer);


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
        gameLoop();


    }

    /**
     * the loop of game.
     *
     * day ----> voting ----> night ----> day ...
     *
     * games ended if all mafias killed or number of mafias is greater or equal to number of citizens
     */
    public void gameLoop(){
        gettingReady();
        createRoles();

        while (!gameOver()){
            day();
            if (gameOver())
                break;
            voting();
            showFirstResultVoting();
            showFinalResultVoting();
            if (gameOver())
                break;
            night();
            nightResult();
        }
        scoreBoard();
        disconnectPlayers();
    }

    /**
     * Disconnect players safely after ending game.
     */
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

    /**
     * Show first result of voting.
     */
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
        if (playerVotes.get(maxVoted) != 0)
            votedPlayer = maxVoted;
        else
            votedPlayer = null;

    }

    /**
     * Show final result of voting after mayor act.
     */
    public void showFinalResultVoting(){
        if (votedPlayer == null) {
            sendMessageToAll(null, "no one removed from game.", false);
            return;
        }
        ExecutorService pool = Executors.newCachedThreadPool();
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

    /**
     * Vote to a player.
     *
     * @param vote voted player
     */
    public synchronized void vote(PlayerHandler vote){
        int value = playerVotes.get(vote);
        playerVotes.replace(vote, value, value + 1);
    }

    /**
     * Show day chat history to a player
     *
     * @param out the output stream of player
     */
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


    /**
     * Voting phase of game.
     */
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

    public void night(){
        shootedPlayer = null;
        drLecterSaved = null;
        sniperTarget = null;
        dieHardInquiry = false;
        silencedPlayer = null;
        savedPlayer = null;

        ExecutorService pool = Executors.newCachedThreadPool();
        for (PlayerHandler player: players){
            player.setTask(Task.NIGHT);
            pool.execute(player);
        }
        pool.shutdown();
        try {
            if (!pool.awaitTermination(2, TimeUnit.MINUTES))
                pool.shutdownNow();
            sendMessageToAll(null, "Time over and night ended.", false);

        }
        catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    public void nightResult(){
        kickedPlayersInNight = new ArrayList<>();

        if (shootedPlayer != null){
            if (!shootedPlayer.equals(savedPlayer)){
                if (shootedPlayer.getRole() instanceof DieHard){
                    DieHard dieHard = (DieHard) shootedPlayer.getRole();
                    if (!dieHard.haveShield())
                        kickedPlayersInNight.add(shootedPlayer);
                    else
                        dieHard.useShield();
                }
            }
        }

        if (sniperTarget != null){
            if (sniperTarget.getRole() instanceof Citizen){
                PlayerHandler sniper = findRolePlayer(new Sniper());
                kickedPlayersInNight.add(sniper);
            }
            else {
                if (!sniperTarget.equals(drLecterSaved)){
                    kickedPlayersInNight.add(sniperTarget);
                }
            }
        }

        if (silencedPlayer != null){
            sendMessageToAll(null, silencedPlayer.getUserName() + " silenced today.\n", false);
            silencedPlayer.setSilenced(true);
        }

        Collections.shuffle(kickedPlayersInNight);
        for (PlayerHandler player: kickedPlayersInNight){
            sendMessageToAll(null, player.getUserName() + " killed tonight.", false);
            player.kill();
            totalPlayerKicked.add(player);
        }

        if (dieHardInquiry) {
            Collections.shuffle(totalPlayerKicked);
            if (totalPlayerKicked.size() == 0)
                sendMessageToAll(null, "dieHard inquiry: no one removed from game", false);
            sendMessageToAll(null, "dieHard inquiry: these roles kicked from game:", false);
            for (PlayerHandler player : totalPlayerKicked){
                sendMessageToAll(null, player.getRole().getClass().getSimpleName(), false);
            }
            sendMessageToAll(null, "\n", false);
        }

    }

    /**
     * Cancel voting (called by mayor).
     */
    public void cancelVoting(){
        votedPlayer = null;
    }

    /**
     * Game over boolean.
     *
     * @return if game ended true, otherwise false
     */
    public boolean gameOver(){
        return mafiaNumber() == 0 || mafiaNumber() >= citizenNumber();
    }

    /**
     * count current number of mafias in the game
     *
     * @return the number of alive mafias
     */
    public int mafiaNumber(){
        int count = 0;
        for (PlayerHandler player: players){
            if (player.getRole() instanceof Mafia && player.isAlive())
                count++;
        }
        return count;
    }

    /**
     *  count current number of Citizens in the game
     *
     * @return the number of alive Citizens
     */
    public int citizenNumber(){
        int count = 0;
        for (PlayerHandler player: players){
            if (player.getRole() instanceof Citizen && player.isAlive())
                count++;
        }
        return count;
    }

    /**
     * Day phase of game
     */
    public void day(){
        ExecutorService pool = Executors.newCachedThreadPool();
        for (PlayerHandler player: players){
            player.setTask(Task.DAY);
            pool.execute(player);
        }
        pool.shutdown();
        try {
            if (!pool.awaitTermination(5, TimeUnit.MINUTES))
                pool.shutdownNow();
            sendMessageToAll(null, "Time over and day ended.", false);

        }
        catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    /**
     * "Getting ready" phase.
     */
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

    /**
     * Create roles of game and set to players.
     */
    public void createRoles(){
        int mafiaNumber = numberPlayers / 3;

        ArrayList<Role> roles = new ArrayList<>();

        roles.add(new GodFather());
        if (numberPlayers > 6)
            roles.add(new DrLecter());
        while (roles.size() < mafiaNumber)
            roles.add(new OrdinaryMafia());

        roles.add(new Detective());
        roles.add(new Doctor());
        roles.add(new DieHard());

        if (numberPlayers > 6) {
            roles.add(new Sniper());
            roles.add(new Mayor());
        }

        if (numberPlayers > 8)
            roles.add(new Psychologist());

        while (roles.size() < numberPlayers)
            roles.add(new OrdinaryCitizen());

        Collections.shuffle(roles);

        for (PlayerHandler player: players){
            player.setRole(roles.get(0));
            roles.remove(0);
            player.notifyRoles();
        }

        for (PlayerHandler player: players){
            player.familiarRoles();
            System.out.println(player.getUserName() + ": " + player.getRole().getClass().getSimpleName());
        }


    }


    /**
     * Send message to all players.
     *
     * @param except      the except player
     * @param message     the message to be sent
     * @param savedInFile message saved in file or not
     */
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

    /**
     * Find player by role.
     *
     * @param role the role of player
     * @return the player
     */
    public PlayerHandler findRolePlayer(Role role){

        for (PlayerHandler player: players){
            if (player.getRole().getClass().equals(role.getClass()))
                return player;

        }
        return null;
    }

    /**
     * Get mafia team list.
     *
     * @return the list of mafias
     */
    public ArrayList<PlayerHandler> getMafiaTeam(){
        ArrayList<PlayerHandler> mafiaTeam = new ArrayList<>();
        for (PlayerHandler player: players){
            if (player.getRole() instanceof Mafia)
                mafiaTeam.add(player);
        }
        return mafiaTeam;
    }

    /**
     * show all player roles at end of game.
     */
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

    /**
     * check that a username exists or not
     *
     * @param userName the user name to be checked.
     * @return the valid
     */
    public synchronized boolean isValidUserName(String userName){
        for (String users: userNames){
            if (users.equals(userName))
                return false;
        }
        return true;
    }

    /**
     * Get players in vote array list.
     *
     * @param except the except
     * @return the array list
     */
    public ArrayList<PlayerHandler> getPlayersInVote(PlayerHandler except){
        ArrayList<PlayerHandler> options = new ArrayList<>();
        for (PlayerHandler player: players){
            if (!player.equals(except) && player.isAlive())
                options.add(player);
        }
        return options;
    }

    /**
     * Add user name to the list.
     *
     * @param userName the user name to be added.
     */
    public synchronized void addUserName(String userName){
        userNames.add(userName);
    }


    /**
     * Set shooted player.
     *
     * @param target the target
     */
    public void setShootedPlayer(PlayerHandler target){
        shootedPlayer = target;
    }

    /**
     * Set dr lecter save.
     *
     * @param target the target
     */
    public void setDrLecterSaved(PlayerHandler target){
        drLecterSaved = target;
    }

    /**
     * Set sniper target.
     *
     * @param target the target
     */
    public void setSniperTarget(PlayerHandler target){
        sniperTarget = target;
    }

    /**
     * Set die hard inquiry.
     *
     * @param dieHardInquiry the die hard inquiry
     */
    public void setDieHardInquiry(boolean dieHardInquiry){
        this.dieHardInquiry = dieHardInquiry;
    }

    /**
     * Set silenced player.
     *
     * @param target the target
     */
    public void setSilencedPlayer(PlayerHandler target){
        silencedPlayer = target;
    }

    /**
     * Set saved player.
     *
     * @param target the target
     */
    public void setSavedPlayer(PlayerHandler target){
        savedPlayer = target;
    }

    /**
     * Send message to mafia team.
     *
     * @param except  the except Player
     * @param message the message to be sent
     */
    public void sendMessageToMafias(PlayerHandler except, String message){
        for (PlayerHandler player: players){
            if (player.getRole() instanceof Mafia && !player.equals(except) && player.isAlive())
                player.receiveMessage(message);
        }
    }
}
