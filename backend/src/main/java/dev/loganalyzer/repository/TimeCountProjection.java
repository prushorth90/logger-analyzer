package dev.loganalyzer.repository;

import java.time.Instant;

public interface TimeCountProjection {
    Instant getTimestamp();

    long getCount();
}