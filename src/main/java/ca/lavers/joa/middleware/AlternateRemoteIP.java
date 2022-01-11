package ca.lavers.joa.middleware;

import ca.lavers.joa.core.Context;
import ca.lavers.joa.core.Middleware;
import ca.lavers.joa.core.NextMiddleware;
import ca.lavers.joa.core.Request;
import ca.lavers.joa.core.util.WrappedRequest;

import java.util.function.Function;

/**
 * Provides an alternate value for the remote IP address when subsequent Middleware
 * calls {@link Request#remoteIp()}.
 *
 * This can be useful when the application is deployed behind a load balancer or other proxy
 * which provides the real client IP in some other way (such as an HTTP header).
 */
public class AlternateRemoteIP implements Middleware {

    private final Function<Context, String> ipGetter;

    /**
     * Constructs an instance which will call the provided function to retrieve
     * the desired remote IP address. If the function returns null, the original
     * remote IP address of the request will be used.
     *
     * @param ipGetter A function that returns the desired remote IP address for
     *                 a given request context
     */
    public AlternateRemoteIP(Function<Context, String> ipGetter) {
        this.ipGetter = ipGetter;
    }

    /**
     * Constructs an instance which will use the value of the specified request header
     * as the remote IP address. If the request contains no such header, the original
     * remote IP address of the request will be used.
     *
     * @param ipHeader The name of the HTTP header that contains the remote IP address
     */
    public AlternateRemoteIP(String ipHeader) {
        this(ctx -> ctx.request().header(ipHeader));
    }

    @Override
    public void call(Context ctx, NextMiddleware next) {
        String ip = ipGetter.apply(ctx);
        if(ip != null) {
            next.runWithAlternateContext(ctx.withAlternateRequest(new RequestWithNewIP(ip, ctx.request())));
        }
        else {
            next.run();
        }
    }
}

class RequestWithNewIP extends WrappedRequest {

    private final String ip;

    protected RequestWithNewIP(String ip, Request wrapped) {
        super(wrapped);
        this.ip = ip;
    }

    @Override
    public String remoteIp() {
        return ip;
    }
}
