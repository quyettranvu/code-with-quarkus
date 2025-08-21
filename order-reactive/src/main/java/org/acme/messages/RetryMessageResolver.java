package org.acme.messages;

import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.faulttolerance.Retry;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Random;

@ApplicationScoped
public class RetryMessageResolver {

    @Outgoing("ticks")
    public Multi<MessageTemplate> ticks() {
        return Multi.createFrom().ticks().every(Duration.ofSeconds(1))
                .onOverflow().drop()
                .onItem().transform(l -> new MessageTemplate<>(String.valueOf(l)));
    }

    @Incoming("ticks")
    @Outgoing("resolve")
    @Retry(maxRetries = 10, delay = 1, delayUnit = ChronoUnit.SECONDS)
    public MessageTemplate<String> resolve(MessageTemplate<String> ticks) {
        simulateError();
        String newPayload = "tick-" + ticks.getPayload();
        return new MessageTemplate<>(newPayload);
    }

    @Incoming("resolve")
    public void printMsg(String msg) {
        if (msg.contains("3")) {
            throw new IllegalArgumentException("boom"); // nack for the message broker
        }
        System.out.println(msg);
    }

    private final Random random = new Random();

    void simulateError() {
        if (random.nextInt(10) > 7) {
            throw new RuntimeException("boom");
        }
    }
}