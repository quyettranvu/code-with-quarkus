package org.acme;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.acme.orders.Order;
import org.acme.orders.Product;
import org.acme.users.UserProfile;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

@ApplicationScoped
public class OrderService {

    @Inject UserService users;

    public Multi<Order> getOrderForUser(UserProfile userProfile) {
        return Order.stream("userId", userProfile.id);
    }

}