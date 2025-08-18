package org.acme.entity.orders;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "products")
public class Product extends PanacheEntity {

    public String name;

    @JsonIgnore
    @ManyToMany(mappedBy = "products", cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    public Set<Order> orders = new HashSet<>();

}