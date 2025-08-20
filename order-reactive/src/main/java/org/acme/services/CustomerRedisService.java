package org.acme.services;

import io.quarkus.redis.client.reactive.ReactiveRedisClient;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.acme.entity.customer.CustomerRedis;
import org.acme.utils.exception.NotFoundException;

import java.util.Arrays;
import java.util.NoSuchElementException;

@Singleton
public class CustomerRedisService {
    private static final String CUSTOMER_HASH_PREFIX = "cust:";

    @Inject
    ReactiveRedisClient reactiveRedisClient;

    public Multi<CustomerRedis> getAllCustomers() {
        return reactiveRedisClient.keys("*")
                .onItem().transformToMulti(response -> Multi.createFrom().iterable(response).map(Response::toString))
                .onItem().transformToUniAndMerge(key -> reactiveRedisClient.hgetall(key).map(response -> constructCustomer(
                        Long.parseLong(key.substring(CUSTOMER_HASH_PREFIX.length())), response
                )));
    }

    public Uni<CustomerRedis> getCustomer(Long id) {
        return reactiveRedisClient.hgetall(CUSTOMER_HASH_PREFIX + id)
                .map(response -> response.size() > 0
                        ? constructCustomer(id, response)
                        : null
                );
    }

    public Uni<CustomerRedis> createCustomer(CustomerRedis customer) {
        return storeCustomer(customer);
    }

    public Uni<CustomerRedis> updateCustomer(CustomerRedis customer) {
        return getCustomer(customer.id)
                .onItem().ifNull().failWith(new NotFoundException())
                .onItem().transformToUni(existing -> {
                    existing.name = customer.name;
                    return storeCustomer(existing).replaceWith(existing);
                });
    }

    private Uni<CustomerRedis> storeCustomer(CustomerRedis customer) {
        // hmset: key, field, value
        return reactiveRedisClient.hmset(Arrays.asList(CUSTOMER_HASH_PREFIX + customer.id, "name", customer.name))
                .onItem().transform(response -> {
                    if (response.toString().equals("OK")) {
                        return customer;
                    }
                    else {
                        throw new NoSuchElementException();
                    }
                });
    }

    public Uni<Void> deleteCustomer(Long id) {
        return reactiveRedisClient.hdel(Arrays.asList(CUSTOMER_HASH_PREFIX + id, "name"))
                .map(response -> response.toInteger() == 1 ? true : null)
                .onItem().ifNull().failWith(new NotFoundException())
                .onItem().ifNotNull().transformToUni(response -> Uni.createFrom().nullItem()); // success, transform to null item
    }

    CustomerRedis constructCustomer(long id, Response response) {
        CustomerRedis customer = new CustomerRedis();
        customer.id = id;
        customer.name = response.get("name").toString();
        return customer;
    }
}