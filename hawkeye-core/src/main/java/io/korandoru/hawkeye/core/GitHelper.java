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

import io.korandoru.hawkeye.core.config.GitModel;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

@Slf4j
public class GitHelper {
    private final Path baseDir;

    @SneakyThrows
    public static GitHelper create(Path baseDir, GitModel config) {
        if (config.getCheckIgnore().isDisable()) {
            return null;
        }

        final Process p;
        try {
            p = new ProcessBuilder()
                    .directory(baseDir.toFile())
                    .command("which", "git")
                    .start();
        } catch (IOException e) {
            if (config.getCheckIgnore().isAuto()) {
                return null;
            } else {
                throw new Exception("cannot perform check-ignore", e);
            }
        }

        if (p.waitFor() != 0) {
            if (config.getCheckIgnore().isEnable()) {
                final String error = IOUtils.toString(p.getErrorStream(), StandardCharsets.UTF_8);
                throw new Exception("cannot perform check-ignore: " + error);
            } else {
                return null;
            }
        }

        return new GitHelper(baseDir);
    }

    public GitHelper(Path baseDir) {
        this.baseDir = baseDir;
    }

    @SneakyThrows
    public void filterIgnoredFiles(Collection<String> files) {
        final Process p = new ProcessBuilder()
                .directory(baseDir.toFile())
                .command("git", "check-ignore", "--stdin", "--no-index")
                .start();

        final String output, error;
        try (final InputStream in = p.getInputStream();
                final InputStream err = p.getErrorStream();
                final OutputStream out = p.getOutputStream()) {
            IOUtils.writeLines(files, null, out, StandardCharsets.UTF_8);
            out.flush();
            out.close();
            output = IOUtils.toString(in, StandardCharsets.UTF_8);
            error = IOUtils.toString(err, StandardCharsets.UTF_8);
        }
        log.warn("Git check-ignore output: {}", output);
        log.warn("Git check-ignore error output: {}", error);

        final Stream<String> lines = Arrays.stream(output.split(System.lineSeparator()));
        final Set<String> ignoredFiles = lines.collect(Collectors.toSet());
        log.debug("Git ignores files: {}", ignoredFiles);

        files.removeAll(ignoredFiles);
        log.debug("Selected files after filter ignore files: {}", files);
    }
}
