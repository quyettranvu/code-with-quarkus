package org.acme.gateway;

import io.smallrye.mutiny.Uni;
import org.acme.http.model.Order;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrderProcessing {

    @RestClient
    ValidationService validationService;

    @Incoming("new-orders")
    @Outgoing("validate-orders")
    // @Timeout(2000)
    public Uni<Void> validate(Order order) {
        return validationService.validate(order)
                .onItem().transform(v -> order);
    }
}
