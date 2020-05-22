package ca.lavers.joa.middleware.errors;

public class BadRequestException extends HttpException {
    public BadRequestException() {
        super(400, "Bad request");
    }
}
