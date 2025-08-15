package org.acme.fault;

import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteFilter;
import io.vertx.ext.web.RoutingContext;

import javax.enterprise.context.ApplicationScoped; // If on Quarkus 3+, prefer: jakarta.enterprise.context.ApplicationScoped
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

    private volatile FaultInjection mode = FaultInjection.NONE;
    private volatile double ratio = 0.5;
    private final Random random = new Random();

    /**
     * Configure the fault injection
     *
     * Example:
     *   GET /fault?mode=service_failure&ratio=0.3
     */
    @Route(path = "/fault")
    public void configureFault(RoutingContext rc) {
        String mode = rc.request().getParam("mode");
        String ratio = rc.request().getParam("ratio");

        // Reset if both null
        if (mode == null && ratio == null) {
            this.mode = FaultInjection.NONE;
            this.ratio = 0.5;
            rc.response().end("Fault injection disabled");
            return;
        }

        // Set mode (if provided)
        if (mode != null) {
            try {
                this.mode = FaultInjection.valueOf(mode.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                rc.response().setStatusCode(400)
                  .end("Invalid mode. Allowed: NONE, INBOUND_REQUEST_LOSS, SERVICE_FAILURE, OUTBOUND_RESPONSE_LOSS");
                return;
            }
        }

        // Set ratio (if provided)
        if (ratio != null) {
            try {
                double d = Double.parseDouble(ratio.trim());
                if (d < 0.0 || d > 1.0) {
                    rc.response().setStatusCode(400)
                      .end("Invalid ratio, must be in [0.0, 1.0]");
                    return;
                }
                this.ratio = d;
            } catch (NumberFormatException e) {
                rc.response().setStatusCode(400).end("Invalid ratio (not a number)");
                return;
            }
        }

        rc.response().end("Fault injection enabled: mode=" + this.mode.name() + ", ratio=" + this.ratio);
    }

    /**
     * Filter injecting the fault according to the configured mode and ratio.
     */
    @RouteFilter
    public void injectFault(RoutingContext rc) {
        // Skip faulting the /fault configuration endpoint or when disabled
        if (this.mode == FaultInjection.NONE || "/fault".equals(rc.request().path())) {
            rc.next();
            return;
        }

        // Decide whether to inject a fault this time
        if (random.nextDouble() < this.ratio) {
            switch (this.mode) {
                case INBOUND_REQUEST_LOSS:
                    // Simulate inbound loss: do not call the service
                    // Option A: drop silently (connection will eventually time out) - not user friendly
                    // Option B (recommended): return a 408 Request Timeout or 503
                    rc.response().setStatusCode(408).end("Simulated inbound request loss");
                    break;

                case SERVICE_FAILURE:
                    // Fail fast: don't forward to the next route, just return 500
                    rc.response().setStatusCode(500).end("FAULTY RESPONSE (simulated)");
                    break;

                case OUTBOUND_RESPONSE_LOSS:
                    // Let the service handle the request, then drop the response
                    rc.addBodyEndHandler(v -> {
                        // Close after the route wrote the response (simulate loss)
                        rc.response().close();
                    });
                    rc.next();
                    break;

                default:
                    rc.next();
                    break;
            }
        } else {
            // No fault this time
            rc.next();
        }
    }
}
