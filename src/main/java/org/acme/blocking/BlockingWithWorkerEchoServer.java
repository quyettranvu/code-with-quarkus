package org.acme.blocking;

import org.acme.constants.ServerResultStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlockingWithWorkerEchoServer { 

    int port = 9999;
    ExecutorService executors = Executors.newFixedThreadPool(10);

    public void start() throws IOException {
        try (ServerSocket server = new ServerSocket(port)) {
            while (true) {
                Socket client = server.accept();

                executors.submit(() -> {
                    try {
                        PrintWriter response = new PrintWriter(client.getOutputStream(), true);
                        BufferedReader request = new BufferedReader(new InputStreamReader(client.getInputStream()));

                        String line;
                        while ((line = request.readLine()) != null) {
                            System.out.println(Thread.currentThread().getName() 
                                + " - Server received message from client: " + line);
                            response.println(line);

                            if (ServerResultStatus.DONE.value().equalsIgnoreCase(line)) {
                                break;
                            }
                        }

                        client.close();
                    } catch (Exception e) {
                        System.err.println("Couldn't serve I/O: " + e);
                    }
                });
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new BlockingWithWorkerEchoServer().start();
    }
}
