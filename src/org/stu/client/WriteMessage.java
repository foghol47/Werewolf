package org.stu.client;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

/**
 * this class used to write message and send it to the server.
 *
 * @author Alireza Jabbari
 */
public class WriteMessage implements Runnable{
    private Socket socket;
    private PrintStream out;
    private Scanner scanner;

    /**
     * Instantiates a new Write message.
     *
     * @param socket the socket
     */
    public WriteMessage(Socket socket){
        this.socket = socket;
        try {
            out = new PrintStream(socket.getOutputStream());
        }
        catch (IOException e){
            e.printStackTrace();
        }
        scanner = new Scanner(System.in);
    }

    @Override
    public void run() {

        String string = "";
        while (true) {
            string = scanner.nextLine();
            out.println(string);
        }

    }
}
