package ca.lavers.joa.middleware.batcher;

import com.fasterxml.jackson.annotation.JsonProperty;
import ca.lavers.joa.core.AbstractRequest;
import ca.lavers.joa.core.Request;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class BatchRequest {

    @JsonProperty("method")
    private String method;

    @JsonProperty("path")
    private String path;

    @JsonProperty("queryParams")
    private Map<String, String> queryParams;

    @JsonProperty("headers")
    private Map<String, String> headers;

    @JsonProperty("body")
    private String body;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(Map<String, String> queryParams) {
        this.queryParams = queryParams;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Request asSubRequest(Request parent) {
        return new SubRequest(parent);
    }

    class SubRequest extends AbstractRequest {
        private Request parent;

        SubRequest(Request parent) {
            this.parent = parent;
        }

        @Override
        public String path() {
            return path;
        }

        @Override
        public String method() {
            return method;
        }

        @Override
        public Map<String, String> queryParams() {
            return queryParams;
        }

        @Override
        public Map<String, String> headers() {
            return headers;
        }

        @Override
        public String header(String name) {
            return headers.get(name);
        }

        @Override
        public String remoteIp() {
            return parent.remoteIp();
        }

        @Override
        public InputStream body() throws IOException {
            return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
        }
    }

}
