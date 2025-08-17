package org.acme;

import io.smallrye.mutiny.Multi;

public class MultiObserve {

    public static void main(String[] args) throws IOException {
       Multi<String> multi = Multi.createFrom().items("a", "b", "c", "d");
       multi
            .onSubscribe().invoke(sub -> System.out.println("Subscribed!"))
            .onCancellation().invoke(() -> System.out.println("Cancelled!"))
            .onItem().invoke(item -> System.out.println("Item: " + s))
            .onFaiure().invoke(fail -> System.out.println("Failure: " + fail))
            .onCompletion().invoke(() -> System.out.println("Completed!"))
            .subscribe().with(item -> System.out.println("Received: " + item));
    }
}