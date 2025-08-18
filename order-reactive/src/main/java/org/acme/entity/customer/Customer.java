package org.acme.entity.customer;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import org.acme.entity.orders.Order;
import org.hibernate.validator.constraints.Length;

import java.util.ArrayList;
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