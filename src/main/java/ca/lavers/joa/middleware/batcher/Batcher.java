package ca.lavers.joa.middleware.batcher;

import ca.lavers.joa.core.Context;
import ca.lavers.joa.core.Middleware;
import ca.lavers.joa.core.NextMiddleware;

import java.io.IOException;
import java.util.UUID;

/**
 * Request-batching middleware
 *
 * Allows clients to specify a batch of requests to carry out as a single POST request to
 * the batch endpoint.
 *
 * If a POST request to the configured path ({@code /batch} by default) is received, then the request body will be interpreted
 * as a list of requests to perform (see {@link BatchRequestList} for the expected format) and the
 * middleware chain following this Batcher will be called once for each of them, with the Context
 * modified appropriately (i.e. other middleware need not be aware of or coded to support batching at all)
 * and then all responses are combined and returned in a single response body (see {@link BatchResponseList} for the format).
 *
 * Where this middleware is installed in the chain matters -- only middleware that comes AFTER the Batcher
 * will be called for each individual sub-request. Any middleware that comes before it will just see the
 * one batch request. Therefore it makes sense to install it before the router (so each sub-request can be
 * routed properly), before the error handler (so that any errors returned by a sub-request will be reported correctly in
 * the batch response, and, depending on requirements, before any request logger (so that each sub-request is logged the
 * same as if it had been sent separately, rather than one log message for the whole batch).
 *
 * For middleware coming after the Batcher, on batch requests there will be two attributes added to the Context:
 * - "batcher"/"originalRequest" is the original Request object representing the batch request itself
 * - "batcher"/"batchId" is a unique ID string generated for this batch request; it will be the same for
 *      all sub-requests in this same batch.
 *
 * Ex.
 *
 * server.use(new Batcher());
 *
 */
public class Batcher implements Middleware {

    public static final String NS = "batcher";

    private final String path;

    public Batcher() {
        this.path = "/batch";
    }

    public Batcher(String path) {
        this.path = path;
    }

    @Override
    public void call(Context ctx, NextMiddleware next) {
        if("POST".equals(ctx.request().method()) && this.path.equals(ctx.request().path())) {
            BatchRequestList body = getBody(ctx);
            if(body == null) {
                ctx.response().status(400);
                ctx.response().body("Bad batch request");
                return;
            }

            final String batchId = UUID.randomUUID().toString();

            BatchResponseList responseList = new BatchResponseList();

            for(BatchRequest r : body.getRequests()) {  // TODO - NPE handling
                // TODO -- handle exceptions thrown by next middleware
                responseList.addResponse(handleSubRequest(r, ctx, batchId, next));
            }

            try {
                ctx.response().body(responseList);
            } catch (IOException e) {
                ctx.response().status(500);
                ctx.response().body("Unable to serialize response");
            }
        }
        else {
            next.run();
        }
    }

    private BatchRequestList getBody(Context ctx) {
        try {
            return ctx.request().parseBody(BatchRequestList.class);
        } catch (Exception e) {
            // TODO -- log or somethin
            return null;
        }
    }

    private BatchResponse handleSubRequest(BatchRequest request, Context ctx, String batchId, NextMiddleware next) {
        BatchResponse response = new BatchResponse();
        Context newContext = new Context(request.asSubRequest(ctx.request()), response);
        // TODO -- copy any attributes from the old context into the new one?
        newContext.put(NS, "originalRequest", ctx.request());
        newContext.put(NS, "batchId", batchId);
        next.runWithAlternateContext(newContext);
        return response;
    }
}
