package org.acme.data;

import java.util.List;
import java.util.Optional;

public record TransactionRowParseResult(Optional<BankTransaction> transaction, List<String> errors) {

    public static TransactionRowParseResult success(BankTransaction transaction) {
        return new TransactionRowParseResult(Optional.of(transaction), List.of());
    }

    public static TransactionRowParseResult withErrors(List<String> errors) {
        return new TransactionRowParseResult(Optional.empty(), List.copyOf(errors));
    }
}
