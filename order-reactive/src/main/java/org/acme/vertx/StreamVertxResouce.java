package org.acme.vertx;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.tuples.Tuple2;
import io.vertx.core.file.OpenOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.file.AsyncFile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.Duration;
import java.util.List;
import java.util.Random;


@Path("/stream-vertx")
public class StreamVertxResouce {

    @Inject
    Vertx vertx;

    @GET
    @Path("/book")
    @Produces(MediaType.TEXT_PLAIN)
    public Multi<String> readBookByMultiBuffer() {
        Multi<Long> ticks = Multi.createFrom().ticks().every(Duration.ofSeconds(1));
        Multi<String> book = vertx.fileSystem().open("war-and-peace.txt", new OpenOptions().setRead(true))
                                .onItem().transformToMulti(AsyncFile::toMulti)
                                .onItem().transform(buffer -> buffer.toString("UTF-8"));
        return Multi.createBy().combining().streams(ticks, book).asTuple()
                                .onItem().transform(Tuple2::getItem2);
    }

    @Inject BookService bookService;

    @GET
    @Path("/books")
    @Produces(MediaType.TEXT_PLAIN)
    public Multi<Book> readBook() {
        Multi<Long> ticks = Multi.createFrom().ticks().every(Duration.ofSeconds(1));
        Multi<Book> books = bookService.getBooks();
        return Multi.createBy().combining().streams(ticks, books).asTuple()
                                .onItem().transform(Tuple2::getItem2);
    }

    @Inject Market market;

    @GET
    @Path("/books")
    @Produces(MediaType.SERVER_SENT_EVENTS)// SSE mechanism will allow to stream unbounded structured data using HTTP
    public Multi<Quote> readBookSSE() {
        return market.getEventStream();
    }

    public static class Book {
        public final long id;
        public final String title;
        public final List<String> authors;

        public Book(long id, String title, List<String> authors) {
            this.id = id;
            this.title = title;
            this.authors = authors;
        }
    }

    @ApplicationScoped
    static class BookService {
        private final List<Book> books = List.of(
                new Book(0, "Fundamentals of Software Architecture", List.of("Mark Richards", "Neal Ford")),
                new Book(1, "Domain-Driven Design", List.of("Eric Evans")),
                new Book(2, "Designing Distributed Systems", List.of("Brendan Burns")),
                new Book(3, "Building Evolutionary Architectures", List.of("Neal Ford", "Rebecca Parsons", "Patrick Kua")),
                new Book(4, "Principles of Concurrent and Distributed Programming", List.of("M. Ben-Ari")),
                new Book(5, "Distributed Systems Observability", List.of("Cindy Sridharan")),
                new Book(6, "Event Streams in Action", List.of("Alexander Dean", "Valentin Crettaz")),
                new Book(7, "Designing Data-Intensive Applications", List.of("Martin Kleppman")),
                new Book(8, "Building Microservices", List.of("Sam Newman")),
                new Book(9, "Kubernetes in Action", List.of("Marko Luksa")),
                new Book(10, "Kafka - the definitive guide", List.of("Gwenn Shapira", "Todd Palino", "Rajini Sivaram", "Krit Petty")),
                new Book(11, "Effective Java", List.of("Joshua Bloch")),
                new Book(12, "Building Event-Driven Microservices", List.of("Adam Bellemare"))
        );

        Multi<Book> getBooks() {
            return Multi.createFrom().iterable(books);
        }
    }

    public static class Quote {
        public final String company;
        public final double value;

        public Quote(String company, double value) {
            this.company = company;
            this.value = value;
        }
    }

    @ApplicationScoped
    public static class Market {
        Multi<Quote> getEventStream() {
            return Multi.createFrom().ticks().every(Duration.ofSeconds(1))
                    .onItem().transform(x -> getRandomQuote());
        }

        Random random = new Random();

        private Quote getRandomQuote() {
            int i = random.nextInt(3);
            String company = "MacroHard";
            if (i ==0) {
                company = "Divinator";
            } else if (i == 1) {
                company = "Black Coat";
            }

            double value = random.nextInt(200) * random.nextDouble();

            return new Quote(company, value);
        }
    }
}