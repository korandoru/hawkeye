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

package io.korandoru.hawkeye.maven.plugin;

import io.korandoru.hawkeye.core.config.HawkEyeConfig;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

abstract class AbstractMojo extends org.apache.maven.plugin.AbstractMojo {
    /**
     * Location of the `licenserc.toml` file.
     */
    @Parameter(property = "hawkeye.configLocation", defaultValue = "${project.basedir}/licenserc.toml")
    public File configLocation;

    /**
     * Whether to do the real formatting or removal.
     */
    @Parameter(property = "hawkeye.dryRun", defaultValue = "false")
    public boolean dryRun;

    /**
     * You can set this flag to true if you want to check the headers for all
     * modules of your project. Only used for multi-modules projects, to check
     * for example the header licenses from the parent module for all submodules.
     */
    @Parameter(property = "hawkeye.aggregate", defaultValue = "false")
    public boolean aggregate = false;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    public MavenProject project;

    protected HawkEyeConfig.Builder configBuilder() {
        final List<String> submodulesExcludes = new ArrayList<>();
        if (project != null && project.getModules() != null && !aggregate) {
            for (String module : project.getModules()) {
                submodulesExcludes.add(module + "/**");
            }
        }
        return HawkEyeConfig.of(configLocation).addExcludes(submodulesExcludes);
    }
}
