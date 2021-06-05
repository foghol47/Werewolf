package org.stu.server;

public class ServerMain {
    public static void main(String[] args) {
        Server gameServer = new Server(8080);
        gameServer.startServer();
    }
}
