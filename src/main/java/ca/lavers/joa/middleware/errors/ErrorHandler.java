package ca.lavers.joa.middleware.errors;

import ca.lavers.joa.core.Context;
import ca.lavers.joa.core.Middleware;
import ca.lavers.joa.core.NextMiddleware;
import ca.lavers.joa.core.errors.HttpException;

import java.util.function.Consumer;

/**
 * Middleware that catches and handles any exceptions thrown from further down
 * the chain
 *
 * If the exception is a subclass of {@link HttpException}, it will set the status
 * and error message appropriately. Otherwise, it will respond with a 500 Internal
 * Server Error to prevent exception leakage to the client.
 *
 * Can be optionally given a function that will be called with every caught exception,
 * so that additional handling can be done (such as logging).
 *
 * Ex.
 *
 *     server.use(new ErrorHandler(e -> {
 *       // Log the full stacktrace of any non-HttpException
 *       if(!(e instanceof HttpException)) {
 *         e.printStackTrace();
 *       }
 *     }));
 */
public class ErrorHandler implements Middleware {

    private final Consumer<Exception> onException;

    public ErrorHandler() {
        onException = (e) -> {};
    }

    public ErrorHandler(Consumer<Exception> onException) {
        this.onException = onException;
    }

    @Override
    public void call(Context ctx, NextMiddleware next) {
        try {
            next.run();
        }
        catch (Exception e) {
            if(e instanceof HttpException) {
                HttpException he = (HttpException) e;
                ctx.response().status(he.getStatus());
                ctx.response().body(he.getReturnedMessage());
            }
            else {
                ctx.response().status(500);
                ctx.response().body("Internal Server Error");
            }

            try {
                onException.accept(e);
            } catch (Exception e2) {
                // ...
            }
        }
    }

}
