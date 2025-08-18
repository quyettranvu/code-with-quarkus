package org.acme;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.acme.orders.Product;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Random;

@ApplicationScoped
public class ProductService {
    
    private final OrderService orderService;

    @Inject
    public ProductService(OrderService orderService) {
        this.orderService = orderService;
    }

    public Uni<Void> createProduct(String name) {
        Product product = new Product();
        product.name = name;
        return Panache.withTransaction(product::persists).replaceWithVoid();
    }

    public Uni<Product> getRecommendedProduct() {
        Random random = new Random();
        return Product.count()
                    .onItem().transform(item -> random.nextInt(Math.toIntExact(item)))
                    .onItem().transformToUni(itemIndex -> Product.findAll().page(itemIndex, 1).firstResult());
    }

    public Multi<Product> getAllOrderedProducts() {
        return orderService.getAllOrders()
                        .onItem().transformToIterable(order -> order.products)
                        .select().distinct();
    }

    public Uni<List<Product>> getAllOrderedProductsAsList() {
        return getAllOrderedProducts()
                .collect().asList();
    }

    public Uni<Product> getProductByName(String name) {
        return Product.find("name", name).firstResult();
    }
}