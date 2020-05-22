package ca.lavers.joa.middleware.batcher;

import com.fasterxml.jackson.annotation.JsonGetter;
import ca.lavers.joa.core.AbstractResponse;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class BatchResponse extends AbstractResponse {

    @JsonGetter("body")
    public String bodyAsString() {
        String text;
        try (Scanner scanner = new Scanner(this.body, StandardCharsets.UTF_8.name())) {
            text = scanner.useDelimiter("\\A").next();
            scanner.close();
            return text;
        }
    }

}
