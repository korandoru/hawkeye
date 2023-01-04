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
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class HeaderDefinitionTest {

    @Test
    void testConstruction() {
        assertThatThrownBy(() -> HeaderDefinition.builder().build())
                .isInstanceOf(NullPointerException.class)
                .hasMessage("type");

        assertThatThrownBy(() -> HeaderDefinition.builder().type("SCRIPT_STYLE").build())
                .isInstanceOf(NullPointerException.class)
                .hasMessage("firstLineDetectionPattern");

        final HeaderDefinition definition = HeaderDefinition.builder()
                .type("SCRIPT_STYLE")
                .firstLineDetectionPattern(Pattern.compile("#.*$"))
                .lastLineDetectionPattern(Pattern.compile("#.*$"))
                .build();
        assertThat(definition).isNotNull();
        assertThat(definition.getType()).isEqualTo("script_style");
    }

}
