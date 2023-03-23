package io.korandoru.hawkeye.core;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MatchPatternTest {

    private record TestCase(Path path, boolean isDir, MatchPattern[] patterns, boolean[] results) {
        private void verify() {
            assertThat(patterns.length).isEqualTo(results.length);
            for (int i = 0; i < patterns.length; i++) {
                assertThat(patterns[i].match(path, isDir))
                        .describedAs("path=%s, isDir=%b, pattern=%s", path, isDir, patterns[i])
                        .isEqualTo(results[i]);
            }
        }
    }

    @Test
    void testMatch() {
        final String dirname = "src";
        final String filename = "hawkeye";
        final String pathname = "build";

        final MatchPattern[] patterns = new MatchPattern[]{
                MatchPattern.of("build", false),
                MatchPattern.of("build/", false),
                MatchPattern.of("build/**", false),
                MatchPattern.of("**/build", false),
                MatchPattern.of("**/build/", false),
                MatchPattern.of("**/build/**", false),
                MatchPattern.of("/build", false),
                MatchPattern.of("/build/", false),
                MatchPattern.of("/build/**", false),
        };

        final Path[] paths = new Path[]{
                Path.of(pathname),
                Path.of(pathname, filename),
                Path.of(dirname, pathname),
                Path.of(dirname, pathname, filename),
        };

        final TestCase[] testCases = new TestCase[]{
                new TestCase(paths[0], false, patterns, new boolean[]{true, false, false, true, false, false, true, false, false}),
                new TestCase(paths[0], true, patterns, new boolean[]{true, false, false, true, false, false, true, false, false}),
                new TestCase(paths[1], false, patterns, new boolean[]{true, false, false, true, false, false, true, false, false}),
                new TestCase(paths[1], true, patterns, new boolean[]{true, false, false, true, false, false, true, false, false}),
                new TestCase(paths[2], false, patterns, new boolean[]{true, false, false, true, false, false, true, false, false}),
                new TestCase(paths[2], true, patterns, new boolean[]{true, false, false, true, false, false, true, false, false}),
                new TestCase(paths[3], false, patterns, new boolean[]{true, false, false, true, false, false, true, false, false}),
                new TestCase(paths[3], true, patterns, new boolean[]{true, false, false, true, false, false, true, false, false}),
        };

        for (TestCase testCase : testCases) {
            testCase.verify();
        }
//
//        for (MatchPattern pattern : patterns) {
//            Path of = Path.of(pathname);
//            System.out.println(pattern.match(of, true));
//            System.out.println(pattern.match(Path.of(dirname, pathname), true));
//            System.out.println(pattern.match(Path.of(pathname, filename), false));
//            System.out.println(pattern.match(of, false));
//            System.out.println();
//        }
    }

}
