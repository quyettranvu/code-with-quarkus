package org.acme.services;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.entity.orders.Product;
import org.acme.entity.users.UserProfile;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class UserService {

    public Uni<Long> createUser(String name) {
        UserProfile user = new UserProfile();
        user.name = name;
        return Panache.withTransaction(()-> user.persist().onItem().transform(u -> ((UserProfile) u).id));
    }

    public Uni<UserProfile> getUserByName(String name) {
        return UserProfile.findByName(name);
    }

    public Multi<UserProfile> getAllUsers() {
        AtomicInteger page = new AtomicInteger(0);
        int pageSize = 100;

        return Multi.createBy().repeating()
                .uni(() -> UserProfile.findAll()
                        .page(Page.of(page.getAndIncrement(), pageSize))
                        .list())
                .whilst(list -> !list.isEmpty())
                .onItem().transformToMultiAndConcatenate(list -> (java.util.concurrent.Flow.Publisher<? extends UserProfile>) Multi.createFrom().iterable(list));
    }

    public Uni<UserProfile> getRandomUser() {
        Random random = new Random();
        return UserProfile.count()
                    .onItem().transform(item -> random.nextInt(Math.toIntExact(item)))
                    .onItem().transformToUni(itemIndex -> Product.findAll().page(itemIndex, 1).firstResult());
    }
}