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

import static java.util.Arrays.stream;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public final class Selection {

    private final File basedir;
    private final FileSystem fs;
    private final String[] included;
    private final String[] excluded;

    private final CompletableFuture<String[]> selectedFiles;

    public Selection(File basedir, String[] included, String[] excluded, boolean useDefaultExcludes) {
        this.basedir = basedir;
        this.fs = basedir.toPath().getFileSystem();
        final String[] usedDefaultExcludes = useDefaultExcludes ? Default.EXCLUDES : new String[0];
        this.included = included.length > 0 ? included : Default.INCLUDE;
        this.excluded = Stream.concat(stream(usedDefaultExcludes), stream(excluded)).toArray(String[]::new);
        this.selectedFiles = new CompletableFuture<>();
    }

    @SneakyThrows
    public String[] getSelectedFiles() {
        if (selectedFiles.isDone()) {
            final String[] files = selectedFiles.get(0, TimeUnit.SECONDS);
            log.debug("Got previous selected files: {} (count: {})", Arrays.toString(files), files.length);
            return files;
        }

        log.debug("Selecting files with baseDir: {}, included: {}, excluded: {}", basedir, Arrays.toString(included), Arrays.toString(excluded));

        final Path basePath = basedir.toPath();

        final List<MatchPattern> excludeList = new ArrayList<>();
        final List<MatchPattern> includeList = new ArrayList<>();

        for (String exclude : excluded) {
            excludeList.add(MatchPattern.of(exclude));
        }

        for (String include : included) {
            if (include.startsWith("!")) {
                throw new IllegalArgumentException("reverse include pattern is not allowed: " + include);
            }
            includeList.add(MatchPattern.of(include));
        }

        final List<String> results = new ArrayList<>();
        final Set<FileVisitOption> followLinksOption = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
        Files.walkFileTree(basePath, followLinksOption, Integer.MAX_VALUE, new FileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                final Path path = basePath.relativize(dir);
                final boolean isExcluded = excludeList.stream().filter(m -> !m.isReverse()).anyMatch(m -> m.match(path, true));
                final boolean isReserveExcluded = excludeList.stream().filter(MatchPattern::isReverse).anyMatch(m -> m.match(path, true));
                return (isExcluded && !isReserveExcluded) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                final Path path = basePath.relativize(file);
                final boolean isIncluded = includeList.stream().anyMatch(m -> m.match(path, false));
                final boolean isExcluded = excludeList.stream().filter(m -> !m.isReverse()).anyMatch(m -> m.match(path, false));
                final boolean isReserveExcluded = excludeList.stream().filter(MatchPattern::isReverse).anyMatch(m -> m.match(path, false));
                if (isIncluded && !(isExcluded && !isReserveExcluded)) {
                    results.add(path.toString());
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });

        this.selectedFiles.complete(results.toArray(String[]::new));
        final String[] files = selectedFiles.get(0, TimeUnit.SECONDS);
        log.debug("Selected files: {} (count: {})", Arrays.toString(files), files.length);

        return files;
    }
}
