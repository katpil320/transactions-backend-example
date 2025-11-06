package org.acme.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.transaction.Transactional;
import org.acme.data.BankTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class TransactionResourceTest {

    @BeforeEach
    @Transactional
    void cleanup() {
        BankTransaction.deleteAll();
    }

    @Test
    void testGetTransactionsReturnsHtmlPage() {
        given()
            .when().get("/transactions")
            .then()
            .statusCode(200)
            .contentType(ContentType.HTML)
            .body(containsString("<html"))
            .body(containsString("Transactions"));
    }

    @Test
    void testPostValidCsvCreatesTransactions() {
        String validCsv = """
            reference,timestamp,amount,currency,description
            TX001,2024-01-15T10:30:00Z,100.50,EUR,Payment for services
            TX002,2024-01-16T14:20:00Z,-50.25,USD,Refund
            """;

        given()
            .contentType("text/csv")
            .body(validCsv)
            .when().post("/transactions")
            .then()
            .statusCode(201);

        given()
            .when().get("/transactions")
            .then()
            .statusCode(200)
            .body(containsString("TX001"))
            .body(containsString("100.5"))
            .body(containsString("TX002"))
            .body(containsString("-50.25"));
    }

    @Test
    void testPostDuplicateReferenceReturnsConflict() {
        String firstCsv = """
            reference,timestamp,amount,currency,description
            TX123,2024-01-15T10:30:00Z,100.50,EUR,First transaction
            """;

        given()
            .contentType("text/csv")
            .body(firstCsv)
            .when().post("/transactions")
            .then()
            .statusCode(201);

        String duplicateCsv = """
            reference,timestamp,amount,currency,description
            TX123,2024-01-16T11:00:00Z,200.00,EUR,Duplicate reference
            """;

        given()
            .contentType("text/csv")
            .body(duplicateCsv)
            .when().post("/transactions")
            .then()
            .statusCode(400)
            .body(containsString("References already exist"))
            .body(containsString("TX123"));
    }

    @Test
    void testPostInvalidTimestampReturnsValidationError() {
        String invalidCsv = """
            reference,timestamp,amount,currency,description
            TX001,invalid-date,100.50,EUR,Bad timestamp
            """;

        given()
            .contentType("text/csv")
            .body(invalidCsv)
            .when().post("/transactions")
            .then()
            .statusCode(400)
            .body(containsString("Invalid timestamp"));
    }

    @Test
    void testPostInvalidAmountReturnsValidationError() {
        String invalidCsv = """
            reference,timestamp,amount,currency,description
            TX001,2024-01-15T10:30:00Z,not-a-number,EUR,Bad amount
            """;

        given()
            .contentType("text/csv")
            .body(invalidCsv)
            .when().post("/transactions")
            .then()
            .statusCode(400)
            .body(containsString("Invalid amount"));
    }

    @Test
    void testPostInvalidCurrencyReturnsValidationError() {
        String invalidCsv = """
            reference,timestamp,amount,currency,description
            TX001,2024-01-15T10:30:00Z,100.50,INVALID,Bad currency
            """;

        given()
            .contentType("text/csv")
            .body(invalidCsv)
            .when().post("/transactions")
            .then()
            .statusCode(400)
            .body(containsString("Currency must be a 3-letter ISO code"));
    }

    @Test
    void testPostMissingRequiredFieldReturnsValidationError() {
        String invalidCsv = """
            reference,timestamp,amount,currency,description
            ,2024-01-15T10:30:00Z,100.50,EUR,Missing reference
            """;

        given()
            .contentType("text/csv")
            .body(invalidCsv)
            .when().post("/transactions")
            .then()
            .statusCode(400)
            .body(containsString("Missing reference"));
    }

    @Test
    void testPostEmptyPayloadReturnsValidationError() {
        given()
            .contentType("text/csv")
            .when().post("/transactions")
            .then()
            .statusCode(400)
            .body(containsString("CSV payload is empty"));
    }

    @Test
    void testPostInvalidHeadersReturnsValidationError() {
        String invalidCsv = """
            wrong,headers,here
            TX001,2024-01-15T10:30:00Z,100.50
            """;

        given()
            .contentType("text/csv")
            .body(invalidCsv)
            .when().post("/transactions")
            .then()
            .statusCode(400)
            .body(containsString("Missing required CSV headers"));
    }

    @Test
    void testGetTransactionsDisplaysHighlightedIncomeTransaction() {
        String csvWithLargestIncome = """
            reference,timestamp,amount,currency,description
            TX001,2024-01-15T10:30:00Z,500.00,EUR,Large income
            TX002,2024-01-16T14:20:00Z,100.00,EUR,Small income
            TX003,2024-01-17T09:15:00Z,-50.00,EUR,Expense
            """;

        given()
            .contentType("text/csv")
            .body(csvWithLargestIncome)
            .when().post("/transactions")
            .then()
            .statusCode(201);

        given()
            .when().get("/transactions")
            .then()
            .statusCode(200)
            .body(containsString("TX001"))
            .body(containsString("highlight"));
    }
}
