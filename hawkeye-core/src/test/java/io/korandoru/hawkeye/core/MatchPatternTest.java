package io.korandoru.hawkeye.core;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MatchPatternTest {

    @Test
    void testMatch(@TempDir Path tempDir0, @TempDir Path tempDir1) throws Exception {
        final MatchPattern[] patterns = new MatchPattern[]{
                MatchPattern.of("build", false),
                MatchPattern.of("build/", false),
                MatchPattern.of("build/**", false),
        };

        final File buildDir = new File(tempDir0.toFile(), "build");
        final File hawkeyeFile = new File(tempDir0.toFile(), "build/hawkeye");
        FileUtils.forceMkdir(buildDir);
        FileUtils.touch(hawkeyeFile);

        final File buildFile = new File(tempDir1.toFile(), "build");
        FileUtils.touch(buildFile);

        for (MatchPattern pattern: patterns) {
            System.out.println(pattern.match(tempDir0.relativize(buildDir.toPath())));
            System.out.println(pattern.match(tempDir0.relativize(hawkeyeFile.toPath())));
            System.out.println(pattern.match(tempDir1.relativize(buildFile.toPath())));
            System.out.println();
        }
    }

}
