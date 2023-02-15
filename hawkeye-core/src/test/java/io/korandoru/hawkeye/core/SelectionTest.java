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

package io.korandoru.hawkeye.core;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SelectionTest {

    @Test
    void testDefaultSelectAll() {
        final Selection selection = new Selection(new File("."), new String[0], new String[0], false);
        assertThat(selection.getExcluded()).hasSize(0);
        assertThat(selection.getIncluded()).hasSize(1);
        assertThat(selection.getSelectedFiles()).hasSizeGreaterThan(0);
    }

    @Test
    void testLimitInclusion() {
        final Selection selection = new Selection(new File("."), new String[]{"toto"}, new String[]{"tata"}, false);
        assertThat(selection.getExcluded()).hasSize(1);
        assertThat(selection.getIncluded()).hasSize(1);
        assertThat(selection.getSelectedFiles()).hasSize(0);
    }

    @Test
    void testLimitInclusionAndCheckDefaultExcludes() {
        final Selection selection = new Selection(new File("."), new String[]{"toto"}, new String[0], true);
        // default excludes from Scanner and Selection + toto
        assertThat(selection.getExcluded()).hasSameSizeAs(Default.EXCLUDES);
        assertThat(selection.getIncluded()).hasSize(1);
        assertThat(selection.getSelectedFiles()).hasSize(0);
        assertThat(selection.getExcluded()).containsAll(Arrays.asList(Default.EXCLUDES));
    }

    @Test
    void testExclusions(@TempDir Path tempDir) throws IOException {
        final File root = createFakeProject(tempDir);
        final Selection selection = new Selection(
                root,
                new String[]{"**/*.txt"},
                new String[]{"target/**", "module/**/target/**"},
                false);
        final String[] selectedFiles = selection.getSelectedFiles();
        final List<String> expectedSelectedFiles = List.of(
                "included.txt",
                String.join(File.separator, "module", "src", "main", "java", "not-ignored.txt"),
                String.join(File.separator, "module", "sub", "subsub", "src", "main", "java", "not-ignored.txt"));
        assertThat(selectedFiles).containsExactlyInAnyOrderElementsOf(expectedSelectedFiles);
    }

    private File createFakeProject(Path tempDir) throws IOException {
        File temp = tempDir.toFile();
        FileUtils.touch(new File(temp, "included.txt"));
        FileUtils.touch(new File(temp, "target/ignored.txt"));
        FileUtils.touch(new File(temp, "module/src/main/java/not-ignored.txt"));
        FileUtils.touch(new File(temp, "module/target/ignored.txt"));
        FileUtils.touch(new File(temp, "module/sub/subsub/src/main/java/not-ignored.txt"));
        FileUtils.touch(new File(temp, "module/sub/subsub/target/foo/not-ignored.txt"));
        return temp;
    }
}
