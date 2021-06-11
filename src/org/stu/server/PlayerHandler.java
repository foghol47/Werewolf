package org.stu.server;

import org.stu.elements.Mayor;
import org.stu.elements.Doctor;
import org.stu.elements.Role;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class PlayerHandler implements Runnable{
    private Server server;
    private Socket socket;
    private BufferedReader in;
    private PrintStream out;
    private String userName;
    private Role role;
    private boolean isAlive;
    private boolean isReady;
    private Task task;

    public PlayerHandler(Server server, Socket socket){
        this.server = server;
        this.socket = socket;
        isAlive = true;
        isReady = false;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintStream(socket.getOutputStream());
        }
        catch (IOException e){
            e.printStackTrace();
        }
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

    public Role getRole(){
        return role;
    }


    @Override
    public void run() {
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

    public void day(){
        out.println("its day. talk to each other.");
        while (true){
            try {
                String message = userName + ": ";
                message += in.readLine();
                if (!task.equals(Task.DAY))
                    break;
                server.sendMessageToAll(this, message, true);
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public void getRoles(){
        out.println("your role is " + role.getClass().getSimpleName() );
        if (role instanceof Mayor){
            String doctorUserName = server.findRoleUserName(new Doctor());
            out.println("Doctor is " + doctorUserName);
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
            try {
                userNameTemp = in.readLine().trim();
            }
            catch (IOException e){
                e.printStackTrace();
            }
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
            try {
                input = in.readLine().toLowerCase();
                if (!task.equals(Task.GETTING_READY))
                    break;
                if (!input.equals("start"))
                    out.println("wrong input. try again");
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }while (!input.equals("start"));
        isReady = true;
        server.incrementReadyPlayers();
    }
}

