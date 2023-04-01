package io.korandoru.hawkeye.core.mapping;

import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public sealed interface Mapping {

    /**
     * Get the associated header style type; empty if not match.
     */
    Optional<String> headerType(@NotNull String filename);

    record Filename(String patten, String headerType) implements Mapping {
        @Override
        public Optional<String> headerType(@NotNull String filename) {
            final String lowerKey = patten.toLowerCase();
            return filename.equals(lowerKey) ? Optional.of(headerType) : Optional.empty();
        }
    }

    record Extension(String patten, String headerType) implements Mapping {
        @Override
        public Optional<String> headerType(@NotNull String filename) {
            final String lowerKey = patten.toLowerCase();
            return filename.endsWith('.' + lowerKey) ? Optional.of(headerType) : Optional.empty();
        }
    }
}
