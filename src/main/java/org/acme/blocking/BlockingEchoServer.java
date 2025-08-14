package org.acme.blocking;

import org.acme.constants.ServerResultStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class BlockingEchoServer {
    
    public static void main(String[] args) throws IOException {
        int port = 9999;

        try (ServerSocket server = new ServerSocket(port)) {
            while(true) {
                Socket client = server.accept();

                PrintWriter response = new PrintWriter(client.getOutputStream(), true);
                BufferedReader request = new BufferedReader(new InputStreamReader(client.getInputStream()));

                String line;
                while ((line = request.readLine()) != null) {
                    System.out.println("Server received message from client: " + line);
                    response.println(line);

                    if (ServerResultStatus.DONE.value().equalsIgnoreCase(line)) {
                        break;
                    }
                }
                client.close();
            }
        }
    }
}