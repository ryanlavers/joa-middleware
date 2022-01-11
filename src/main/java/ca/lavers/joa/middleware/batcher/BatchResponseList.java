package ca.lavers.joa.middleware.batcher;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class BatchResponseList {

    @JsonProperty("responses")
    private List<BatchResponse> responses = new ArrayList<>();

    public List<BatchResponse> getResponses() {
        return responses;
    }

    public void setResponses(List<BatchResponse> responses) {
        this.responses = responses;
    }

    public void addResponse(BatchResponse response) {
        this.responses.add(response);
    }
}
