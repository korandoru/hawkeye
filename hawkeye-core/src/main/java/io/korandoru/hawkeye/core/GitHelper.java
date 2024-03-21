package io.korandoru.hawkeye.core;

import io.korandoru.hawkeye.core.config.FeatureGate;
import io.korandoru.hawkeye.core.config.GitModel;
import io.korandoru.hawkeye.core.rust.ResultException;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GitHelper {
    private static final boolean NATIVE_LIBRARY_LOADED;

    private final long repo;

    static {
        boolean nativeLibraryLoaded = false;
        try {
            // try dynamic library - the search path can be configured via "-Djava.library.path"
            System.loadLibrary("hawkeyejni");
            log.info("Loaded the hawkeyejni shared library.");
            nativeLibraryLoaded = true;
        } catch (UnsatisfiedLinkError e) {
            log.warn("Unable to load the hawkeyejni shared library.", e);
        }
        NATIVE_LIBRARY_LOADED = nativeLibraryLoaded;
    }

    public static GitHelper create(Path baseDir, GitModel config) {
        final FeatureGate checkIgnore = config.getCheckIgnore();
        if (checkIgnore.isDisable()) {
            return null;
        }

        if (!NATIVE_LIBRARY_LOADED) {
            if (checkIgnore.isAuto()) {
                log.info("git.checkIgnore=auto is resolved to disable; unable to load the hawkeyejni shared library.");
                return null;
            }
            throw new ResultException(ResultException.Code.GitError, "Unable to load the hawkeyejni shared library.");
        }

        try {
            final long repo = discoverRepo(baseDir.toAbsolutePath().toString());
            if (checkIgnore.isAuto()) {
                log.info("git.checkIgnore=auto is resolved to enable");
            }
            return new GitHelper(repo);
        } catch (ResultException e) {
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
