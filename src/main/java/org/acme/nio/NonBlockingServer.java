package org.acme.nio;

import org.acme.constants.ServerResultStatus;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

public class NonBlockingServer {
    
    public static void main(String[] args) throws IOException {
        InetSocketAddress address = new InetSocketAddress("localhost", 9999);
        Selector selector = Selector.open(); // cornerstone of non blocking I/O -> as an observer, monitoring the management of registered channels, can handle multiple requests via 1 thread
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.configureBlocking(false);

        channel.socket().bind(address);
        channel.register(selector, SelectionKey.OP_ACCEPT);

        while(true) {
            int readyChannels = selector.select(); // ready channels waiting for events
            if (readyChannels == 0) {
                continue;
            }

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
            while(keyIterator.hasNext()) {
                SelectionKey selectedKey = keyIterator.next();
                if (selectedKey.isAcceptable()) {
                    // Accept new connection
                    ServerSocketChannel serverChannel = (ServerSocketChannel) selectedKey.channel();
                    SocketChannel clientChannel = serverChannel.accept();
                    clientChannel.configureBlocking(false);
                    clientChannel.register(selector, SelectionKey.OP_READ);
                    System.out.println("Client connection accepted and registered with selector for reading: " + clientChannel.getLocalAddress())
                } 
                else if (selectedKey.isReadable()) {
                    SocketChannel clientReadChannel = selectedKey.channel();
                    ByteBuffer payloadBuffer = ByteBuffer.allocate(256);
                    int size =clientReadChannel.read(payloadBuffer);
                    if (size != -1) { // handle the disconnection if no more data can be read
                        System.out.println("Disconnection from " + clientReadChannel.getRemoteAddress());
                        clientReadChannel.close();
                        selectedKey.cancel();
                    } else {
                        String result = new String(payload.array(), StandardCharsets.UTF_8).trim();
                        System.out.println("Received message: " + result);
                        if (result.equals(ServerResultStatus.DONE.value())) {
                            clientReadChannel.close();
                        }

                        // prepare the buffer for writing and echo the received message back to the client
                        payloadBuffer.rewind();
                        clientReadChannel.write(buffer);
                    }
                }
                keyIterator.remove(); // be sure not making twice
            }
        }
    }
}