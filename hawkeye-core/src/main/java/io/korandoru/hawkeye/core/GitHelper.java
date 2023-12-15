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
import java.nio.file.Path;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.treewalk.filter.PathFilter;

@Slf4j
public final class GitHelper {
    private final Path repoBaseDir;
    private final Repository repository;

    @SneakyThrows
    public static GitHelper create(Path baseDir, GitModel config) {
        if (config.getCheckIgnore() == FeatureGate.DISABLE) {
            return null;
        }

        final RepositoryBuilder build = new RepositoryBuilder();
        build.findGitDir(baseDir.toFile());
        if (build.getGitDir() != null) {
            return new GitHelper(build.build());
        } else if (config.getCheckIgnore() == FeatureGate.AUTO) {
            return null;
        } else {
            throw new Exception("baseDir should be in a Git repository");
        }
    }

    private GitHelper(Repository repository) {
        this.repository = repository;
        this.repoBaseDir = repository.getDirectory().getParentFile().toPath().toAbsolutePath();
    }

    @SneakyThrows
    public boolean checkIgnored(Path file) {
        final String relativePath = repoBaseDir.relativize(file).toString();
        try (final TreeWalk treeWalk = new TreeWalk(repository)) {
            treeWalk.addTree(new FileTreeIterator(repository));
            treeWalk.setFilter(PathFilter.create(relativePath));
            while (treeWalk.next()) {
                final WorkingTreeIterator it = treeWalk.getTree(0, WorkingTreeIterator.class);
                if (it.isEntryIgnored()) {
                    log.debug("File {} has been ignored by Git.", file);
                    return true;
                }
                if (it.getEntryFileMode().equals(FileMode.TREE)) {
                    treeWalk.enterSubtree();
                }
            }
        }
        return false;
    }
}
