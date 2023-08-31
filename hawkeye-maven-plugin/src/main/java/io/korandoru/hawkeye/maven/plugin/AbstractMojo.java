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

import java.io.File;
import org.apache.maven.plugins.annotations.Parameter;

abstract class AbstractMojo extends org.apache.maven.plugin.AbstractMojo {
    /**
     * Location of the `licenserc.toml` file.
     */
    @Parameter(name = "config", alias = "cfg", defaultValue = "${maven.multiModuleProjectDirectory}/licenserc.toml")
    public File config;

    /**
     * Whether to do the real formatting or removal.
     */
    @Parameter(name = "dryRun", defaultValue = "false")
    public boolean dryRun;
}
