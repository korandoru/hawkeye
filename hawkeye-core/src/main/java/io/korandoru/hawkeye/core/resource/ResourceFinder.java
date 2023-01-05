/*
 * Copyright 2023 Korandoru Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.korandoru.hawkeye.core.resource;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class ResourceFinder {
    private final Path basedir;
    private URLClassLoader compileClassPath;
    private URLClassLoader pluginClassPath;

    public ResourceFinder(final Path basedir) {
        this.basedir = basedir;
    }

    public void setCompileClassPath(List<String> classpath) {
        final List<URL> urls = new ArrayList<>();
        if (classpath != null) {
            for (String absolutePath : classpath) {
                final File file = new File(absolutePath);
                if (file.isDirectory()) {
                    try {
                        urls.add(file.toURI().toURL());
                    } catch (MalformedURLException e) {
                        throw new IllegalArgumentException("Malformed URL " + file, e);
                    }
                }
            }
        }
        this.compileClassPath = new URLClassLoader(urls.toArray(new URL[0]));
    }

    public void setPluginClassPath(ClassLoader classLoader) {
        this.pluginClassPath = new URLClassLoader(new URL[0], classLoader);
    }

    /**
     * Find a resource by searching:
     *
     * <ol>
     * <li>In the filesystem, relative to basedir</li>
     * <li>In the filesystem, as an absolute path (or relative to current execution directory)</li>
     * <li>In project classpath</li>
     * <li>In plugin classpath</li>
     * <li>As a URL</li>
     * </ol>
     *
     * @param resource to find
     * @return a valid URL
     * @throws ResourceNotFoundException if the resource is not found
     */
    @NotNull
    public URL findResource(String resource) {
        URL res = null;

        // first search relatively to the base directory
        try {
            final Path p = basedir.resolve(resource);
            res = toURL(p.toAbsolutePath());
        } catch (final InvalidPathException e) {
            // no-op - can be caused by resource being a URI on windows when Path.resolve is called
        }

        if (res != null) {
            return res;
        }

        // if not found, search for absolute location on file system, or relative to execution dir
        try {
            res = toURL(Paths.get(resource));
        } catch (final InvalidPathException e) {
            // no-op - can be caused by resource being a URI on windows when Paths.get is called
        }
        if (res != null) {
            return res;
        }

        // if not found, try the classpath
        final String cpResource = resource.startsWith("/") ? resource.substring(1) : resource;

        // tries compile-classpath of project
        if (compileClassPath != null) {
            res = compileClassPath.getResource(cpResource);
            if (res != null) {
                return res;
            }
        }

        // tries this plugin classpath
        if (pluginClassPath != null) {
            res = pluginClassPath.getResource(cpResource);
            if (res != null) {
                return res;
            }
        }

        // otherwise, tries to return a valid URL
        try {
            res = new URL(resource);
            res.openStream().close();
            return res;
        } catch (IOException e) {
            throw new ResourceNotFoundException("Resource " + resource + " not found", e);
        }
    }

    private URL toURL(final Path path) {
        if (Files.exists(path) && Files.isReadable(path)) {
            try {
                return path.toUri().toURL();
            } catch (MalformedURLException ignored) {
                return null;
            }
        }
        return null;
    }
}
