package org.acme.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.acme.data.BankTransaction;
import org.acme.exception.TransactionValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class CsvParser {

    private static final String[] EXPECTED_HEADERS = {"reference", "timestamp", "amount", "currency", "description"};

    private CsvParser() {
    }

    public static List<BankTransaction> parse(InputStream csvStream) {
        List<BankTransaction> transactions = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (Reader reader = new InputStreamReader(csvStream, StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, 
                CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setTrim(true)
                    .build())) {

            validateHeaders(parser);

            for (CSVRecord csvRecord : parser) {
                processRecord(csvRecord, transactions, errors);
            }

            validateResults(transactions, errors);

        } catch (IOException ex) {
            throw TransactionValidationException.withMessage("Unable to read CSV payload: " + ex.getMessage());
        }

        return transactions;
    }

    private static void validateHeaders(CSVParser parser) {
        if (parser.getHeaderMap().isEmpty()) {
            throw TransactionValidationException.withMessage("CSV payload is empty");
        }

        List<String> missingHeaders = new ArrayList<>();
        for (String header : EXPECTED_HEADERS) {
            if (!parser.getHeaderMap().containsKey(header)) {
                missingHeaders.add(header);
            }
        }

        if (!missingHeaders.isEmpty()) {
            throw TransactionValidationException.withMessage(
                "Missing required CSV headers: " + String.join(", ", missingHeaders)
            );
        }
    }

    private static void processRecord(CSVRecord csvRecord, List<BankTransaction> transactions, List<String> errors) {
        int lineNumber = (int) csvRecord.getRecordNumber() + 1;
        List<String> recordErrors = new ArrayList<>();

        String reference = validateReference(lineNumber, csvRecord.get("reference"), recordErrors);
        Instant timestamp = validateTimestamp(lineNumber, csvRecord.get("timestamp"), recordErrors);
        BigDecimal amount = validateAmount(lineNumber, csvRecord.get("amount"), recordErrors);
        String currency = validateCurrency(lineNumber, csvRecord.get("currency"), recordErrors);
        String description = csvRecord.get("description");

        if (recordErrors.isEmpty()) {
            transactions.add(buildTransaction(reference, timestamp, amount, currency, description));
        } else {
            errors.addAll(recordErrors);
        }
    }

    private static String validateReference(int lineNumber, String value, List<String> errors) {
        if (value == null || value.isEmpty()) {
            errors.add(formatErrorMessage(lineNumber, "Missing reference"));
            return null;
        }
        return value;
    }

    private static Instant validateTimestamp(int lineNumber, String value, List<String> errors) {
        if (value == null || value.isEmpty()) {
            errors.add(formatErrorMessage(lineNumber, "Missing timestamp"));
            return null;
        }

        try {
            return Instant.parse(value);
        } catch (Exception ex) {
            errors.add(formatErrorMessage(lineNumber, "Invalid timestamp '%s'".formatted(value)));
            return null;
        }
    }

    private static BigDecimal validateAmount(int lineNumber, String value, List<String> errors) {
        if (value == null || value.isEmpty()) {
            errors.add(formatErrorMessage(lineNumber, "Missing amount"));
            return null;
        }

        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            errors.add(formatErrorMessage(lineNumber, "Invalid amount '%s'".formatted(value)));
            return null;
        }
    }

    private static String validateCurrency(int lineNumber, String value, List<String> errors) {
        if (value == null || value.isEmpty()) {
            errors.add(formatErrorMessage(lineNumber, "Missing currency"));
            return null;
        }

        String upperCurrency = value.toUpperCase();
        if (upperCurrency.length() != 3) {
            errors.add(formatErrorMessage(lineNumber, "Currency must be a 3-letter ISO code"));
            return null;
        }

        return upperCurrency;
    }

    private static BankTransaction buildTransaction(String reference, Instant timestamp, 
                                                    BigDecimal amount, String currency, String description) {
        BankTransaction transaction = new BankTransaction();
        transaction.setReference(reference);
        transaction.setTimestamp(timestamp);
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setDescription(description == null || description.isEmpty() ? null : description);
        return transaction;
    }

    private static void validateResults(List<BankTransaction> transactions, List<String> errors) {
        if (!errors.isEmpty()) {
            throw new TransactionValidationException(errors);
        }

        if (transactions.isEmpty()) {
            throw TransactionValidationException.withMessage("No valid transaction rows found in CSV");
        }
    }

    private static String formatErrorMessage(int lineNumber, String message) {
        return "Line %d: %s".formatted(lineNumber, message);
    }
}
