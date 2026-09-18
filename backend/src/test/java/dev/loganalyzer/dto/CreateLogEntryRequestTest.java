package dev.loganalyzer.dto;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateLogEntryRequestTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsMissingRequiredFields() {
        CreateLogEntryRequest request = new CreateLogEntryRequest(
                null, " ", "", null, "", null, " ", null);

        Set<ConstraintViolation<CreateLogEntryRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("timestamp", "serviceName", "environment", "severity", "message", "host");
    }

    @Test
    void rejectsValuesLongerThanDatabaseColumns() {
        CreateLogEntryRequest request = new CreateLogEntryRequest(
                java.time.Instant.now(), "s".repeat(256), "e".repeat(65),
                dev.loganalyzer.entity.Severity.INFO, "message", "t".repeat(256), "h".repeat(256), null);

        Set<ConstraintViolation<CreateLogEntryRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("serviceName", "environment", "traceId", "host");
    }
}