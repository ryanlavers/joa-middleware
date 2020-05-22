package ca.lavers.joa.middleware.errors;

public class NotFoundException extends HttpException {
    public NotFoundException() {
        super(404, "Not Found");
    }
}
