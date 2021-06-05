package org.stu.client;

import java.util.Scanner;

public class ClientMain {

    public static void main(String[] args) {
        System.out.println("enter port:");
        Scanner scanner = new Scanner(System.in);
        int port = scanner.nextInt();
        scanner.nextLine();

        Client client = new Client(port);
        client.execute();

    }
}
