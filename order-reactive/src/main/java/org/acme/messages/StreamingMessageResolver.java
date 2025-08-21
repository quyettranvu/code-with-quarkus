package org.acme.messages;

import io.smallrye.mutiny.Multi;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import javax.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.util.List;

@ApplicationScoped
public class StreamingMessageResolver {

    @Outgoing("ticks")
    public Multi<MessageTemplate> ticks() {
        return Multi.createFrom().ticks().every(Duration.ofSeconds(1))
                    .onOverflow().drop();
    }

    @Incoming("ticks")
    @Outgoing("groups")
    public Multi<List<String>> group(Multi<Long> stream) {
        return stream
                    .onItem().transform(l -> Long.toString(l))
                    .group().intoLists().of(5);
    }

    @Incoming("groups")
    @Outgoing("resolve")
    public String processGroup(List<String> list) {
        return "Hello " + String.join(",", list);
    }

    @Incoming("resolve")
    public void printMsg(String msg) {
        System.out.println(msg);
    }

    // This will be a case of blocking implementation that delegates to the worker thread for understanding, do not abuse it
    // @Incoming("groups-blocking")
    // @Outgoing("resolve-blocking")
    // public String processGroupOnWorkerThread(List<String> list) {
    //     try {
    //         Thread.sleep(1000);
    //     } catch (InterruptedException e) {
    //         Thread.currentThread().interrupt();
    //     }
    //     return Hello + String.join(", ", list);
    // }
}