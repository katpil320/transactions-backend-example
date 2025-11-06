package org.acme.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.acme.data.BankTransaction;
import org.acme.exception.TransactionValidationException;

import io.quarkus.logging.Log;

@ApplicationScoped
public class TransactionService {

    @Transactional
    public void importCsv(InputStream csvStream) {
        List<BankTransaction> candidates = CsvParser.parse(csvStream);
        validateNoDuplicatesInPayload(candidates);
        validateNoDuplicatesInDatabase(candidates);
        persistAllTransactions(candidates);
    }

    public List<BankTransaction> getAllTransactionsSortedByTimestamp() {
        return BankTransaction.listAll(io.quarkus.panache.common.Sort.by("timestamp").descending());
    }

    private void validateNoDuplicatesInPayload(List<BankTransaction> candidates) {
        Set<String> references = new HashSet<>();
        for (BankTransaction candidate : candidates) {
            if (!references.add(candidate.getReference())) {
                throw TransactionValidationException.withMessage(
                    "Duplicate reference '%s' in uploaded file".formatted(candidate.getReference())
                );
            }
        }
    }

    private void validateNoDuplicatesInDatabase(List<BankTransaction> candidates) {
        List<String> references = candidates.stream()
            .map(BankTransaction::getReference)
            .toList();

        List<String> existingReferences = BankTransaction.<BankTransaction>list("reference in ?1", references)
            .stream()
            .map(BankTransaction::getReference)
            .toList();

        if (!existingReferences.isEmpty()) {
            throw new TransactionValidationException(
                List.of("References already exist: " + String.join(", ", existingReferences))
            );
        }
    }

    private void persistAllTransactions(List<BankTransaction> candidates) {
        for (BankTransaction transaction : candidates) {
            transaction.persist();
        }
    }
}
