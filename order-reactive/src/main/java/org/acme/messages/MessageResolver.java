package org.acme.messages;

import io.smallrye.mutiny.Multi;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import javax.enterprise.context.ApplicationScoped;
import java.time.Duration;

@ApplicationScoped
public class MessageExample {

    @Outgoing("ticks")
    public Multi<MessageTemplate> ticks() {
        return Multi.createFrom().ticks().every(Duration.ofSeconds(1))
                    .onOverflow().drop()
                    .onItem().transform(MessageTemplate::new);
    }

    @Incoming("ticks")
    @Outgoing("resolve")
    public MessageTemplate<String> resolve(MessageTemplate<String> ticks) {
        return ticks.withPayload();
    }

    @Incoming("resolve")
    public void printMsg(String msg) {
        if (msg.contains("3")) {
            throw new IllegalArgumentException("boom"); // nack for the message broker
        }
        System.out.println(msg);
    }
}