package org.acme;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.acme.blocking.BlockingEchoServer; 

@Path("/hello")
public class GreetingResource {

    // @GET
    // @Produces(MediaType.TEXT_PLAIN)
    // public String hello() {
    //     return "Hello RESTEasy";
    // }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String welcome() {
        if (serverThread == null || !serverThread.isAlive()) {
            serverThread = new Thread(() -> {
                try {
                    BlockingEchoServer.main(new String[]{}); // run your server
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, "blocking-echo-thread");
            serverThread.setDaemon(true); // don't block JVM shutdown
            serverThread.start();
            return "BlockingEchoServer started on port 9999.";
        }
        return "BlockingEchoServer is already running.";
    }
}
