package dtm.dmanager.exceptions;

public class ApplicationManagerInitializeException extends RuntimeException{

    public ApplicationManagerInitializeException(String message){
        super(message);
    }

    public ApplicationManagerInitializeException(String message, Throwable th){
        super(message, th);
    }
}
