/*
 * Copyright 2023 Korandoru Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.korandoru.hawkeye.core.header;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import io.korandoru.hawkeye.core.resource.HeaderSource;
import io.korandoru.hawkeye.core.resource.ResourceFinder;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class HeaderSourceTest {

    private final ResourceFinder finder = new ResourceFinder(Path.of("src", "test", "resources"));

    @Test
    void testOfInline() {
        HeaderSource actual = HeaderSource.of("inline header", "single-line-header.txt", finder);
        assertThat(actual.getContent()).isEqualTo("inline header");
        assertThat(actual.isInline()).isTrue();
    }

    @Test
    void testOfInlineOnly() {
        HeaderSource actual = HeaderSource.of("inline header", null, null);
        assertThat(actual.getContent()).isEqualTo("inline header");
        assertThat(actual.isInline()).isTrue();
    }

    @Test
    void testOfUrl() {
        HeaderSource actual = HeaderSource.of("", "single-line-header.txt", finder);
        assertThat(actual.getContent()).isEqualTo("just a one line header file for copyright");
        assertThat(actual.isInline()).isFalse();
    }

    @Test
    void testOfUrlOnly() {
        HeaderSource actual = HeaderSource.of(null, "single-line-header.txt", finder);
        assertThat(actual.getContent()).isEqualTo("just a one line header file for copyright");
        assertThat(actual.isInline()).isFalse();
    }

    @Test
    void testOfEmptyAndNull() {
        assertThatThrownBy(() -> HeaderSource.of(null, null, finder)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HeaderSource.of("", null, finder)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HeaderSource.of(null, "", finder)).isInstanceOf(IllegalArgumentException.class);
    }
}
