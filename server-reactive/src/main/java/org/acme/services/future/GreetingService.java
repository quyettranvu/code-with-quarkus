package org.acme.services.future;

import io.smallrye.mutiny.tuples.Tuple2;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Service
public class GreetingService {

     public static void main(String[] args) { 
        GreetingHelperService service = new GreetingHelperService();
        // return the completation state object without detailed processing
        CompletationState<String> future = service.greeting("Luke");
        
        // in case of diving into process, consume and transform
        service.greeting("Luke")
            .thenApply(response -> response.toUpperCase())
            .thenAccept(greeting -> System.out.println(greeting));

        service.greeting("Luke")
            .thenCompose(greetingForLuke -> {
                return service.greeting("Leia")
                                .thenApply(greetingForLeia -> Tuple2.of(greetingForLuke, greetingForLeia));
            })
            .thenAccept(tuple ->
                System.out.println(tuple.getItem1() + " " + tuple.getItem2())
            );

        // in case of encapsulating both success and failure
        CompletableFuture<String> luke = service.greeting("Luke").toCompletableFuture();
        CompletableFuture<String> leia = service.greeting("Leia").toCompletableFuture();

        CompletableFuture.allOf(luke, leia)
              .thenAccept(ignored -> {
                    System.out.println(luke.join() + " " + leia.join());
                });

        service.greeting("Luke").exceptionally(exception -> "Erorr! Luke");
        service.greeting("Leia").exceptionally(exception -> "Error! Leia");
     }
    
    static class GreetingHelperService {
        CompletionStage<String> greeting(String name) {
            return completedFuture.completedFuture("Hello " + name);
        }
    }
}