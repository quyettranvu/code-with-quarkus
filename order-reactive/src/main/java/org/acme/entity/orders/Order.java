package org.acme.entity.orders;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.vertx.mutiny.sqlclient.Row;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "orders")
public class Order extends PanacheEntity {

    public Long userId;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
    @JoinTable(
            name = "Order_Products",
            joinColumns = {@JoinColumn(name = "order_id")},
            inverseJoinColumns = {@JoinColumn(name = "product_id")}
    )
    public List<Product> products;

    public static Order from(Row row) {
        Order o = new Order();
        o.id = row.getLong("id");
//        o.customerId = row.getLong("customerid");
//        o.description = row.getString("description");
//        o.total = row.getDouble("total");
        return o;
    }
}