package org.acme.messages;

import org.eclipse.microprofile.reactive.messaging.Message;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

public class MessageTemplate implements Message<String> {

    private final String payload;

    public MessageTemplate(String payload) {
        this.payload = payload;
    }

    @Override
    public String getPayload() {
        return this.payload;
    }

    @Override
    public Supplier<CompletionState<Void>> getAck() {
        return () -> {
            System.out.println("Acknowledgment for " + payload);
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override 
    public Funtion<Throwable, CompletionStage<Void>> getNack() {
        return reason ->  {
            System.out.println("Negative acknowledgment for " + payload + ", the reason is " + reason);
            return CompletableFuture.completedFuture(null);
        };
    }
};