package org.acme.messages;

import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import java.time.Duration;

@ApplicationScoped
public class MessageExample {

    @Outgoing("ticks")
    public Multi<MessageTemplate<String>> ticks() {
        return Multi.createFrom().ticks().every(Duration.ofSeconds(1))
                    .onOverflow().drop()
                    .onItem().transform(l -> new MessageTemplate<>(String.valueOf(l)));
    }

    @Incoming("ticks")
    @Outgoing("resolve")
    public Message<Object> resolve(MessageTemplate<String> ticks) {
        return ticks.withPayload((Object)ticks.getPayload());
    }

    @Incoming("resolve")
    public void printMsg(String msg) {
        if (msg.contains("3")) {
            throw new IllegalArgumentException("boom"); // nack for the message broker
        }
        System.out.println(msg);
    }
}