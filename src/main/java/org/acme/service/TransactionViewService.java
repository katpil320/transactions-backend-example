package org.acme.service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.acme.data.BankTransaction;

import io.quarkus.qute.TemplateData;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TransactionViewService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss z")
            .withZone(ZoneId.of("UTC"));

    private static final DecimalFormat AMOUNT_FORMAT = createAmountFormat();

    public List<TransactionRow> buildTransactionRows(List<BankTransaction> entities) {
        String highlightReference = findLargestIncomeReference(entities);
        return entities.stream()
                .map(entity -> toRow(entity, highlightReference))
                .toList();
    }

    private static DecimalFormat createAmountFormat() {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        symbols.setDecimalSeparator('.');
        symbols.setGroupingSeparator(' ');
        return new DecimalFormat("###,##0.##", symbols);
    }

    private String findLargestIncomeReference(List<BankTransaction> entities) {
        return entities.stream()
                .filter(tx -> tx.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .max(Comparator.comparing(BankTransaction::getAmount))
                .map(BankTransaction::getReference)
                .orElse(null);
    }

    private TransactionRow toRow(BankTransaction entity, String highlightReference) {
        boolean highlight = Objects.equals(entity.getReference(), highlightReference);
        return new TransactionRow(
                entity.getReference(),
                DATE_TIME_FORMATTER.format(entity.getTimestamp()),
                formatAmount(entity.getAmount(), entity.getCurrency()),
                entity.getDescription() == null ? "" : entity.getDescription(),
                highlight
        );
    }

    private String formatAmount(BigDecimal amount, String currency) {
        return AMOUNT_FORMAT.format(amount) + " " + currency;
    }

    @TemplateData
    public record TransactionRow(String reference, String formattedTimestamp, String formattedAmount, 
                                 String description, boolean highlight) {
    }
}
