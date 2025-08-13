package org.acme.blocking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class BlockingWithWorkerEchoServer { 
    int port = 9999;
    ExecutorService executors = new Executors.newFixedThreadPool(10);

    try (ServerSocket server = new ServerSocket(port)) {
        while(true) {
            Socket client = server.accept();

            executors.submit(() -> {
                try {
                    PrintWriter response = new PrintWriter(client.getOutputStream(), true);
                    BufferedReader request = new BufferedReader(new InputStreamReader(client.getInputStream()));

                    String line;
                    while ((line = request.readLine()) != null) {
                        System.out.println(Thread.currentThread().getName() + " - Server received message from client: " + line);
                        response.println(line);

                        if ("done".equalsIgnoreCase(line)) {
                            break;
                        }
                    }

                    client.close();
                } catch (Exception e) {
                    System.err.println("Couldn't serve I/O: " + e.toString());
                }
            })
        }
    }
}