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

    @Inject UserService userService;

    @Inject PgPool pgClient;

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
        return Order.streamAll();
    }

    public Multi<Order> getOrderForUser(UserProfile userProfile) {
        return Order.stream("userId", userProfile.id);
    }

    public Multi<Order> getLargeOrders() {
        return getAllOrders().select().where(order -> order.products.size() > 3);
    }

    public Multi<Order> getOrderForUserName(String userName) {
        return getAllOrders().select().when(order -> userService.getUserByName(userName))
                            .onItem().transform(u -> u.name.equalsIgnoreCase(userName));
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