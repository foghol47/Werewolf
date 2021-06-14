package org.stu.server;

import org.stu.elements.Mafia;
import org.stu.elements.Mayor;
import org.stu.elements.Doctor;
import org.stu.elements.Role;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;

/**
 * this class controls the players in the game.
 *
 * @author Alireza Jabbari
 */
public class PlayerHandler implements Runnable{
    private Server server;
    private Socket socket;
    private BufferedReader in;
    private PrintStream out;
    private String userName;
    private Role role;
    private boolean isAlive;
    private Task task;

    /**
     * Instantiates a new Player handler.
     *
     * @param server the server
     * @param socket the socket
     */
    public PlayerHandler(Server server, Socket socket){
        this.server = server;
        this.socket = socket;
        isAlive = true;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintStream(socket.getOutputStream());
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Get socket of client.
     *
     * @return the socket of client
     */
    public Socket getSocket(){
        return socket;
    }

    /**
     * Get username of player.
     *
     * @return username of player
     */
    public String getUserName(){
        return userName;
    }

    /**
     * Set task of player.
     *
     * @param task the task to be set
     */
    public void setTask(Task task){
        this.task = task;
    }

    /**
     * Set role of player.
     *
     * @param role the role to be set
     */
    public void setRole(Role role){
        this.role = role;
    }

    /**
     * Is alive boolean.
     *
     * @return true if player is alive, otherwise false
     */
    public boolean isAlive(){
        return isAlive;
    }

    /**
     * Kill a player.
     */
    public void kill(){
        isAlive = false;
        out.println("are you want watch game or quit the game?");
        out.println("1) watch");
        out.println("2) quit");
        try {
            String input = getResponse().trim();
            int choice = Integer.parseInt(input);
            if (choice == 2){
                out.println("!exit");
                socket.close();
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
        catch (NumberFormatException e){
            e.printStackTrace();
        }
    }

    /**
     * Get role of player.
     *
     * @return the role of player
     */
    public Role getRole(){
        return role;
    }


    @Override
    public void run() {
        if (socket.isClosed())
            return;

        switch (task){
            case INIT_USERNAME:
                initUserName();
                break;
            case GETTING_READY:
                gettingReady();
                break;
            case DAY:
                day();
                break;
            case VOTING:
                voting();
                break;
            case MAYOR_ACT:
                mayorAct();
                break;
        }





    }

//    public synchronized void setStateWaiting(){
//        if (!server.allPlayersFinished(this)){
//            try {
//                Thread.currentThread().wait();
//            }
//            catch (InterruptedException e){
//                e.printStackTrace();
//            }
//        }
//    }

    /**
     * voting phase of game
     *
     * players vote a person to kill and remove it from game
     */
    public void voting(){
        out.println("its time to vote.");
        if (!isAlive)
            return;
        ArrayList<PlayerHandler> playersInVote = server.getPlayersInVote(this);
        int i = 1;
        for (PlayerHandler player: playersInVote){
            out.println(i + ") " + player.getUserName() );
            i++;
        }
        out.println("enter number of player or enter 0 to vote no one:");
        int choice = -1;
        do {
            try {
                String input = getResponse().trim();
                if (!task.equals(Task.VOTING))
                    return;
                choice = Integer.parseInt(input);

                if (choice < 0 || choice > playersInVote.size())
                    out.println("wrong input. try again");
                else if (choice != 0)
                    server.vote(playersInVote.get(choice - 1));
            }
            catch (NumberFormatException e){
                out.println("wrong input. try again");
            }
        }while (choice < 0 || choice > playersInVote.size());


    }

    /**
     * day phase of game
     *
     * in day, players talk to each other to find mafia
     * it takes 5 minute time or it terminates if all players enter "ready"
     */
    public void day(){
        out.println("its day. talk to each other.");
        if (!isAlive)
            return;
        while (true){

            String message = userName + ": ";
            String input = getResponse();
            if (!task.equals(Task.DAY) || input.equals("ready"))
                break;
            message += input;
            server.sendMessageToAll(this, message, true);

        }
    }

    /**
     * Notify role of player.
     */
    public void notifyRoles(){
        out.println("your role is " + role.getClass().getSimpleName() );

    }

    /**
     * this method sends mafia roles for mafia team and sends doctor role for mayor
     */
    public void familiarRoles(){
        if (role instanceof Mayor){
            String doctorUserName = server.findRolePlayer(new Doctor()).getUserName();
            out.println("Doctor is " + doctorUserName);
        }
        else if (role instanceof Mafia){
            out.println("mafia members:");
            ArrayList<PlayerHandler> mafiaTeam = server.getMafiaTeam();
            for (PlayerHandler mafia: mafiaTeam){
                out.println(mafia.getClass().getSimpleName() + ": " + mafia.getUserName());
            }
        }
    }

    /**
     * Receive a message and print for client.
     *
     * @param message the message to be received.
     */
    public void receiveMessage(String message){
        out.println(message);
    }

    /**
     * Send message to other clients.
     *
     * @param message the message to be sent.
     */
    public void sendMessage(String message){
        server.sendMessageToAll(this, message, true);
    }

    /**
     * Initial username of player.
     * usernames cannot be repeated.
     */
    public void initUserName(){

        String userNameTemp = "";
        while (true){
            out.println("enter your username:");

            userNameTemp = getResponse().trim();

            if (server.isValidUserName(userNameTemp))
                break;
            else
                out.println("this username exists.");
        }
        userName = userNameTemp;
        server.addUserName(userName);

    }

    /**
     * before start of game, players should send "start" to start the game
     */
    public void gettingReady(){
        out.println("if you ready enter \"start\"");
        String input = "";
        do {

            input = getResponse().toLowerCase();
            if (!task.equals(Task.GETTING_READY))
                break;
            if (!input.equals("start"))
                out.println("wrong input. try again");

        }while (!input.equals("start"));
        server.incrementReadyPlayers();
    }

    /**
     * this method call mayor for its act
     *
     * mayor can cancel voting.
     */
    public void mayorAct(){
        if (role instanceof Mayor) {
            out.println("are you want cancel voting? if yes enter 1 and if no enter another number");
            try {
                String input = getResponse();
                int choice = Integer.parseInt(input);
                if (choice == 1)
                    server.cancelVoting();
            }
            catch (NumberFormatException e){
                e.printStackTrace();
            }
        }
        else
            out.println("wait for mayor...");

    }

    /**
     * show day chat history from file
     */
    public void showHistory(){
        server.showHistory(out);
    }

    /**
     * read response of player.
     *
     * @return the input of client
     */
    public String getResponse(){
        String input = "";
        try {
            do {
                input = in.readLine();
                if (input.equals("exit")){
                    out.println("!exit");
                    isAlive = false;
                    socket.close();
                }
                else if (input.equals("history")){
                    showHistory();
                }

            }while (!input.equals("exit") && !input.equals("history"));

        }
        catch (IOException e){
            System.out.println(userName + " disconnected.");
        }
        return input;
    }
}

