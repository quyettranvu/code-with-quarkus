package org.acme;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.acme.blocking.BlockingEchoServer;
import org.acme.blocking.BlockingWithWorkerEchoServer;
import org.acme.nio.NonBlockingServer;
import org.acme.netty.NettyEchoServer;
import org.acme.vertx.VertxEchoServer;

@Path("/hello")
public class GreetingResource {

    private volatile Thread serverThread; // main thread

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

    @GET
    @Path("/worker-thread-server")
    @Produces(MediaType.TEXT_PLAIN)
    public String welcomeFromWorkerThread() {
        if (serverThread == null || !serverThread.isAlive()) {
            serverThread = new Thread(() -> {
                try {
                    BlockingWithWorkerEchoServer.main(new String[]{});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, "non-blocking-echo-server-using-multiple-threads");
            serverThread.setDaemon(true);
            serverThread.start();
            return "BlockingWithWorkerEchoServer started on port 9999.";
        }
        return "BlockingWithWorkerEchoServer is already running.";
    }

    @GET
    @Path("/nio-server")
    @Produces(MediaType.TEXT_PLAIN)
    public String welcomeFromNioServer() {
        if (serverThread == null || !serverThread.isAlive()) {
            serverThread = new Thread(() -> {
                try {
                    NonBlockingServer.main(new String[]{});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, "non-blocking-server-using-nio");
            serverThread.setDaemon(true);
            serverThread.start();
            return "NonBlockingServerWithNio started on port 9999.";
        }
        return "NonBlockingServerWithNio is already running.";
    }

    @GET
    @Path("/netty-server")
    @Produces(MediaType.TEXT_PLAIN)
    public String welcomeFromNettyServer() {
        if (serverThread == null || !serverThread.isAlive()) {
            serverThread = new Thread(() -> {
                try {
                    NettyEchoServer.main(new String[]{});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, "non-blocking-server-using-netty");
            serverThread.setDaemon(true);
            serverThread.start();
            return "NonBlockingServerWithNetty started on port 9999.";
        }
        return "NonBlockingServerWithNetty is already running.";
    }

    @GET
    @Path("/vertx-server")
    @Produces(MediaType.TEXT_PLAIN)
    public String welcomeFromVertxServer() {
        if (serverThread == null || !serverThread.isAlive()) {
            serverThread = new Thread(() -> {
                try {
                    VertxEchoServer.main(new String[]{});
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, "non-blocking-server-using-vertx");
            serverThread.setDaemon(true);
            serverThread.start();
            return "NonBlockingServerWithVertx started on port 9999.";
        }
        return "NonBlockingServerWithVertx is already running.";
    }
}
