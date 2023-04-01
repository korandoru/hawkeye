package io.korandoru.hawkeye.core.mapping;

import java.util.Objects;
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Filename filename = (Filename) o;
            return Objects.equals(patten, filename.patten);
        }

        @Override
        public int hashCode() {
            return patten != null ? patten.hashCode() : 0;
        }
    }

    record Extension(String patten, String headerType) implements Mapping {
        @Override
        public Optional<String> headerType(@NotNull String filename) {
            final String lowerKey = patten.toLowerCase();
            return filename.endsWith('.' + lowerKey) ? Optional.of(headerType) : Optional.empty();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Extension filename = (Extension) o;
            return Objects.equals(patten, filename.patten);
        }

        @Override
        public int hashCode() {
            return patten != null ? patten.hashCode() : 0;
        }
    }
}
