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

package io.korandoru.hawkeye.core.rust;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicReference;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class SharedLibraryLoader {
    private enum LibraryState {
        NOT_LOADED,
        LOADING,
        LOADED,
        FAILED,
    }

    private static final AtomicReference<LibraryState> libraryLoaded = new AtomicReference<>(LibraryState.NOT_LOADED);

    static {
        SharedLibraryLoader.loadLibrary();
    }

    public static void loadLibrary() {
        if (libraryLoaded.get() == LibraryState.LOADED) {
            return;
        }

        if (libraryLoaded.get() == LibraryState.FAILED) {
            return;
        }

        if (libraryLoaded.compareAndSet(LibraryState.NOT_LOADED, LibraryState.LOADING)) {
            try {
                doLoadLibrary();
                log.info("Loaded the hawkeyejni shared library");
                libraryLoaded.set(LibraryState.LOADED);
            } catch (IOException e) {
                log.info("Unable to load the hawkeyejni shared library", e);
                libraryLoaded.set(LibraryState.FAILED);
            }
            return;
        }

        while (libraryLoaded.get() == LibraryState.LOADING) {
            Thread.onSpinWait();
        }
    }

    private static void doLoadLibrary() throws IOException {
        try {
            // try dynamic library - the search path can be configured via "-Djava.library.path"
            System.loadLibrary("hawkeyejni");
            return;
        } catch (UnsatisfiedLinkError ignore) {
            // ignore - try from classpath
        }

        doLoadBundledLibrary();
    }

    private static void doLoadBundledLibrary() throws IOException {
        final String libraryPath = bundledLibraryPath();
        try (final InputStream is = SharedLibraryLoader.class.getResourceAsStream(libraryPath)) {
            if (is == null) {
                throw new IOException("cannot find " + libraryPath);
            }
            final File tmpFile = File.createTempFile("hawkeyejni", null);
            tmpFile.deleteOnExit();
            Files.copy(is, tmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.load(tmpFile.getAbsolutePath());
        }
    }

    private static String bundledLibraryPath() {
        return "/" + System.mapLibraryName("hawkeyejni");
    }
}
