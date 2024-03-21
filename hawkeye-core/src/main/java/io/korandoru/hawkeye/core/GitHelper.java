package io.korandoru.hawkeye.core;

import io.korandoru.hawkeye.core.config.FeatureGate;
import io.korandoru.hawkeye.core.config.GitModel;
import io.korandoru.hawkeye.core.rust.ResultException;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GitHelper {
    private final long repo;

    static {
        try {
            // try dynamic library - the search path can be configured via "-Djava.library.path"
            System.loadLibrary("hawkeyejni");
            log.debug("Loaded the hawkeyejni shared library.");
        } catch (UnsatisfiedLinkError e) {
            // ignore - try from classpath
            log.debug("Unable to load the hawkeyejni shared library.", e);
        }
    }

    public static GitHelper create(Path baseDir, GitModel config) {
        final FeatureGate checkIgnore = config.getCheckIgnore();
        if (checkIgnore.isDisable()) {
            return null;
        }
        try {
            final long repo = discoverRepo(baseDir.toAbsolutePath().toString());
            if (checkIgnore.isAuto()) {
                log.info("git.checkIgnore=auto is resolved to enable");
            }
            return new GitHelper(repo);
        } catch (UnsatisfiedLinkError | ResultException e) {
            if (checkIgnore.isAuto()) {
                log.info("git.checkIgnore=auto is resolved to disable", e);
                return null;
            }
            throw e;
        }
    }

    private GitHelper(long repo) {
        this.repo = repo;
    }

    public boolean isPathIgnored(String path) {
        return isPathIgnored(repo, path);
    }

    private static native long discoverRepo(String baseDir);
    private static native boolean isPathIgnored(long repo, String path);
}
