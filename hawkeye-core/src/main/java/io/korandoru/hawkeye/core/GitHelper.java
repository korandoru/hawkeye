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
        final String libraryName = System.mapLibraryName("hawkeyejni");
        try {
            // try dynamic library - the search path can be configured via "-Djava.library.path"
            System.loadLibrary("hawkeyejni");
            log.info("Loaded the {} shared library.", libraryName);
            nativeLibraryLoaded = true;
        } catch (UnsatisfiedLinkError e) {
            log.warn("Unable to load the {} shared library.", libraryName, e);
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
