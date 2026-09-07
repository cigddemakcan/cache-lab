package com.cigdem.cachelab.lab;

public class IntentionalRollbackException extends RuntimeException {

    public IntentionalRollbackException() {
        super("This exception is intentional: the database transaction must roll back");
    }
}
