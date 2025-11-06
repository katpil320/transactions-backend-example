package org.acme.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.acme.data.BankTransaction;
import org.acme.data.TransactionRowParseResult;
import org.acme.exception.TransactionValidationException;

public final class CsvParser {

    private CsvParser() {
    }

    public static List<BankTransaction> parse(InputStream csvStream) {
        List<String> lines = readLines(csvStream);
        validateNotEmpty(lines);
        return parseAllLines(lines);
    }

    private static void validateNotEmpty(List<String> lines) {
        if (lines.isEmpty()) {
            throw TransactionValidationException.withMessage("CSV payload is empty");
        }
    }

    private static List<BankTransaction> parseAllLines(List<String> lines) {
        List<String> errors = new ArrayList<>();
        List<BankTransaction> candidates = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            processLine(lines.get(i), i, errors, candidates);
        }

        validateNoErrors(errors);
        validateHasCandidates(candidates);

        return candidates;
    }

    private static void processLine(String line, int index, List<String> errors, List<BankTransaction> candidates) {
        String trimmedLine = line.trim();

        if (shouldSkipLine(trimmedLine, index)) {
            return;
        }

        int rowNumber = index + 1;
        TransactionRowParseResult result = parseLine(rowNumber, trimmedLine);
        errors.addAll(result.errors());
        result.transaction().ifPresent(candidates::add);
    }

    private static boolean shouldSkipLine(String line, int index) {
        return line.isEmpty() || (index == 0 && isHeaderLine(line));
    }

    private static boolean isHeaderLine(String line) {
        String lower = line.toLowerCase();
        return lower.contains("reference") && lower.contains("timestamp") && lower.contains("amount");
    }

    private static void validateNoErrors(List<String> errors) {
        if (!errors.isEmpty()) {
            throw new TransactionValidationException(errors);
        }
    }

    private static void validateHasCandidates(List<BankTransaction> candidates) {
        if (candidates.isEmpty()) {
            throw TransactionValidationException.withMessage("No valid transaction rows found in CSV");
        }
    }

    private static List<String> readLines(InputStream csvStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvStream, StandardCharsets.UTF_8))) {
            return reader.lines().toList();
        } catch (IOException ex) {
            throw new TransactionValidationException(List.of("Unable to read CSV payload: " + ex.getMessage()));
        }
    }

    private static TransactionRowParseResult parseLine(int lineNumber, String line) {
        List<String> errors = new ArrayList<>();
        String[] columns = line.split(",", -1);

        if (columns.length < 4) {
            errors.add(formatErrorMessage(lineNumber, "Expected at least 4 columns but found %d".formatted(columns.length)));
            return TransactionRowParseResult.withErrors(errors);
        }

        CsvRow row = new CsvRow(columns);
        validateRequiredFields(lineNumber, row, errors);

        if (!errors.isEmpty()) {
            return TransactionRowParseResult.withErrors(errors);
        }

        return TransactionRowParseResult.success(buildTransaction(row));
    }

    private static void validateRequiredFields(int lineNumber, CsvRow row, List<String> errors) {
        validateReference(lineNumber, row.reference(), errors);
        validateTimestamp(lineNumber, row.timestampValue(), errors);
        validateAmount(lineNumber, row.amountValue(), errors);
        validateCurrency(lineNumber, row.currencyValue(), errors);
    }

    private static void validateReference(int lineNumber, String reference, List<String> errors) {
        if (reference.isEmpty()) {
            errors.add(formatErrorMessage(lineNumber, "Missing reference"));
        }
    }

    private static void validateTimestamp(int lineNumber, String timestampValue, List<String> errors) {
        if (timestampValue.isEmpty()) {
            errors.add(formatErrorMessage(lineNumber, "Missing timestamp"));
            return;
        }

        try {
            Instant.parse(timestampValue);
        } catch (Exception ex) {
            errors.add(formatErrorMessage(lineNumber, "Invalid timestamp '%s'".formatted(timestampValue)));
        }
    }

    private static void validateAmount(int lineNumber, String amountValue, List<String> errors) {
        if (amountValue.isEmpty()) {
            errors.add(formatErrorMessage(lineNumber, "Missing amount"));
            return;
        }

        try {
            new BigDecimal(amountValue);
        } catch (NumberFormatException ex) {
            errors.add(formatErrorMessage(lineNumber, "Invalid amount '%s'".formatted(amountValue)));
        }
    }

    private static void validateCurrency(int lineNumber, String currencyValue, List<String> errors) {
        if (currencyValue.isEmpty()) {
            errors.add(formatErrorMessage(lineNumber, "Missing currency"));
            return;
        }

        if (currencyValue.toUpperCase().length() != 3) {
            errors.add(formatErrorMessage(lineNumber, "Currency must be a 3-letter ISO code"));
        }
    }

    private static BankTransaction buildTransaction(CsvRow row) {
        BankTransaction transaction = new BankTransaction();
        transaction.setReference(row.reference());
        transaction.setTimestamp(Instant.parse(row.timestampValue()));
        transaction.setAmount(new BigDecimal(row.amountValue()));
        transaction.setCurrency(row.currencyValue().toUpperCase());
        transaction.setDescription(row.description().isEmpty() ? null : row.description());
        return transaction;
    }

    private static String formatErrorMessage(int lineNumber, String message) {
        return "Line %d: %s".formatted(lineNumber, message);
    }

    private record CsvRow(String reference, String timestampValue, String amountValue,
                          String currencyValue, String description) {
        CsvRow(String[] columns) {
            this(
                    columns[0].trim(),
                    columns[1].trim(),
                    columns[2].trim(),
                    columns[3].trim(),
                    columns.length >= 5 ? columns[4].trim() : ""
            );
        }
    }
}
