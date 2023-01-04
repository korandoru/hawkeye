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

package io.korandoru.hawkeye.core;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HeaderTest {

    @Test
    void testWindowsLineSeparator() throws Exception {
        final Header header = new Header(new UrlHeaderSource(getClass().getResource("/test-header1.txt")));
        assertThat(header.getHeaderContentLines()).hasSize(13);
        assertThat(header.getHeaderContentOneLine()).contains("${year}");
        assertThat(header.getLocation().isFromUrl(getClass().getResource("/test-header1.txt"))).isTrue();

        final File file = new File("src/test/resources/test-header2.txt");
        final String content = IOUtils.toString(file.toURI(), StandardCharsets.UTF_8);
        assertThat(header.buildForDefinition(HeaderType.ASP.getDefinition(), "\r\n")).isEqualTo(content);
    }
}
