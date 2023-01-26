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

package io.korandoru.hawkeye.core.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ResourceFinderTest {

    private static ResourceFinder finder;

    @BeforeAll
    public static void setup() {
        finder = new ResourceFinder(Paths.get("."));
        finder.setCompileClassPath(List.of("src/test/data/compileCP"));
        finder.setPluginClassPath(ResourceFinderTest.class.getClassLoader());
    }

    @Test
    void testLoadAbsoluteFile() throws Exception {
        final Path path = Paths.get("src").resolve("test/data/compileCP/test.txt").toAbsolutePath();
        assertThat(path).exists();
        final URL url = finder.findResource(path.toString());
        assertThat(Path.of(url.toURI())).isEqualTo(path);
    }

    @Test
    void testLoadNonexistentFile() {
        assertThatThrownBy(() -> finder.findResource("ho ho"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Resource ho ho not found");
    }

    @Test
    void testLoadRelativeFile() {
        final URL url = finder.findResource("src/test/data/compileCP/test.txt");
        assertThat(url.getPath()).contains("src/test/data/compileCP/test.txt");
    }

    @Test
    void testLoadFromCompileCP() {
        assertThat(finder.findResource("test.txt")).isNotNull();
    }

    @Test
    void testLoadFromPluginCP() {
        assertThat(finder.findResource("/bouh.txt")).isNotNull();
    }

    @Test
    void testLoadFromURL() throws Exception {
        final Path path = Paths.get("src").resolve("test/data/compileCP/test.txt").toAbsolutePath();
        assertThat(path).exists();
        final String url = path.toUri().toURL().toString();
        assertThat(finder.findResource(url)).isNotNull();
    }

}
