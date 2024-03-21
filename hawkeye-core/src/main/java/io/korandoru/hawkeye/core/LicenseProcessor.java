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

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import io.korandoru.hawkeye.core.config.HawkEyeConfig;
import io.korandoru.hawkeye.core.config.HeaderStylesModel;
import io.korandoru.hawkeye.core.document.Document;
import io.korandoru.hawkeye.core.document.DocumentFactory;
import io.korandoru.hawkeye.core.document.DocumentPropertiesLoader;
import io.korandoru.hawkeye.core.document.DocumentType;
import io.korandoru.hawkeye.core.header.Header;
import io.korandoru.hawkeye.core.header.HeaderDefinition;
import io.korandoru.hawkeye.core.header.HeaderType;
import io.korandoru.hawkeye.core.mapping.Mapping;
import io.korandoru.hawkeye.core.report.Report;
import io.korandoru.hawkeye.core.report.ReportConstants;
import io.korandoru.hawkeye.core.resource.HeaderSource;
import io.korandoru.hawkeye.core.resource.ResourceFinder;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor
public abstract class LicenseProcessor implements Callable<Report> {

    protected final HawkEyeConfig config;
    private final String action;

    @SneakyThrows
    @Override
    public Report call() {
        final Report report = new Report(action);

        final Path baseDir = config.getBaseDir();
        if (!Files.isDirectory(baseDir)) {
            throw new IOException(baseDir + " does not exist or is not a directory.");
        }

        final ResourceFinder resourceFinder = new ResourceFinder(baseDir);
        resourceFinder.setPluginClassPath(getClass().getClassLoader());

        final HeaderSource headerSource =
                HeaderSource.of(config.getInlineHeader(), config.getHeaderPath(), resourceFinder);
        final Header header = new Header(headerSource);
        final Selection selection = new Selection(
                baseDir.toFile(),
                config.getIncludes().toArray(new String[0]),
                config.getExcludes().toArray(new String[0]),
                config.isUseDefaultExcludes());
        final Set<String> selectedFiles =
                Arrays.stream(selection.getSelectedFiles()).collect(Collectors.toSet());
        final OldGitHelper oldGitHelper = OldGitHelper.create(baseDir, config.getGit());
        if (oldGitHelper != null) {
            oldGitHelper.filterIgnoredFiles(selectedFiles);
        }

        final Set<Mapping> mapping = new HashSet<>(config.getMapping());
        if (config.isUseDefaultMapping()) {
            mapping.addAll(DocumentType.defaultMapping());
        }

        final DocumentPropertiesLoader propertiesLoader = document -> {
            final Map<String, String> properties = new LinkedHashMap<>(config.getProperties());
            properties.put("hawkeye.core.filename", document.getFile().getName());
            return properties;
        };

        final Map<String, HeaderDefinition> definitionMap = new HashMap<>(HeaderType.defaultDefinitions());
        final TomlMapper mapper = new TomlMapper();
        for (String additionalHeader : config.getAdditionalHeaders()) {
            final URL source = resourceFinder.findResource(additionalHeader);
            final HeaderStylesModel stylesModel = mapper.readValue(source, HeaderStylesModel.class);
            definitionMap.putAll(stylesModel.toHeaderDefinitions());
        }
        // force inclusion of unknown item to manage unknown files
        definitionMap.put(HeaderType.UNKNOWN.getDefinition().getType(), HeaderType.UNKNOWN.getDefinition());

        final DocumentFactory documentFactory = new DocumentFactory(
                baseDir.toFile(),
                mapping,
                definitionMap,
                StandardCharsets.UTF_8,
                config.getKeywords().toArray(new String[0]),
                propertiesLoader);

        for (final String file : selectedFiles) {
            final Document document = documentFactory.createDocuments(file);
            if (document.is(header)) {
                continue;
            }

            if (document.isNotSupported()) {
                report.add(document.getFile(), ReportConstants.RESULT_UNKNOWN);
            } else if (document.hasHeader(header, config.isStrictCheck())) {
                onExistingHeader(document, header, report);
            } else {
                onHeaderNotFound(document, header, report);
            }
        }

        return report;
    }

    protected abstract void onHeaderNotFound(Document document, Header header, Report report);

    protected abstract void onExistingHeader(Document document, Header header, Report report);
}
