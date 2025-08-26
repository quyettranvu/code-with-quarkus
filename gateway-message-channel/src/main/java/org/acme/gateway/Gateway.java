package org.acme.gateway;

import io.smallrye.mutiny.Uni;
import org.acme.model.Order;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.eclipse.microprofile.reactive.messaging.Channel;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/")
public class Gateway {

    @RestClient
    GreetingService greetingService;
    @RestClient
    QuoteService quoteService;

    @GET
    @Path("/quote")
    public Uni<String> getQuote() {
        return quoteService.getQuote();
    }

    @GET
    @Path("/greeting-quote")
    public Uni<Greeting> getBoth(@QueryParam("name") @DefaultValue("anonymous") String name) {
        Uni<String> greeting = greetingService.greeting(name);
        Uni<String> quote = quoteService.getQuote()
                .onFailure().recoverWithItem("No coffee - no quote");

        return Uni.combine().all().unis(greeting, quote).asTuple()
                .onItem().transform(tuple -> new Greeting(tuple.getItem1(), tuple.getItem2()));
    }

    public static class Greeting {
        public final String greeting;
        public final String quote;

        public Greeting(String greeting, String quote) {
            this.greeting = greeting;
            this.quote = quote;
        }
    }

    @Channel("new-orders")
    MultinyEmitter<Order> emitter;

    @POST
    @Path("/create-order")
    public Uni<Response> order(Order order) {
        return emitter.send(order)
                .onItem().transform(x -> Response.accepted().build())
                .onFailure().recoverWithItem(Response.status(Response.Status.BAD_REQUEST).build());
    }

    @GET
    @Path("/orders")
    public Multi<Order> getAllValidatedOrders() {
        return Order.streamAll();
    }
}
