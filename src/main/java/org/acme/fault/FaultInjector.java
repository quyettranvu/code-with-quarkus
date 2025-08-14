package org.acme.fault;

import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteFilter;
import io.vertx.ext.web.RoutingContext;

import javax.enterprise.context.ApplicationScoped;
import java.util.Random;

@ApplicationScoped
public class FaultInjector {
    // Interceptor handling HTTP request and response

    enum FaultInjection {
        NONE,
        INBOUND_REQUEST_LOSS,
        SERVICE_FAILURE,
        OUTBOUND_RESPONSE_LOSS
    }
    
    FaultInjection mode = FaultInjection.NONE;
    double ratio = 0.5;
    Random random = new Random();

    /**
     * Configure the fault injection
     *
     * @param rc the routing context
     */
    @Route(path = "/fault")
    public void configureFault(RoutingContext rc) {
        String mode = rc.request().getParam("mode");
        String ratio = rc.request().getParam("ratio");

        // initialize as default if null
        if (mode == null && ratio == null) {
            // reset
            this.mode = FaultInjection.NONE;
            this.ratio = 0.5;
            rc.response().end("Fault injection disabled");
            return;
        }
        
        // set mode and ratio of fault
        if (mode != null) {
            this.mode = FaultInjection.valueOf(mode.toUpperCase());
        }
        if (ratio != null) {
            double d = Double.parseDouble(ratio);
            if (d > 1) {
                rc.response()
                        .setStatusCode(400)
                        .end("Invalid ratio, must be in [0, 1)");
                return;
            }
            this.ratio = d;
        }
        rc.response().end("Fault injection enabled: mode=" + this.mode.name() + ", ratio=" + this.ratio);
    }
    
    /**
     * Filter injected the fault according to the configured mode and ratio.
     *
     * @param rc the routing context
     */
    @RouteFilter
    public void injectFault(RoutingContext rc) {
        // If the fault injection is disabled or the request targets the fault injection configuration,
        // just let it pass.
        if (this.mode == FaultInjection.NONE || rc.request().path().equals("/fault")) {
            rc.next();
            return;
        }

        if (ratio > random.nextDouble()) {
            swich(this.mode) {
                case INBOUND_REQUEST_LOSS:
                    //Inbound request is loss, do not call the service and no need to inject fault
                    break;
                case SERVICE_FAILURE:
                    rc.next();
                    rc.response()
                        .setStatusCode(500)
                        .end("FAULTY RESPONSE");
                    break;
                case OUTBOUND_RESPONSE_LOSS:
                    rc.next();
                    rc.addBodyEndHandler(v -> {
                        // Close after the route wrote the response (simulate loss)
                        rc.response().close();
                    });
                    break;
                default:
                    rc.next();
                    break;
            }
        } else {
            // if still within the range
            rc.next();
        }
    }
}