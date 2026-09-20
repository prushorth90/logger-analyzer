package dev.loganalyzer.search;

import dev.loganalyzer.entity.Severity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogSearchQueryParserTest {
    private final LogSearchQueryParser parser = new LogSearchQueryParser();

    @Test
    void extractsAllowlistedTokensAndLeavesSearchText() {
        ParsedLogSearch parsed = parser.parse(
                "severity:ERROR service:payment-service environment:production traceId:trace-42 provider timeout");

        assertThat(parsed.severity()).isEqualTo(Severity.ERROR);
        assertThat(parsed.serviceName()).isEqualTo("payment-service");
        assertThat(parsed.environment()).isEqualTo("production");
        assertThat(parsed.traceId()).isEqualTo("trace-42");
        assertThat(parsed.text()).isEqualTo("provider timeout");
    }

    @Test
    void treatsUnknownFieldsAsPlainText() {
        assertThat(parser.parse("queryJson:{match_all:true} payment").text())
                .isEqualTo("queryJson:{match_all:true} payment");
    }

    @Test
    void rejectsInvalidSeverity() {
        assertThatThrownBy(() -> parser.parse("severity:CRITICAL payment"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("severity must be");
    }
}