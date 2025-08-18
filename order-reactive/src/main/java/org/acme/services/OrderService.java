package org.acme.services;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.entity.orders.Order;
import org.acme.entity.orders.Product;
import org.acme.entity.users.UserProfile;

import java.util.List;

@ApplicationScoped
public class OrderService {

    @Inject
    UserService userService;

    @Inject
    PgPool pgClient;

    public Uni<Long> placeOrder(UserProfile user, List<Product> products) {
        Order order = new Order();
        order.userId = user.id;
        order.products = products;
        return Panache.withTransaction(()-> {
            Uni<Order> uni = order.persist();
            return uni.onItem().transform(o -> o.id);
        });
    }

    public Multi<Order> getAllOrders() {
        return Order.findAll()
                .list()
                .onItem().transformToMulti(list -> Multi.createFrom().iterable(
                        list.stream().map(e -> (Order) e).toList()
                ));
    }

    public Multi<Order> getOrderForUser(UserProfile userProfile) {
        return Order.find("userId", userProfile.id)
                .list()
                .onItem().transformToMulti(list -> Multi.createFrom().iterable(
                        list.stream().map(e -> (Order) e).toList()
                ));
    }

    public Multi<Order> getLargeOrders() {
        return getAllOrders().select().where(order -> order.products.size() > 3);
    }

    public Multi<Order> getOrderForUserName(String userName) {
        return userService.getUserByName(userName)   // Uni<UserProfile>
                .onItem().transformToMulti(user -> {
                    if (user == null) {
                        return Multi.createFrom().empty();
                    }
                    return Order.find("userId", user.id).list().onItem().transformToMulti(list -> Multi.createFrom().iterable(
                            list.stream()
                            .map(entity -> (Order) entity)
                            .toList()));
                });
    }

    public Uni<List<Order>> getOrdersForCustomer(Long customer) {
        return pgClient
            .preparedQuery("SELECT id, customerid, description, total FROM orders WHERE customerid = $1")
            .execute(Tuple.of(customer))
            .onItem().transformToMulti(set -> Multi.createFrom().iterable(set))
            .onItem().transform(Order::from)
            .collect().asList();
    }
}