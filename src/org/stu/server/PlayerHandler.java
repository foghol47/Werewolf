package org.stu.server;

import org.stu.elements.*;

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
    private boolean isSilenced = false;

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

    public void setSilenced(boolean isSilenced){
        this.isSilenced = isSilenced;
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
            String input = getResponse();
            if (input == null)
                return;
            int choice = Integer.parseInt(input.trim());
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
                isSilenced = false;
                break;
            case VOTING:
                voting();
                break;
            case MAYOR_ACT:
                mayorAct();
                break;
            case NIGHT:
                night();
                break;
            case KILLED:
                kill();
                break;
        }





    }

    /**
     * voting phase of game
     *
     * players vote a person to kill and remove it from game
     */
    public void voting(){
        out.println("** its time to VOTE. **");
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
                String input = getResponse();
                if (!task.equals(Task.VOTING) || input == null)
                    return;
                choice = Integer.parseInt(input.trim());

                if (choice < 0 || choice > playersInVote.size())
                    out.println("wrong input. try again");
                else if (choice != 0) {
                    PlayerHandler selectedPlayer = playersInVote.get(choice - 1);
                    server.vote(selectedPlayer);
                    server.sendMessageToAll(this, userName + " voted to " + selectedPlayer.getUserName(), false);
                }
            }
            catch (NumberFormatException e){
                out.println("wrong input. try again");
            }
        }while (choice < 0 || choice > playersInVote.size());


    }

    public void night(){
        if (!isAlive)
            return;

        out.println("** its NIGHT... **");
        if (role instanceof Detective){
            detectiveAct();
        }
        else if (role instanceof Mafia){
            mafiaAct();
        }
        else if (role instanceof Sniper){
            sniperAct();
        }
        else if (role instanceof DieHard){
            dieHardAct();
        }
        else if (role instanceof Psychologist){
            psychologistAct();
        }
        else if (role instanceof Doctor){
            doctorAct();
        }
    }

    /**
     * day phase of game
     *
     * in day, players talk to each other to find mafia
     * it takes 5 minute time or it terminates if all players enter "ready"
     */
    public void day(){
        out.println("** its DAY. **\ntalk to each other.");
        if (!isAlive || isSilenced)
            return;
        while (true){

            String message = userName + ": ";
            String input = getResponse();
            if (!task.equals(Task.DAY) || input == null || input.equals("ready"))
                return;
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
            PlayerHandler doctor = server.findRolePlayer(new Doctor());
            if (doctor == null){
                out.println("we have no doctor in game.");
            }
            String doctorUserName = doctor.getUserName();
            out.println("Doctor is " + doctorUserName);
        }
        else if (role instanceof Mafia){
            out.println("mafia members:");
            ArrayList<PlayerHandler> mafiaTeam = server.getMafiaTeam();
            for (PlayerHandler mafia: mafiaTeam){
                out.println(mafia.getRole().getClass().getSimpleName() + ": " + mafia.getUserName());
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

            input = getResponse();
            if (!task.equals(Task.GETTING_READY) || input == null)
                return;
            if (!input.toLowerCase().equals("start"))
                out.println("wrong input. try again");

        }while (!input.toLowerCase().equals("start"));

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
                if (!task.equals(Task.MAYOR_ACT) || input == null)
                    return;
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

    public void mafiaAct(){
        ArrayList<PlayerHandler> players = server.getPlayersInVote(this);
        int i = 1;
        for (PlayerHandler player: players){
            out.println(i + ") " + player.getUserName() );
            i++;
        }
        out.println("enter number of player to shoot him:");
        int choice = 0;
        do {
            String input = getResponse();
            if (!task.equals(Task.NIGHT) || input == null)
                return;
            choice = Integer.parseInt(input.trim());
            if (choice < 1 || choice > players.size()){
                out.println("wrong input. try again");
            }
        }while(choice < 1 || choice > players.size());

        PlayerHandler selectedPlayer = players.get(choice - 1);
        server.sendMessageToMafias(this, userName + " selected " + selectedPlayer.getUserName());

        if (role instanceof GodFather){
            server.setShootedPlayer(selectedPlayer);
        }
        else if (role instanceof DrLecter){
            if (!server.findRolePlayer(new GodFather()).isAlive())
                server.setShootedPlayer(selectedPlayer);
            drLecterAct();
        }
        else{
            if (!server.findRolePlayer(new GodFather()).isAlive() && !server.findRolePlayer(new DrLecter()).isAlive())
                server.setShootedPlayer(selectedPlayer);
        }

    }

    public void drLecterAct(){
        DrLecter drLecter = (DrLecter) role;

        ArrayList<PlayerHandler> mafiaTeam = server.getMafiaTeam();
        int i = 1;
        for (PlayerHandler player: mafiaTeam){
            out.println(i + ") " + player.getUserName() );
            i++;
        }
        out.println("enter number of player to save him:");
        int choice = 0;
        PlayerHandler selectedPlayer = null;
        do {
            String input = getResponse();
            if (!task.equals(Task.NIGHT) || input == null)
                return;
            choice = Integer.parseInt(input.trim());
            if (choice < 1 || choice > mafiaTeam.size()){
                out.println("wrong input. try again");
            }
            else {
                selectedPlayer = mafiaTeam.get(choice - 1);
                if (selectedPlayer.equals(this) ){
                    if (drLecter.isSavedHimself()){
                        out.println("you saved your self previous. select another person");
                        continue;
                    }
                    else
                        drLecter.setSavedHimself(true);
                }
            }
        }while(choice < 1 || choice > mafiaTeam.size());

        server.setDrLecterSaved(selectedPlayer);
    }

    public void detectiveAct(){
        ArrayList<PlayerHandler> players = server.getPlayersInVote(this);
        int i = 1;
        for (PlayerHandler player: players){
            out.println(i + ") " + player.getUserName() );
            i++;
        }
        out.println("enter number of player to detect its role:");
        int choice = 0;
        do {
            String input = getResponse();
            if (!task.equals(Task.NIGHT) || input == null)
                return;
            choice = Integer.parseInt(input.trim());
            if (choice < 1 || choice > players.size()){
                out.println("wrong input. try again");
            }
        }while(choice < 1 || choice > players.size());
        PlayerHandler selectedPlayer = players.get(choice - 1);
        if (selectedPlayer.getRole() instanceof Citizen || selectedPlayer.getRole() instanceof GodFather){
            out.println(selectedPlayer.getUserName() + " is a citizen.");
        }
        else{
            out.println(selectedPlayer.getUserName() + " is a Mafia.");
        }
    }

    public void doctorAct(){
        ArrayList<PlayerHandler> players = server.getPlayersInVote(null);
        int i = 1;
        for (PlayerHandler player: players){
            out.println(i + ") " + player.getUserName() );
            i++;
        }
        out.println("enter number of player to save him:");
        int choice = 0;
        do {
            String input = getResponse();
            if (!task.equals(Task.NIGHT) || input == null)
                return;
            choice = Integer.parseInt(input.trim());
            if (choice < 1 || choice > players.size()){
                out.println("wrong input. try again");
            }
        }while(choice < 1 || choice > players.size());
        PlayerHandler selectedPlayer = players.get(choice - 1);

        server.setSavedPlayer(selectedPlayer);
    }

    public void dieHardAct(){
        DieHard dieHard = (DieHard) role;
        if (dieHard.haveInquiry()){
            out.println("if you want inquiry enter 1 and if not enter 0.");
            String input = getResponse();
            if (!task.equals(Task.NIGHT) || input == null)
                return;
            int choice = Integer.parseInt(input.trim());

            if (choice == 1){
                server.setDieHardInquiry(true);
                dieHard.useInquiry();
            }
        }
    }

    public void psychologistAct(){
        ArrayList<PlayerHandler> players = server.getPlayersInVote(this);
        int i = 1;
        for (PlayerHandler player: players){
            out.println(i + ") " + player.getUserName() );
            i++;
        }
        out.println("enter number of player to silent him:");
        out.println("if you dont want no one silenced enter 0");
        int choice = -1;
        do {
            String input = getResponse();
            if (!task.equals(Task.NIGHT) || input == null)
                return;
            choice = Integer.parseInt(input.trim());
            if (choice < 0 || choice > players.size()){
                out.println("wrong input. try again");
            }
            else if (choice == 0)
                return;
        }while(choice < 0 || choice > players.size());
        PlayerHandler selectedPlayer = players.get(choice - 1);

        server.setSilencedPlayer(selectedPlayer);
    }

    public void sniperAct(){
        ArrayList<PlayerHandler> players = server.getPlayersInVote(this);
        int i = 1;
        for (PlayerHandler player: players){
            out.println(i + ") " + player.getUserName() );
            i++;
        }
        out.println("enter number of player to shoot him:");
        out.println("note: if you select a citizen you will killed. if you dont want shoot anyone enter 0");
        int choice = -1;
        do {
            String input = getResponse();
            if (!task.equals(Task.NIGHT) || input == null)
                return;
            choice = Integer.parseInt(input.trim());
            if (choice < 0 || choice > players.size()){
                out.println("wrong input. try again");
            }
            else if (choice == 0)
                return;
        }while(choice < 0 || choice > players.size());
        PlayerHandler selectedPlayer = players.get(choice - 1);

        server.setSniperTarget(selectedPlayer);

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
                    //return null;
                }
                else if (input.equals("history")){
                    showHistory();
                }

            }while (input.equals("exit") || input.equals("history"));

        }
        catch (IOException e){
            System.out.println(userName + " disconnected.");
            isAlive = false;
            return null;
        }
        return input;
    }
}

