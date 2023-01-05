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

package io.korandoru.hawkeye.core;

import io.korandoru.hawkeye.core.document.Document;
import io.korandoru.hawkeye.core.document.DocumentFactory;
import io.korandoru.hawkeye.core.document.DocumentType;
import io.korandoru.hawkeye.core.header.Header;
import io.korandoru.hawkeye.core.header.HeaderType;
import io.korandoru.hawkeye.core.resource.HeaderSource;
import io.korandoru.hawkeye.core.resource.ResourceFinder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class LicenseProcessor implements Callable<Report> {

    private final HawkEyeConfig config;
    private final Report.Action action;

    @Override
    public Report call() {
        final Report report = new Report(action);

        final Path baseDir = Path.of(config.getBaseDir());
        final ResourceFinder resourceFinder = new ResourceFinder(baseDir);
        resourceFinder.setPluginClassPath(getClass().getClassLoader());

        final HeaderSource headerSource = HeaderSource.of(
                config.getInlineHeader(),
                config.getHeaderPath(),
                resourceFinder);
        final Header header = new Header(headerSource);
        final Selection selection = new Selection(
                baseDir.toFile(),
                config.getIncludes().toArray(new String[0]),
                config.getExcludes().toArray(new String[0]),
                config.isUseDefaultExcludes());
        final String[] selectedFiles = selection.getSelectedFiles();

        final Map<String, String> mapping = new LinkedHashMap<>();
        if (config.isUseDefaultMapping()) {
            mapping.putAll(DocumentType.defaultMapping());
        }
        mapping.putAll(config.getMapping());

        final DocumentFactory documentFactory = new DocumentFactory(
                baseDir.toFile(),
                mapping,
                HeaderType.defaultDefinitions(),
                StandardCharsets.UTF_8,
                config.getKeywords().toArray(new String[0]),
                d -> {
                    final Map<String, String> perDoc = new LinkedHashMap<>(config.getProperties());
                    perDoc.put("file.name", d.getFile().getName());
                    return perDoc;
                });

        for (final String file : selectedFiles) {
            final Document document = documentFactory.createDocuments(file);
            if (document.is(header)) {
                continue;
            }

            if (document.isNotSupported()) {
                report.add(document.getFile(), Report.Result.UNKNOWN);
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
