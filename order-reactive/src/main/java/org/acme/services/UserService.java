package org.acme.services;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.acme.orders.Product;
import org.acme.users.UserProfile;

import javax.enterprise.context.ApplicationScoped;
import java.util.Random;

@ApplicationScoped
public class UserService {

    public Long createUser(String name) {
        UserProfile user = new UserProfile();
        user.name = name;
        Panache.withTransaction(()-> user.persist().onItem().transform(u -> ((UserProfile) u).id));
    }

    public Uni<UserProfile> getUserByName(String name) {
        return UserProfile.findByName(name);
    }

    public Multi<UserProfile> getAllUsers() {
        return UserProfile.streamAll();
    }

    public Uni<UserProfile> getRandomUser() {
        Random random = new Random();
        return UserProfile.count()
                    .onItem().transform(item -> random.nextInt(Math.toIntExact(item)))
                    .onItem().transformToUni(itemIndex -> Product.findAll().page(itemIndex, 1).firstResult());
    }
}