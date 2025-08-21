package org.acme.services;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.entity.orders.Product;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class ProductService {
    
    private final OrderService orderService;

    @Inject
    public ProductService(OrderService orderService) {
        this.orderService = orderService;
    }

    public Uni<Void> createProduct(String name) {
        Product product = new Product();
        product.setName(name);
        return Panache.withTransaction(product::persist).replaceWithVoid();
    }

    public Multi<Product> getAllProducts() {
        int pageSize = 500;
        AtomicInteger pageIndex = new AtomicInteger();

        return Multi.createBy().repeating().uni(
                        () -> pageIndex,
                        index -> Product.findAll()
                                .page(Page.of(index.getAndIncrement(), pageSize))
                                .list()
                )
                .whilst(list -> !list.isEmpty())     // stop when a page is empty
                .onItem().disjoint();                // flatten List<Product> into Product items
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