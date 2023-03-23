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

    public static MatchPattern of(String pattern, boolean allowReverse) {
        if (!allowReverse && pattern.startsWith("!")) {
            throw new IllegalArgumentException("reverse pattern is not allowed: " + pattern);
        }

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
        return reverse ^ matchPathPattern(patternParts, strDirs, isDir);
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
            // String is exhausted
            if (patIdxStart > patIdxEnd) {
                // pattern is exhausted. Succeed.
                return true;
            }
            final List<String> remainPatDirs = patDirs.subList(patIdxStart, patIdxEnd + 1);
            if (isConsecutiveAsterisks(remainPatDirs)) {
                if (dirOnly) {
                    return isDir;
                }
                return true;
            }
            return false;
        } else {
            if (patIdxStart > patIdxEnd) {
                // String not exhausted, but pattern is. Failure.
                return false;
            }
        }

        // up to last '**'
        while (patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd) {
            String patDir = patDirs.get(patIdxEnd);
            if (patDir.equals("**")) {
                break;
            }
            if (!match(patDir, strDirs.get(strIdxEnd))) {
                return false;
            }
            patIdxEnd--;
            strIdxEnd--;
        }

        if (strIdxStart > strIdxEnd) {
            // String is exhausted
            final List<String> remainPatDirs = patDirs.subList(patIdxStart, patIdxEnd + 1);
            // MUST consume at least one str part
            // return true if pattern exhausted or all consecutive asterisks
            return remainPatDirs.isEmpty() || isConsecutiveAsterisks(remainPatDirs);
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

        final List<String> remainPatDirs = patDirs.subList(patIdxStart, patIdxEnd + 1);
        return isConsecutiveAsterisks(remainPatDirs);
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
