package org.acme.entity.orders;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.vertx.mutiny.sqlclient.Row;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "order_custom")
public class OrderCustom extends PanacheEntity {
  @Column(nullable = false)
  public Long customerId;

  public String description;

  public BigDecimal total;

  public static OrderCustom from(Row row) {
    OrderCustom order = new OrderCustom();
    order.id = row.getLong("id");
    order.customerId = row.getLong("customerid");
    order.description = row.getString("description");
    order.total = row.getBigDecimal("total");
    return order;
  }
}