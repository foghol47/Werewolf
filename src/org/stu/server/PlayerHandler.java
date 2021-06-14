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

public class PlayerHandler implements Runnable{
    private Server server;
    private Socket socket;
    private BufferedReader in;
    private PrintStream out;
    private String userName;
    private Role role;
    private boolean isAlive;
    private Task task;

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

    public Socket getSocket(){
        return socket;
    }

    public String getUserName(){
        return userName;
    }

    public void setTask(Task task){
        this.task = task;
    }

    public void setRole(Role role){
        this.role = role;
    }

    public boolean isAlive(){
        return isAlive;
    }

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

    public void notifyRoles(){
        out.println("your role is " + role.getClass().getSimpleName() );

    }
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

    public void receiveMessage(String message){
        out.println(message);
    }

    public void sendMessage(String message){
        server.sendMessageToAll(this, message, true);
    }

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

    public void showHistory(){
        server.showHistory(out);
    }

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

