package org.acme.gateway;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import org.acme.http.model.Order;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrderStorage {

    @Incoming("validate-orders")
    public Uni<Void> store(Order order) {
        return Panache.withTransaction(order::persist).replaceWithVoid();
    }
}