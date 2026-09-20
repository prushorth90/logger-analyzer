package dev.loganalyzer.search;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.loganalyzer.entity.Severity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class LogSearchQueryParser {
    private static final Pattern TOKEN = Pattern.compile(
            "(?i)(?:^|\\s)(severity|service|environment|traceId):(?:\"([^\"]*)\"|(\\S+))");

    public ParsedLogSearch parse(String query) {
        if (!StringUtils.hasText(query)) {
            return new ParsedLogSearch(null, null, null, null, null);
        }
        if (query.length() > 500) {
            throw new IllegalArgumentException("Search query must not exceed 500 characters");
        }

        String serviceName = null;
        String environment = null;
        Severity severity = null;
        String traceId = null;
        Matcher matcher = TOKEN.matcher(query);
        StringBuilder text = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(text, " ");
            String value = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
            switch (matcher.group(1).toLowerCase(Locale.ROOT)) {
                case "severity" -> severity = parseSeverity(value);
                case "service" -> serviceName = value;
                case "environment" -> environment = value;
                case "traceid" -> traceId = value;
                default -> throw new IllegalStateException("Unexpected search field");
            }
        }
        matcher.appendTail(text);
        String remainingText = text.toString().trim().replaceAll("\\s+", " ");
        return new ParsedLogSearch(remainingText.isEmpty() ? null : remainingText,
                serviceName, environment, severity, traceId);
    }

    private Severity parseSeverity(String value) {
        try {
            return Severity.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("severity must be DEBUG, INFO, WARN, or ERROR");
        }
    }
}