package io.korandoru.hawkeye.core;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class MatchPatternTest {

    private static void checkMatches(Path path, boolean isDir, Set<MatchPattern> patterns, Set<String> matches) {
        for (MatchPattern pattern : patterns) {
            final String description = String.format("path=%s, isDir=%b, pattern=%s", path, isDir, pattern);
            assertThat(pattern.match(path, isDir))
                    .describedAs(description)
                    .isEqualTo(matches.contains(pattern.toString()));
        }
    }

    @Test
    void testMatch() {
        final Set<MatchPattern> patterns = Set.of(
                MatchPattern.of("build", false),
                MatchPattern.of("build/", false),
                MatchPattern.of("build/**", false),
                MatchPattern.of("**/build", false),
                MatchPattern.of("**/build/", false),
                MatchPattern.of("**/build/**", false),
                MatchPattern.of("/build", false),
                MatchPattern.of("/build/", false),
                MatchPattern.of("/build/**", false)
        );

        System.out.println(MatchPattern.of("build", false).match(Path.of("build", "hawkeye"), false));
        System.exit(1);
        final Path[] paths = new Path[]{
                Path.of("build"),
                Path.of("build", "hawkeye"),
                Path.of("src", "build"),
                Path.of("src", "build", "hawkeye"),
        };

        final Set<String> allMatches = patterns.stream().map(MatchPattern::toString).collect(Collectors.toSet());
        final Set<String> fileMatches = Set.of("build", "**/build", "/build");
        final Set<String> subMatches = Set.of("build", "build/", "build/**", "**/build", "**/build/", "**/build/**");

        checkMatches(paths[0], false, patterns, fileMatches);
        checkMatches(paths[0], true, patterns, allMatches);
        checkMatches(paths[1], false, patterns, allMatches);
//        checkMatches(paths[1], true, patterns, allMatches);
//        checkMatches(paths[2], false, patterns, fileMatches);
//        checkMatches(paths[2], true, patterns, allMatches);
//        checkMatches(paths[3], false, patterns, fileMatches);
//        checkMatches(paths[3], true, patterns, allMatches);
    }

}
