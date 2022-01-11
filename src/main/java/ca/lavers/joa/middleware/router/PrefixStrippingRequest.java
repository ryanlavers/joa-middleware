package ca.lavers.joa.middleware.router;

import ca.lavers.joa.core.Request;
import ca.lavers.joa.core.util.WrappedRequest;

public class PrefixStrippingRequest extends WrappedRequest {

    private final String prefix;

    public PrefixStrippingRequest(Request wrapped, String prefix) {
        super(wrapped);
        this.prefix = prefix;
    }

    @Override
    public String path() {
        final String path = super.path();
        if(path.startsWith(prefix)) {
            return super.path().substring(prefix.length());
        }
        throw new IllegalStateException("Request path does not start with configured prefix; cannot strip!");
    }
}
