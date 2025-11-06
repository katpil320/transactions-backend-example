package org.acme.exception;

import java.util.List;

public class TransactionValidationException extends RuntimeException {

    private final List<String> errors;

    public TransactionValidationException(List<String> errors) {
        super(errors == null || errors.isEmpty() ? "Validation failed" : String.join("; ", errors));
        this.errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }

    public static TransactionValidationException withMessage(String message) {
        return new TransactionValidationException(List.of(message));
    }
}
