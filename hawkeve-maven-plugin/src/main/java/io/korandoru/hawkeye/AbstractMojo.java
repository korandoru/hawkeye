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

package io.korandoru.hawkeye;

import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

abstract class AbstractMojo extends org.apache.maven.plugin.AbstractMojo {
    @Parameter(name = "config", alias = "cfg", defaultValue = "${project.basedir}/licenserc.toml")
    public File config;
    @Parameter(name = "dryRun", defaultValue = "false")
    public boolean dryRun;
}
