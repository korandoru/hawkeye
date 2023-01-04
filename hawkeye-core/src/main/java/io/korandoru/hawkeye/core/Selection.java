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

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;
import lombok.SneakyThrows;

@Getter
public final class Selection {

    private final File basedir;
    private final FileSystem fs;
    private final String[] included;
    private final String[] excluded;

    private final CompletableFuture<String[]> selectedFiles;

    public Selection(File basedir, String[] included, String[] excluded, boolean useDefaultExcludes) {
        this.basedir = basedir;
        this.fs = basedir.toPath().getFileSystem();
        final String[] overrides = buildOverrideInclusions(useDefaultExcludes, included);
        this.included = buildInclusions(included, overrides);
        this.excluded = buildExclusions(useDefaultExcludes, excluded, overrides);
        this.selectedFiles = new CompletableFuture<>();
    }

    @SneakyThrows
    public String[] getSelectedFiles() {
        if (selectedFiles.isDone()) {
            return selectedFiles.getNow(new String[0]);
        }

        final Path basePath = basedir.toPath();
        final List<PathMatcher> folderExcludes = findFolderExcludes();
        final List<PathMatcher> includedPatterns = buildPathMatchers(included);
        final List<PathMatcher> excludedPatterns = buildPathMatchers(excluded);

        final List<String> results = new ArrayList<>();
        Files.walkFileTree(basePath, new FileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)  {
                final Path path = basePath.relativize(dir);
                final boolean isExcluded = folderExcludes.stream().anyMatch(m -> m.matches(path));
                return isExcluded ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                final Path path = basePath.relativize(file);
                final boolean isIncluded = includedPatterns.stream().anyMatch(m -> m.matches(path));
                final boolean isExcluded = excludedPatterns.stream().anyMatch(m -> m.matches(path));
                if (isIncluded && !isExcluded) {
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
        return results.toArray(String[]::new);
    }

    private static String[] buildExclusions(boolean useDefaultExcludes, String[] excludes, String[] overrides) {
        final List<String> exclusions = new ArrayList<>();
        if (useDefaultExcludes) {
            exclusions.addAll(asList(Default.EXCLUDES));
        }
        // remove from the default exclusion list the patterns that have been explicitly included
        for (String override : overrides) {
            exclusions.remove(override);
        }
        if (excludes != null && excludes.length > 0) {
            exclusions.addAll(asList(excludes));
        }
        return exclusions.toArray(new String[0]);
    }

    private static String[] buildInclusions(String[] includes, String[] overrides) {
        // if we use the default exclusion list, we just remove
        final List<String> inclusions = new ArrayList<>(asList(includes != null && includes.length > 0 ? includes : Default.INCLUDE));
        inclusions.removeAll(asList(overrides));
        if (inclusions.isEmpty()) {
            inclusions.addAll(asList(Default.INCLUDE));
        }
        return inclusions.toArray(new String[0]);
    }

    private static String[] buildOverrideInclusions(boolean useDefaultExcludes, String[] includes) {
        // return the list of patterns that we have explicitly included when using default exclude list
        if (!useDefaultExcludes || includes == null || includes.length == 0) {
            return new String[0];
        }
        List<String> overrides = new ArrayList<>(asList(Default.EXCLUDES));
        overrides.retainAll(asList(includes));
        return overrides.toArray(new String[0]);
    }

    private List<PathMatcher> findFolderExcludes() {
        final List<String> excludes = new ArrayList<>();
        for (final String exclude : excluded) {
            if (exclude.endsWith(File.separator) || exclude.endsWith(File.separator + "**")) {
                excludes.add(exclude.substring(0, exclude.lastIndexOf(File.separator)));
            }
            excludes.add(exclude);
        }
        return buildPathMatchers(excludes.toArray(new String[0]));
    }

    private List<PathMatcher> buildPathMatchers(String[] patterns) {
        return stream(patterns)
                .map(p -> p.replaceAll("(?=[^/])\\*\\*/", "{,**/}"))
                .map(p -> fs.getPathMatcher("glob:" + p))
                .toList();
    }
}
