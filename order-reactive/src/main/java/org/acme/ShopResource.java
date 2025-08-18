package org.acme;

import org.acme.entity.users.UserProfile;
import org.acme.services.UserService;
import org.acme.sevices.ProductService;
import org.acme.service.OrderService;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.panache.common.Sort;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.acme.entity.orders.Order;
import org.acme.entity.orders.Product;
import org.acme.dto.ProductModel;
import org.acme.utils.StringUtil;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.Duration;
import java.util.List;
import javax.validation.Valid;
import javax.ws.rs.core.Response;

@Path("/shop")
public class ShopResource {

    private final UserService userService;
    private final ProductService productService;
    private final OrderService orderService;

    @Inject
    public ShopResource(UserService userService, ProductService productService, OrderService orderService) {
        this.userService = userService;
        this.productService = productService;
        this.orderService = orderService;
    }

    public void init(@Observes StartupEvent ev) {
        Order o1 = new Order();
        Product p1 = (Product) Product.find("name", "Pen").firstResult().await().indefinitely();
        Product p2 = (Product) Product.find("name", "Hat").firstResult().await().indefinitely();
        o1.products = List.of(p1, p2);
        o1.userId = UserProfile.findByName("Bob").await().indefinitely().id;
        Panache.withTransaction(()-> Order.persist(o1)).await().indefinitely(); // change to blocking and block the current thread until result arrives
    }

    public Uni<String> addUser(String name) {
        return userService.createUser(name)
                .onFailure().retry().withBackOff(Duration.ofSeconds(5))
                .onItem().transform(id -> "New User " + name + " inserted")
                .onFailure().recoverWithItem(failure -> "User not inserted: " + failure.getMessage());
    }

    @GET
    @Path("user/{name}")
    public Uni<String> getByUserName(@PathParam("name") String name) {
        Uni<UserProfile> uniUser = userService.getUserByName(name);
        return uniUser
                .onItem().transform(user -> user.name);
                .onFailure().recoverWithItem("anonymous");
    }

    @POST
    @Path("/user/{name}")
    public Long createNewUser(@QueryParams("name") String name) {
        return userService.createUser(name)
                .onItem().invoke(item -> System.out.println("New user created: " + name + ", id: " + l))
                .onFailure().invoke(failure -> System.out.println("Cannot create the user " + name + ": " + failure.getMessage()));
    }

    @GET
    @Path("/products")
    public Multi<ProductModel> getProducts() {
        return productService.getAllProducts()
                .onItem().transform(item -> StringUtil.captializeAllFirstLetters(item.name))
                .onFailure().transform(ProductModel::new);
    }

    @GET
    @Path("/orders")
    public Multi<Order> getAllOrders() {
        return userService.getAllUsers()
                .onItem().transformToUniAndConcatenate(user -> orderService.getOrderForUser(user));
        // in case not matter the order
        // return userService.getAllUsers()
        //         .onItem().transformToMultiAndMerge(user -> orderService.getOrderForUser(user));
    }

    @Get
    @Path("/orders/{user}")
    public Multi<Order> getAllOrdersForUser(@PathParam("user") String userName) {
        return userService.getUserByName(userName)
                .onItem().transformToMulti(user -> orderService.getOrderForUser(user));
    }

    @GET
    @Path("/recommendations")
    @Produces(MediaType.SERVER_SENT_EVENTS) // Server-sent events, streaming protocol over HTTP continuously sent to the client from server, incorporating automatical updates
    public Multi<Product> getRecommendations() {
        return Multi.createFrom().ticks().every(Duration.ofSeconds(1))
                .onOverflow().drop() // since the backpressure is not appropriate in this context cause we want all the latest recommendations as fast as possible, so choosing dropif overflowing
                .onItem().transformToUniAndConcatenate(x -> productService.getRecommendations());
    }

    @GET
    @Path("/random-recommendation")
    public Uni<String> getRandomRecommendations() {
        Uni<UserProfile> uni1 = userService.getRandomUser();
        Uni<Product> uni2 = productService.getRecommendedProduct();
        Uni.combine().all().unis(uni1, uni2).asTuple() //convert to tuple
                .onItem().transform(tuple -> "Hello " + tuple.getItem1().name + ", we recommend you " + tuple.getItem2().name);
                return Multi.createBy().combining().streams(u, p ).asTuple()
                            .onItem().transform(tuple -> "Hello " + tuple.getItem1().name", we recommend you "
                            + tuple.getItem2().name);
        }

    @GET
    @Path("/random-recommendations")
    public Multi<String> getRandomRecommendations() {
        Multi<UserProfile> multi1 = Multi.createFrom().ticks().every(Duration.ofSeconds(1)).onOverflow().drop()
                                        .onItem().transformToUniAndConcatenate(x -> userService.getRandomUser());
        Multi<Product> multi2 = Multi.createFrom().ticks().every(Duration.ofSeconds(1)).onOverflow().drop()
                                        .onItem().transformToUniAndConcatenate(y -> productService.getRecommendedProduct());
        Uni<Product> uni2 = productService.getRecommendedProduct();
        return Uni.combine().all().unis(uni1, uni2).asTuple() //convert to tuple
                .onItem().transform(tuple -> "Hello " + tuple.getItem1().name + ", we recommend you " + tuple.getItem2().name);
    }

    @GET
    @Path("/customers")
    public Multi<Customer> findAllCustomers() {
        return Customer.streamAll(Sort.by("name"));
    }

    @GET
    @Path("/customer/{id}")
    public Uni<Response> getCustomer(@RestPath Long id) {
        Uni<Customer> customerUni = Customer.<Customer>findById(id)
                 .onItem().ifNull().failWith(new WebApplicationException("Failed to find customer", Response.Status.NOT_FOUND));
        Uni<List<Order>> customerOrdersUni = orderService.getAllOrdersForCustomer();
        return Uni.combine()
                .all.unis(customerUni, customerOrdersUni).asTuple()
                .onItem().transform(customer -> Response.ok(customer).build());
    }

    @POST
    public Uni<Response> createCustomer(@Valid Customer customer) {
        if (customer.id != null) {
            throw new WebApplicationException("Invalid customer set on request", 422);
        }

        return Panache.withTransaction(customer::persist)
                .replaceWith(Response.ok(customer).status(Response.Status.CREATED).build());
    }

    @PUT
    @Path("/customer/{id}")
    public Uni<Response> updateCustomer(@RestPath Long id, @Valid Customer customer) {
        if (customer.id == null) {
            throw new WebApplicationException("Invalid customer set on request", 422);
        }
        return Panache.withTransaction(
            () -> Customer.<Customer>findById(id)
                .onItem().ifNotNull().invoke(entity -> entity.name = customer.name)
        )
        .onItem().ifNotNull().transform(entity -> Response.ok(entity).build())
        .onItem().ifNull().continuewith(Response.ok(entity).status(Response.Status.NOT_FOUND).build());
    }

    @DELETE
    @Path("/customer/{id}")
    public Uni<Response> deleteCustomer(@RestPath Long id) {
        return Panache.withTransaction(() -> Customer.deleteById(id))
        .map(deleted -> deleted ? Response.ok().status(Response.Status.OK).build() : Response.ok().status(Response.Status.NOT_FOUND).build());
    }
}
