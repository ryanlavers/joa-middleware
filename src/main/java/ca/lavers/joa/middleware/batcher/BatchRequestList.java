package ca.lavers.joa.middleware.batcher;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class BatchRequestList {

    @JsonProperty("requests")
    private List<BatchRequest> requests;

    public List<BatchRequest> getRequests() {
        return requests;
    }

    public void setRequests(List<BatchRequest> requests) {
        this.requests = requests;
    }
}
