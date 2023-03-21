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
import java.util.StringTokenizer;
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

        log.debug(
                "Selecting files with baseDir: {}, included: {}, excluded: {}",
                basedir,
                Arrays.toString(included),
                Arrays.toString(excluded));

        final Path basePath = basedir.toPath();

        final List<String[]> excludeList = new ArrayList<>();
        final List<String[]> includeList = new ArrayList<>();
        final List<String[]> invertExcludeList = new ArrayList<>();

        for (String exclude : excluded) {
            String pattern = exclude;
            if (pattern.startsWith("!")) {
                pattern = pattern.substring(1);
            }
            if (pattern.endsWith("/")) {
                pattern = pattern.substring(0, pattern.length() - 1);
            }
            if (pattern.endsWith("/**")) {
                pattern = pattern.substring(0, pattern.length() - 3);
            }

            final String[] pattenParts = tokenizePathToString(pattern, "/");
            if (exclude.startsWith("!")) {
                invertExcludeList.add(pattenParts);
            } else {
                excludeList.add(pattenParts);
            }
        }

        for (String include : included) {
            includeList.add(tokenizePathToString(include, "/"));
        }


        final List<String> results = new ArrayList<>();
        final Set<FileVisitOption> followLinksOption = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
        Files.walkFileTree(basePath, followLinksOption, Integer.MAX_VALUE, new FileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                final Path path = basePath.relativize(dir);
                final boolean isExcluded = excludeList.stream().anyMatch(m -> matchPathPattern(m, path, true));
                final boolean isInvertExcluded = invertExcludeList.stream().anyMatch(m -> matchPathPattern(m, path, true));
                return (isExcluded && !isInvertExcluded) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                final Path path = basePath.relativize(file);
                final boolean isIncluded = includeList.stream().anyMatch(m -> matchPathPattern(m, path, true));
                final boolean isExcluded = excludeList.stream().anyMatch(m -> matchPathPattern(m, path, true));
                final boolean isInvertExcluded = invertExcludeList.stream().anyMatch(m -> matchPathPattern(m, path, true));
                if (isIncluded && !(isExcluded && !isInvertExcluded)) {
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

    private static String[] tokenizePathToString(String path, String separator) {
        final List<String> ret = new ArrayList<>();
        final StringTokenizer st = new StringTokenizer(path, separator);
        while (st.hasMoreTokens()) {
            ret.add(st.nextToken());
        }
        return ret.toArray(new String[0]);
    }

    private static boolean matchPathPattern(String[] patDirs, Path path, boolean isCaseSensitive) {
        final String[] strDirs = tokenizePathToString(path.toString(), File.separator);
        return matchPathPattern(patDirs, strDirs, isCaseSensitive);
    }

    private static boolean matchPathPattern(String[] patDirs, String[] strDirs, boolean isCaseSensitive) {
        int patIdxStart = 0;
        int patIdxEnd = patDirs.length - 1;
        int strIdxStart = 0;
        int strIdxEnd = strDirs.length - 1;

        // up to first '**'
        while (patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd) {
            String patDir = patDirs[patIdxStart];
            if (patDir.equals("**")) {
                break;
            }
            if (!match(patDir, strDirs[strIdxStart], isCaseSensitive)) {
                return false;
            }
            patIdxStart++;
            strIdxStart++;
        }
        if (strIdxStart > strIdxEnd) {
            // String is exhausted
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (!patDirs[i].equals("**")) {
                    return false;
                }
            }
            return true;
        } else {
            if (patIdxStart > patIdxEnd) {
                // String not exhausted, but pattern is. Failure.
                return false;
            }
        }

        // up to last '**'
        while (patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd) {
            String patDir = patDirs[patIdxEnd];
            if (patDir.equals("**")) {
                break;
            }
            if (!match(patDir, strDirs[strIdxEnd], isCaseSensitive)) {
                return false;
            }
            patIdxEnd--;
            strIdxEnd--;
        }
        if (strIdxStart > strIdxEnd) {
            // String is exhausted
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (!patDirs[i].equals("**")) {
                    return false;
                }
            }
            return true;
        }

        while (patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd) {
            int patIdxTmp = -1;
            for (int i = patIdxStart + 1; i <= patIdxEnd; i++) {
                if (patDirs[i].equals("**")) {
                    patIdxTmp = i;
                    break;
                }
            }
            if (patIdxTmp == patIdxStart + 1) {
                // '**/**' situation, so skip one
                patIdxStart++;
                continue;
            }
            // Find the pattern between padIdxStart & padIdxTmp in str between
            // strIdxStart & strIdxEnd
            int patLength = (patIdxTmp - patIdxStart - 1);
            int strLength = (strIdxEnd - strIdxStart + 1);
            int foundIdx = -1;
            strLoop:
            for (int i = 0; i <= strLength - patLength; i++) {
                for (int j = 0; j < patLength; j++) {
                    String subPat = patDirs[patIdxStart + j + 1];
                    String subStr = strDirs[strIdxStart + i + j];
                    if (!match(subPat, subStr, isCaseSensitive)) {
                        continue strLoop;
                    }
                }

                foundIdx = strIdxStart + i;
                break;
            }

            if (foundIdx == -1) {
                return false;
            }

            patIdxStart = patIdxTmp;
            strIdxStart = foundIdx + patLength;
        }

        for (int i = patIdxStart; i <= patIdxEnd; i++) {
            if (!patDirs[i].equals("**")) {
                return false;
            }
        }

        return true;
    }

    private static boolean match(String pattern, String str, boolean isCaseSensitive) {
        char[] patArr = pattern.toCharArray();
        char[] strArr = str.toCharArray();
        int patIdxStart = 0;
        int patIdxEnd = patArr.length - 1;
        int strIdxStart = 0;
        int strIdxEnd = strArr.length - 1;
        char ch;

        boolean containsStar = false;
        for (char aPatArr : patArr) {
            if (aPatArr == '*') {
                containsStar = true;
                break;
            }
        }

        if (!containsStar) {
            // No '*'s, so we make a shortcut
            if (patIdxEnd != strIdxEnd) {
                return false; // Pattern and string do not have the same size
            }
            for (int i = 0; i <= patIdxEnd; i++) {
                ch = patArr[i];
                if (ch != '?' && !equals(ch, strArr[i], isCaseSensitive)) {
                    return false; // Character mismatch
                }
            }
            return true; // String matches against pattern
        }

        if (patIdxEnd == 0) {
            return true; // Pattern contains only '*', which matches anything
        }

        // Process characters before first star
        // CHECKSTYLE_OFF: InnerAssignment
        while ((ch = patArr[patIdxStart]) != '*' && strIdxStart <= strIdxEnd)
        // CHECKSTYLE_ON: InnerAssignment
        {
            if (ch != '?' && !equals(ch, strArr[strIdxStart], isCaseSensitive)) {
                return false; // Character mismatch
            }
            patIdxStart++;
            strIdxStart++;
        }
        if (strIdxStart > strIdxEnd) {
            // All characters in the string are used. Check if only '*'s are
            // left in the pattern. If so, we succeeded. Otherwise failure.
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (patArr[i] != '*') {
                    return false;
                }
            }
            return true;
        }

        // Process characters after last star
        // CHECKSTYLE_OFF: InnerAssignment
        while ((ch = patArr[patIdxEnd]) != '*' && strIdxStart <= strIdxEnd)
        // CHECKSTYLE_ON: InnerAssignment
        {
            if (ch != '?' && !equals(ch, strArr[strIdxEnd], isCaseSensitive)) {
                return false; // Character mismatch
            }
            patIdxEnd--;
            strIdxEnd--;
        }
        if (strIdxStart > strIdxEnd) {
            // All characters in the string are used. Check if only '*'s are
            // left in the pattern. If so, we succeeded. Otherwise failure.
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (patArr[i] != '*') {
                    return false;
                }
            }
            return true;
        }

        // process pattern between stars. padIdxStart and patIdxEnd point
        // always to a '*'.
        while (patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd) {
            int patIdxTmp = -1;
            for (int i = patIdxStart + 1; i <= patIdxEnd; i++) {
                if (patArr[i] == '*') {
                    patIdxTmp = i;
                    break;
                }
            }
            if (patIdxTmp == patIdxStart + 1) {
                // Two stars next to each other, skip the first one.
                patIdxStart++;
                continue;
            }
            // Find the pattern between padIdxStart & padIdxTmp in str between
            // strIdxStart & strIdxEnd
            int patLength = (patIdxTmp - patIdxStart - 1);
            int strLength = (strIdxEnd - strIdxStart + 1);
            int foundIdx = -1;
            strLoop:
            for (int i = 0; i <= strLength - patLength; i++) {
                for (int j = 0; j < patLength; j++) {
                    ch = patArr[patIdxStart + j + 1];
                    if (ch != '?' && !equals(ch, strArr[strIdxStart + i + j], isCaseSensitive)) {
                        continue strLoop;
                    }
                }

                foundIdx = strIdxStart + i;
                break;
            }

            if (foundIdx == -1) {
                return false;
            }

            patIdxStart = patIdxTmp;
            strIdxStart = foundIdx + patLength;
        }

        // All characters in the string are used. Check if only '*'s are left
        // in the pattern. If so, we succeeded. Otherwise failure.
        for (int i = patIdxStart; i <= patIdxEnd; i++) {
            if (patArr[i] != '*') {
                return false;
            }
        }
        return true;
    }

    private static boolean equals(char c1, char c2, boolean isCaseSensitive) {
        if (c1 == c2) {
            return true;
        }
        if (!isCaseSensitive) {
            // NOTE: Try both upper case and lower case as done by String.equalsIgnoreCase()
            return Character.toUpperCase(c1) == Character.toUpperCase(c2)
                    || Character.toLowerCase(c1) == Character.toLowerCase(c2);
        }
        return false;
    }
}
