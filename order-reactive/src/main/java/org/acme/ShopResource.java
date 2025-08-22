package org.acme;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.entity.customer.Customer;
import org.acme.entity.users.UserProfile;
import org.acme.services.CustomerRedisService;
import org.acme.services.OrderService;
import org.acme.services.ProductService;
import org.acme.services.UserService;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.panache.common.Sort;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.acme.entity.orders.Order;
import org.acme.entity.orders.Product;
import org.acme.entity.customer.CustomerRedis;
import org.acme.dto.ProductModel;
import org.acme.utils.StringUtil;
import org.jboss.resteasy.reactive.RestPath;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static io.smallrye.mutiny.helpers.spies.Spy.onFailure;

@Path("/shop")
public class ShopResource {

    private final AtomicLong customerId = new AtomicLong(1);
    private final UserService userService;
    private final ProductService productService;
    private final OrderService orderService;
    private final CustomerRedisService customerService;

    @Inject
    public ShopResource(UserService userService, ProductService productService, OrderService orderService, CustomerRedisService customerService) {
        this.userService = userService;
        this.productService = productService;
        this.orderService = orderService;
        this.customerService = customerService;
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
                .onFailure().retry().withBackOff(Duration.ofSeconds(5)).atMost(3)
                .onItem().transform(id -> "New User " + name + " inserted")
                .onFailure().recoverWithItem(failure -> "User not inserted: " + failure.getMessage());
    }

    @GET
    @Path("user/{name}")
    public Uni<String> getByUserName(@PathParam("name") String name) {
        Uni<UserProfile> uniUser = userService.getUserByName(name);
        return uniUser
                .onItem().transform(user -> user.name)
                .onFailure().recoverWithItem("anonymous");
    }

    @POST
    @Path("/user/{name}")
    public Uni<Long> createNewUser(@QueryParam("name") String name) {
        return userService.createUser(name)
                .onItem().invoke(item -> System.out.println("New user created: " + name + ", id: " + item))
                .onFailure().invoke(failure -> System.out.println("Cannot create the user " + name + ": " + failure.getMessage()));
    }

    @GET
    @Path("/products")
    public Multi<ProductModel> getProducts() {
        return productService.getAllOrderedProducts()
                .onItem().transform(product -> new ProductModel(
                        StringUtil.captializeAllFirstLetters(product.getName())
                ))
                .onFailure().recoverWithItem(err ->
                        new ProductModel("Unavailable" )
                );
    }

    @GET
    @Path("/orders")
    public Multi<Order> getAllOrders() {
        return userService.getAllUsers()
                .onItem().transformToUniAndConcatenate(user -> orderService.getOrderForUser(user).toUni());
        // in case not matter the order
        // return userService.getAllUsers()
        //         .onItem().transformToMultiAndMerge(user -> orderService.getOrderForUser(user));
    }

    @GET
    @Path("/orders/{user}")
    public Multi<Order> getAllOrdersForUser(@PathParam("user") String userName) {
        return userService.getUserByName(userName)
                .onItem().transformToMulti(orderService::getOrderForUser);
    }

    @GET
    @Path("/recommendations")
    @Produces(MediaType.SERVER_SENT_EVENTS) // Server-sent events, streaming protocol over HTTP continuously sent to the client from server, incorporating automatically updates
    public Multi<Product> getRecommendations() {
        return Multi.createFrom().ticks().every(Duration.ofSeconds(1))
                .onOverflow().drop() // since the backpressure is not appropriate in this context cause we want all the latest recommendations as fast as possible, so choosing drop if overflowing
                .onItem().transformToUniAndConcatenate(x -> productService.getRecommendedProduct());
    }

    @GET
    @Path("/random-recommendation")
    public Uni<String> getRandomRecommendations() {
        Uni<UserProfile> uni1 = userService.getRandomUser();
        Uni<Product> uni2 = productService.getRecommendedProduct();
        return Uni.combine().all().unis(uni1, uni2).asTuple()
            .onItem().transform(tuple ->
                "Hello " + tuple.getItem1().name + ", we recommend you " + tuple.getItem2().name
            );
        }

    @GET
    @Path("/random-recommendations")
    public Multi<String> getRandomRecommendationsAsCombination() {
        Multi<UserProfile> multi1 = Multi.createFrom().ticks().every(Duration.ofSeconds(1)).onOverflow().drop()
                                        .onItem().transformToUniAndConcatenate(x -> userService.getRandomUser());
        Multi<Product> multi2 = Multi.createFrom().ticks().every(Duration.ofSeconds(1)).onOverflow().drop()
                                        .onItem().transformToUniAndConcatenate(y -> productService.getRecommendedProduct());
        Uni<Product> uni2 = productService.getRecommendedProduct();
        return Multi.createBy().combining().streams(multi1, multi2).asTuple()
                .onItem().transform(tuple ->
                        "Hello " + tuple.getItem1().getName() + ", we recommend you " + tuple.getItem2().getName()
                );
    }

