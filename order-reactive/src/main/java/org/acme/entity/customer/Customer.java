package org.acme.entity.customer;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "customer")
public class Customer extends PanacheEntity {

    @Column(nullable = false)
    @NotBlank(message = "Customer name can not be blank")
    @Length(min = 3, message = "Customer names must be at least three characters")
    public String name;

    @Transient
    public List<Order> orders = new ArrayList<>();
}