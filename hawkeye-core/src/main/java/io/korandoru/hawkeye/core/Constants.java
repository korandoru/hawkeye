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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {
    public static final String UNKNOWN = "<unknown>";
    public static final String VERSION;

    static {
        ClassLoader classLoader = Constants.class.getClassLoader();
        try (InputStream is = classLoader.getResourceAsStream("hawkeye.properties")) {
            final Properties properties = new Properties();
            properties.load(is);
            VERSION = properties.getProperty("project.version", UNKNOWN);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot load hawkeye properties file: hawkeye.properties", e);
        } catch (Exception e) {
            throw new UncheckedIOException("cannot load hawkeye properties file: hawkeye.properties", new IOException(e));
        }
    }
}
