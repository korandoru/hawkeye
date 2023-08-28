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

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public final class MatchPattern {

    private final boolean reverse;
    private final boolean dirOnly;
    private final List<String> patternParts;
    private final String patternOrigin;

    private MatchPattern(String patternOrigin, List<String> patternParts, boolean dirOnly, boolean reverse) {
        this.patternOrigin = patternOrigin;
        this.patternParts = patternParts;
        this.dirOnly = dirOnly;
        this.reverse = reverse;
    }

    public static MatchPattern of(String pattern) {
        String fixedPattern = pattern;

        if (fixedPattern.startsWith("!")) {
            fixedPattern = fixedPattern.substring(1);
        }

        if (fixedPattern.startsWith("/")) {
            fixedPattern = fixedPattern.substring(1);
        } else {
            fixedPattern = "**" + "/" + fixedPattern;
        }

        if (fixedPattern.endsWith("/")) {
            fixedPattern = fixedPattern + "**";
        } else {
            fixedPattern = fixedPattern + "/**";
        }

        return new MatchPattern(
                pattern,
                tokenizePathToString(fixedPattern, "/"),
                pattern.endsWith("/") || pattern.endsWith("/**"),
                pattern.startsWith("!"));
    }

    @Override
    public String toString() {
        return patternOrigin;
    }

    public boolean match(Path path, boolean isDir) {
        final List<String> strDirs = tokenizePathToString(path.toString(), File.separator);
        return matchPathPattern(patternParts, strDirs, isDir);
    }

    public boolean isReverse() {
        return reverse;
    }

    private static List<String> tokenizePathToString(String path, String separator) {
        final List<String> result = new ArrayList<>();
        final StringTokenizer tokenizer = new StringTokenizer(path, separator);
        while (tokenizer.hasMoreTokens()) {
            result.add(tokenizer.nextToken());
        }
        return result;
    }

    private boolean matchPathPattern(List<String> patDirs, List<String> strDirs, boolean isDir) {
        int patIdxStart = 0;
        int patIdxEnd = patDirs.size() - 1;
        int strIdxStart = 0;
        int strIdxEnd = strDirs.size() - 1;

        // up to first '**'
        while (patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd) {
            String patDir = patDirs.get(patIdxStart);
            if (patDir.equals("**")) {
                break;
            }
            if (!match(patDir, strDirs.get(strIdxStart))) {
                return false;
            }
            patIdxStart++;
            strIdxStart++;
        }

        if (strIdxStart > strIdxEnd) {
            return checkPatternOnStringExhausted(patDirs.subList(patIdxStart, patIdxEnd + 1), isDir);
        }

        while (patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd) {
            int patIdxTmp = -1;
            for (int i = patIdxStart + 1; i <= patIdxEnd; i++) {
                if (patDirs.get(i).equals("**")) {
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
                    String subPat = patDirs.get(patIdxStart + j + 1);
                    String subStr = strDirs.get(strIdxStart + i + j);
                    if (!match(subPat, subStr)) {
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

        if (strIdxStart > strIdxEnd) {
            return checkPatternOnStringExhausted(patDirs.subList(patIdxStart, patIdxEnd + 1), isDir);
        }

        // String is not exhausted - MUST remains the last double asterisks
        return true;
    }

    private boolean checkPatternOnStringExhausted(List<String> patDirs, boolean isDir) {
        if (patDirs.isEmpty()) {
            // pattern is exhausted - Succeed
            return true;
        }

        if (isConsecutiveAsterisks(patDirs)) {
            if (dirOnly) {
                return isDir;
            }
            return true;
        }

        return false;
    }

    private boolean isConsecutiveAsterisks(List<String> patDirs) {
        return !patDirs.isEmpty() && patDirs.stream().allMatch("**"::equals);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean match(String pattern, String str) {
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
                if (ch != '?' && ch != strArr[i]) {
                    return false; // Character mismatch
                }
            }
            return true; // String matches against pattern
        }

        if (patIdxEnd == 0) {
            return true; // Pattern contains only '*', which matches anything
        }

        // Process characters before first star
        while ((ch = patArr[patIdxStart]) != '*' && strIdxStart <= strIdxEnd) {
            if (ch != '?' && ch != strArr[strIdxStart]) {
                return false; // Character mismatch
            }
            patIdxStart++;
            strIdxStart++;
        }
        if (strIdxStart > strIdxEnd) {
            // All characters in the string are used. Check if only '*'s are
            // left in the pattern. If so, we succeeded; otherwise, we failed.
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (patArr[i] != '*') {
                    return false;
                }
            }
            return true;
        }

        // Process characters after last star
        while ((ch = patArr[patIdxEnd]) != '*' && strIdxStart <= strIdxEnd) {
            if (ch != '?' && ch != strArr[strIdxEnd]) {
                return false; // Character mismatch
            }
            patIdxEnd--;
            strIdxEnd--;
        }
        if (strIdxStart > strIdxEnd) {
            // All characters in the string are used. Check if only '*'s are
            // left in the pattern. If so, we succeeded; otherwise, we failed.
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
                    if (ch != '?' && ch != strArr[strIdxStart + i + j]) {
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
        // in the pattern. If so, we succeeded; otherwise, we failed.
        for (int i = patIdxStart; i <= patIdxEnd; i++) {
            if (patArr[i] != '*') {
                return false;
            }
        }
        return true;
    }
}
