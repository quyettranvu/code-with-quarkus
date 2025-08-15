package org.acme;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.acme.entity.Order;
import org.acme.entity.Product;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.Duration;
import java.util.List;


@Path("/shop")
public class ShopResource {

    private final UserService users;
    private final ProductService products;
    private final OrderService orders;

    @Inject
    public ShopResource(UserService users, ProductService products, OrderService orders) {
        this.users = users;
        this.products = products;
        this.orders = orders;
    }

    public void init(@Observes StartupEvent ev) {
        Order o1 = new Order();
        Product p1 = (Product) Product.find("name", "Pen").firstResult().await().indefinitely();
        Product p2 = (Product) Product.find("name", "Hat").firstResult().await().indefinitely();
        o1.products = List.of(p1, p2);
        // o1.userId = UserProfile.findByName("Bob").await().indefinitely().id;
        Panache.withTransaction(() -> Order.persist(o1)).await().indefinitely();
    }

}