    @GET
    @Path("/customers")
    public Uni<List<Customer>> findAllCustomers() {
        return Customer.findAll(Sort.by("name")).list();
    }

    @GET
    @Path("/customer/{id}")
    public Uni<Response> getCustomer(@RestPath Long id) {
        Uni<Customer> customerUni = Customer.<Customer>findById(id)
                 .onItem().ifNull().failWith(new WebApplicationException("Failed to find customer", Response.Status.NOT_FOUND));
        Uni<List<Order>> customerOrdersUni = orderService.getAllOrdersForCustomer();
        return Uni.combine()
                .all().unis(customerUni, customerOrdersUni).asTuple()
                .onItem().transform(customer -> Response.ok().build());
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
        .onItem().ifNull().continueWith(Response.ok().status(Response.Status.NOT_FOUND).build());
    }

    @DELETE
    @Path("/customer/{id}")
    public Uni<Response> deleteCustomer(@RestPath Long id) {
        return Panache.withTransaction(() -> Customer.deleteById(id))
        .map(deleted -> deleted ? Response.ok().status(Response.Status.OK).build() : Response.ok().status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/customer-redis")
    public Multi<CustomerRedis> getAllCustomersRedis() {
        return customerService.getAllCustomers();
    }

    @GET
    @Path("/customer-redis/{id}")
    public Uni<CustomerRedis> getCustomerRedis(@RestPath Long id) {
        return customerService.getCustomer(id).onItem().ifNull().failWith(new WebApplicationException("Failed to find customer", Response.Status.NOT_FOUND));
    }

    
    @POST
    public Uni<Response> createCustomer(CustomerRedis customer) {
        if (customer.id != null || customer.name.isEmpty()) {
            throw new WebApplicationException("Invalid customer set on request", 422);
        }

        customer.id = customerId.getAndIncrement();

        return customerService.createCustomer(customer)
            .onItem().transform(cust -> Response.ok(cust).status(Response.Status.CREATED).build())
            .onFailure().recoverWithItem(Response.serverError().build());
    }
    
    @POST
    public Uni<Response> updateCustomer(CustomerRedis customer) {
        if (customer.id == null || (customer.name == null || customer.name.isEmpty()))  {
            throw new WebApplicationException("Invalid customer set on request", 422);
        }

        return customerService.updateCustomer(customer)
            .onItem().transform(cust -> Response.ok(cust).status(Response.Status.CREATED).build())
            .onFailure().recoverWithItem(Response.serverError().build());
    }

    @DELETE
    @Path("/customer-redis/{id}")
    public Uni<Response> deleteCustomerRedis(@RestPath Long id) {
        return customerService.deleteCustomer(id)
            .onItem().transform(i -> Response.ok().status(Response.Status.NO_CONTENT).build())
            .onFailure().recoverWithItem(Response.ok().status(Response.Status.NOT_FOUND).build());
    }

    @Channel("upload")
    Emitter<Person> emitter;

    @POST
    public Uni<Response> upload(Person person) {
        System.out.println("emitting " + person.name + " / " + emitter.isCancelled());
        return Uni.createFrom().completionStage(() -> {
            CompletableFuture<Void> future = new CompletableFuture<>();
            Message<Person> msg = Message.of(person, () -> {
                System.out.println("Ack " + person.name);
                future.complete(null);
                return CompletableFuture.completedFuture(null);
            }, f -> {
                System.out.println("Nack " + person.name + " " + f.getMessage());
                future.completeExceptionally(f);
                return CompletableFuture.completedFuture(null);
            });
            emitter.send(msg);
            return future;
        })
                .replaceWith(Response.accepted().build())
                .onFailure().recoverWithItem(t -> Response.status(Response.Status.BAD_REQUEST).entity(t.getMessage()).build());
    }

    @GET
    public Uni<List<Person>> getAll() {
        return Person.listAll();
    }

    @POST
    @Path("/post")
    public Uni<Response> post(Person person) {
        return Panache.withTransaction(() -> person.persistAndFlush())
                .replaceWith(Response.accepted().build())
                .onFailure().recoverWithItem(t -> Response.status(Response.Status.BAD_REQUEST).entity(t.getMessage()).build());
    }
}
