package org.acme.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetServer;

public class VertxEchoServer {

    public static void main(String[] args) throws Exception {
        Vertx vertx = Vertx.vertx();

        NetServer server = vertx.createNetServer();

        server.connectHandler(socket -> {
            // Echo the incoming data back to the client
            socket.handler(socket::write);
        });

        server.listen(9999, res -> {
            if (res.succeeded()) {
                System.out.println("Echo server is now listening on port 9999");
            } else {
                System.out.println("Failed to bind!");
                res.cause().printStackTrace();
            }
        });
    }
}