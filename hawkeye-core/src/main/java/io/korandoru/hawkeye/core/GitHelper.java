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
                    .command("git", "status", "--short")
                    .start();
        } catch (IOException e) {
            if (config.getCheckIgnore().isEnable()) {
                throw new Exception("Git NotFound", e);
            } else {
                log.debug("checkIgnore auto is resolved to disable");
                return null;
            }
        }

        if (p.waitFor() != 0) {
            if (config.getCheckIgnore().isEnable()) {
                final String error = IOUtils.toString(p.getErrorStream(), StandardCharsets.UTF_8);
                throw new Exception("Git NotFound: " + error);
            } else {
                log.debug("checkIgnore=auto is resolved to disable");
                return null;
            }
        }

        if (config.getCheckIgnore().isAuto()) {
            log.debug("checkIgnore=auto is resolved to enable");
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
                .command("git", "check-ignore", "--stdin")
                .start();

        final Collection<String> localFiles = files.stream()
                .filter(file -> noSymbolLink(baseDir.resolve(file)))
                .toList();

        log.debug("git check-ignore inputs {}", localFiles);
        final String output;
        try (OutputStream pStdIn = p.getOutputStream()) {
            try (InputStream pStdOut = p.getInputStream()) {
                IOUtils.writeLines(localFiles, null, pStdIn, StandardCharsets.UTF_8);
                pStdIn.flush();
                pStdIn.close();
                output = IOUtils.toString(pStdOut, StandardCharsets.UTF_8);
            }
        }
        log.debug("git check-ignore outputs {}", output);

        final int code = p.waitFor();
        // 0   - One or more of the provided paths is ignored.
        // 1   - None of the provided paths are ignored.
        // 128 - A fatal error was encountered.
        if (code != 0 && code != 1) {
            final String error = IOUtils.toString(p.getErrorStream(), StandardCharsets.UTF_8);
            throw new Exception("git check-ignore failed with code " + code + ":\n" + error);
        }

        final Stream<String> lines = Arrays.stream(output.split(System.lineSeparator()));
        final Set<String> ignoredFiles = lines.collect(Collectors.toSet());
        log.debug("Git ignores files: {}", ignoredFiles);

        files.removeAll(ignoredFiles);
        log.debug("Selected files after filter ignore files: {}", files);
    }

    @SneakyThrows
    private static boolean noSymbolLink(Path path) {
        if (path.toAbsolutePath().normalize().equals(path.toRealPath())) {
            return true;
        }
        log.debug("Skip symbol link {}", path);
        return false;
    }
}
