package app.popdapplication.exception;

public class ReviewMicroserviceUnavailableException extends RuntimeException{

    public ReviewMicroserviceUnavailableException() {
    }

    public ReviewMicroserviceUnavailableException(String message) {
        super(message);
    }
}
