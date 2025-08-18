package org.acme.vertx;

import io.smallrye.mutiny.Uni;
import io.vertx.core.file.FileSystemException;
import io.vertx.mutiny.core.Vertx;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

@Path("/multiny-vertx")
public class MultinyVertxResouce {
    
    @Inject
    Vertx vertx;

    @GET
    @Path("/lorem")
    public Uni<String> getLoremIpsum() {
        return vertx.fileSystem().readFile("lorem.txt")
                .onItem().transform(buffer -> buffer.toString("UTF-8"));
    }

    @GET
    @Path("/missing")
    public Uni<String> getMissingFile() {
        return vertx.fileSystem().readFile("oups.txt")
                .onItem().transform(buffer -> buffer.toString("UTF-8"));
    }

    @GET
    @Path("/recover")
    public Uni<String> recover() {
        return vertx.fileSystem().readFile("oups.txt")
                .onItem().transform(buffer -> buffer.toString("UTF-8"))
                .onFailure().recoverWithItem("oups!");
    }

    @GET
    @Path("/404")
    public Uni<Response> get404() {
        return vertx.fileSystem().readFile("oups.txt")
                .onItem().transform(buffer -> buffer.toString("UTF-8"))
                .onItem().transform(content -> Response.ok(content).build())
                .onFailure().recoverWithItem(Response.status(Response.Status.NOT_FOUND).build());
    }

    @ServerExceptionMapper
    public Response mapFileSystemException(FileSystemException e) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(e.getMessage())
                .build();
    }
}