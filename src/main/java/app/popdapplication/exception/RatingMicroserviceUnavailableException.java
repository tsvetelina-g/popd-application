package app.popdapplication.exception;

public class RatingMicroserviceUnavailableException extends RuntimeException{

    public RatingMicroserviceUnavailableException() {
    }

    public RatingMicroserviceUnavailableException(String message) {
        super(message);
    }
}
