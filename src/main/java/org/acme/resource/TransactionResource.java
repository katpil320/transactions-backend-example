package org.acme.resource;

import io.quarkus.logging.Log;
import io.quarkus.qute.Template;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;
import java.util.List;

import org.acme.data.BankTransaction;
import org.acme.exception.TransactionValidationException;
import org.acme.service.TransactionService;
import org.acme.service.TransactionViewService;
import org.acme.service.TransactionViewService.TransactionRow;

@Path("/transactions")
public class TransactionResource {

    private final Template transactions;
    private final TransactionService transactionService;
    private final TransactionViewService viewService;

    public TransactionResource(Template transactions, TransactionService transactionService, 
                              TransactionViewService viewService) {
        this.transactions = transactions;
        this.transactionService = transactionService;
        this.viewService = viewService;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String listTransactions() {
        Log.info("Listing all transactions");
        List<BankTransaction> entities = transactionService.getAllTransactionsSortedByTimestamp();
        List<TransactionRow> rows = viewService.buildTransactionRows(entities);
        return transactions.data("transactions", rows).render();
    }

    @POST
    @Consumes("text/csv")
    public Response uploadTransactions(InputStream csvStream) {
        Log.info("Uploading transactions from CSV");
        validatePayload(csvStream);
        transactionService.importCsv(csvStream);
        return Response.status(Response.Status.CREATED).build();
    }

    private void validatePayload(InputStream csvStream) {
        if (csvStream == null) {
            throw TransactionValidationException.withMessage("CSV payload is required");
        }
    }
}