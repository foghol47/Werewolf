package org.stu.client;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

public class WriteMessage implements Runnable{
    private Socket socket;
    private PrintStream out;
    private Scanner scanner;

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
