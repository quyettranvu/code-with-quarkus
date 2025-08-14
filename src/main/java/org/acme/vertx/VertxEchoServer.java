package org.acme.vertx;

import io.vertx.core.Vertx;

public class VertxEchoServer {

    public static void main(String[] args) throws Exception {
        Vertx vertx = new Vertx();
        // Create a TCP Server on Vert.x event loop single thread
        vertx.creatNetServer()
                .connectHandler(socket -> {
                    // Just write the content back
                    socket.handler(buffer -> socket.write(buffer));
                })
                .listen(9999);
    }
}