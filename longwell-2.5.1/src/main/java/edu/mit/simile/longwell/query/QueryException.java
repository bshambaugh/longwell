package edu.mit.simile.longwell.query;

public class QueryException extends Exception {

    private static final long serialVersionUID = -2502974319369326572L;

    public QueryException(String message) {
        super(message);
    }

    public QueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
