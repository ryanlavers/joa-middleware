package ca.lavers.joa.middleware;

import ca.lavers.joa.core.Context;
import ca.lavers.joa.core.Middleware;
import ca.lavers.joa.core.errors.BadRequestException;

import java.io.IOException;
import java.util.Optional;

/**
 * Deserializes the request body into the desired type and stores it
 * as a Context attribute, before invoking the next middleware. Useful
 * if multiple middleware will want to read and inspect the request body,
 * such as separate validation and handler middlewares.
 *
 * Ex:
 *
 * BodyParser body = new BodyParser();
 * Middleware validateSubmission = ...;
 * ...
 * router.post("/submit", body.parse(Submission.class), validateSubmission, (ctx, next) -> {
 *     body.getBody(ctx, Submission.class).ifPresent(submission -> {
 *       // do something with submission
 *     });
 * });
 */
public class BodyParser {

    // Default Context attributes where we'll store the parsed body object
    public static final String NS = BodyParser.class.getCanonicalName();
    public static final String BODY = "parsedBody";

    private final String ns;
    private final String attr;

    /**
     * Construct a BodyParser which will store the request body in the context
     * under the default namespace and attribute name
     */
    public BodyParser() {
        ns = NS;
        attr = BODY;
    }

    /**
     * Construct a BodyParser which will store the request body in the context
     * under a custom namespace and attribute name
     */
    public BodyParser(String ctxNamespace, String ctxAttrName) {
        ns = ctxNamespace;
        attr = ctxAttrName;
    }

    /**
     * The context namespace this BodyParser will store the request body under
     */
    public String getNs() {
        return ns;
    }

    /**
     * The context attribute name this BodyParser will store the request body under
     */
    public String getAttr() {
        return attr;
    }

    /**
     * Returns a Middleware that will parse the request body into the given type
     * and store it in the context.
     */
    public Middleware parse(Class<?> clz) {
        return (ctx, next) -> {
            try {
                ctx.put(ns, attr, ctx.request().parseBody(clz));
            } catch (IOException e) {
                // TODO -- do we care about e? Should we log it? Feed it to a consumer like ErrorHandler?
                throw new BadRequestException("Unable to parse request body");
            }
            next.run();
        };
    }

    /**
     * Retrieves the deserialized request body (if any) from the context.
     *
     * @param ctx The context containing the request body
     * @param ns The context namespace to look under
     * @param attr The attribute name to look under
     * @param clz The expected type of the request body
     * @return The deserialized request body (if any)
     */
    public static <T> Optional<T> getBody(Context ctx, String ns, String attr, Class<T> clz) {
        return ctx.get(ns, attr, clz);
    }

    /**
     * Retrieves the deserialized request body (if any) from the context.
     *
     * @param ctx The context containing the request body
     * @param clz The expected type of the request body
     * @return The deserialized request body (if any)
     */
    public <T> Optional<T> getBody(Context ctx, Class<T> clz) {
        return ctx.get(ns, attr, clz);
    }
}
