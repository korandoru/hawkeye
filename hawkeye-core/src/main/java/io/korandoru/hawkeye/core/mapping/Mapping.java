/*
 * Copyright 2023 Korandoru Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
