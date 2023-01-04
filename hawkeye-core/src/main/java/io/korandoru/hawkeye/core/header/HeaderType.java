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

package io.korandoru.hawkeye.core.header;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Defines the default header definitions available out of the box.
 */
public enum HeaderType {
    ASCIIDOC_STYLE("////", "  // ", "////EOL", "", null, "^////$", "^////$", false, true, false),
    MVEL_STYLE("@comment{", "  ", "}", "", null, "@comment\\{$", "\\}$", true, true, false),
    JAVADOC_STYLE("/**", " * ", " */", "", null, "(\\s|\\t)*/\\*.*$", ".*\\*/(\\s|\\t)*$", false, true, false),
    SCALA_STYLE("/**", "  * ", "  */", "", null, "(\\s|\\t)*/\\*.*$", ".*\\*/(\\s|\\t)*$", false, true, false),
    JAVAPKG_STYLE("EOL/*-", " * ", " */", "", "^package [a-z_]+(\\.[a-z_][a-z0-9_]*)*;$", "(EOL)*(\\s|\\t)*/\\*.*$", ".*\\*/(\\s|\\t)*$", false, true, false),
    SCRIPT_STYLE("#", "# ", "#EOL", "", "^#!.*$", "#.*$", "#.*$", false, false, false),
    HAML_STYLE("-#", "-# ", "-#EOL", "", "^-#!.*$", "-#.*$", "-#.*$", false, false, false),
    XML_STYLE("<!--EOL", "    ", "EOL-->", "", "^<\\?xml.*>$", "(\\s|\\t)*<!--.*$", ".*-->(\\s|\\t)*$", true, true, false),
    XML_PER_LINE("EOL", "<!-- ", "EOL", " -->", "^<\\?xml.*>$", "(\\s|\\t)*<!--.*$", ".*-->(\\s|\\t)*$", false, false, true),
    SEMICOLON_STYLE(";", "; ", ";EOL", "", null, ";.*$", ";.*$", false, false, false),
    APOSTROPHE_STYLE("'", "' ", "'EOL", "", null, "'.*$", "'.*$", false, false, false),
    EXCLAMATION_STYLE("!", "! ", "!EOL", "", null, "!.*$", "!.*$", false, false, false),
    DOUBLEDASHES_STYLE("--", "-- ", "--EOL", "", null, "--.*$", "--.*$", false, false, false),
    SLASHSTAR_STYLE("/*", " * ", " */", "", null, "(\\s|\\t)*/\\*.*$", ".*\\*/(\\s|\\t)*$", false, true, false),
    BRACESSTAR_STYLE("{*", " * ", " *}", "", null, "(\\s|\\t)*\\{\\*.*$", ".*\\*\\}(\\s|\\t)*$", false, true, false),
    SHARPSTAR_STYLE("#*", " * ", " *#", "", null, "(\\s|\\t)*#\\*.*$", ".*\\*#(\\s|\\t)*$", false, true, false),
    DOUBLETILDE_STYLE("~~", "~~ ", "~~EOL", "", null, "~~.*$", "~~.*$", false, false, false),
    DYNASCRIPT_STYLE("<%--EOL", "    ", "EOL--%>", "", null, "(\\s|\\t)*<%--.*$", ".*--%>(\\s|\\t)*$", true, true, false),
    DYNASCRIPT3_STYLE("<!---EOL", "    ", "EOL--->", "", null, "(\\s|\\t)*<!---.*$", ".*--->(\\s|\\t)*$", true, true, false),
    PERCENT_STYLE("", "% ", "EOL", "", null, "^% .*$", "^% .*$", false, false, false),
    PERCENT3_STYLE("%%%", "%%% ", "%%%EOL", "", null, "%%%.*$", "%%%.*$", false, false, false),
    EXCLAMATION3_STYLE("!!!", "!!! ", "!!!EOL", "", null, "!!!.*$", "!!!.*$", false, false, false),

    DOUBLESLASH_STYLE("//", "// ", "//EOL", "", null, "//.*$", "//.*$", false, false, false),
    SINGLE_LINE_DOUBLESLASH_STYLE("", "// ", "", "", null, "//.*$", "//.*$", false, false, false),
    TRIPLESLASH_STYLE("///", "/// ", "///EOL", "", null, "///.*$", "///.*$", false, false, false),
    // non generic
    PHP("/*", " * ", " */", "", "^<\\?php.*$", "(\\s|\\t)*/\\*.*$", ".*\\*/(\\s|\\t)*$", false, true, false),
    ASP("<%", "' ", "%>", "", null, "(\\s|\\t)*<%( .*)?$", ".*%>(\\s|\\t)*$", true, true, false),
    LUA("--[[EOL", "    ", "EOL]]", "", null, "--\\[\\[$", "\\]\\]$", true, true, false),
    FTL("<#--EOL", "    ", "EOL-->", "", null, "(\\s|\\t)*<#--.*$", ".*-->(\\s|\\t)*$", true, true, false),
    FTL_ALT("[#--EOL", "    ", "EOL--]", "", "\\[#ftl(\\s.*)?\\]", "(\\s|\\t)*\\[#--.*$", ".*--\\](\\s|\\t)*$", true, true, false),
    TEXT("====", "    ", "====EOL", "", null, "====.*$", "====.*$", true, true, false),
    BATCH("@REM", "@REM ", "@REMEOL", "", null, "@REM.*$", "@REM.*$", false, false, false),
    MUSTACHE_STYLE("{{!", "    ", "}}", "", null, "\\{\\{\\!.*$", "\\}\\}.*$", false, true, false),
    // unknown
    UNKNOWN("", "", "", "", null, null, null, false, false, false);

    private final HeaderDefinition definition;

    HeaderType(
            String firstLine,
            String beforeEachLine,
            String endLine,
            String afterEachLine,
            String skipLinePattern,
            String firstLineDetectionPattern,
            String lastLineDetectionPattern,
            boolean allowBlankLines,
            boolean multiLine,
            boolean padLines
    ) {
        this.definition = HeaderDefinition.builder()
                .type(name())
                .firstLine(firstLine)
                .endLine(endLine)
                .beforeEachLine(beforeEachLine)
                .afterEachLine(afterEachLine)
                .skipLinePattern(Optional.ofNullable(skipLinePattern).map(Pattern::compile).orElse(null))
                .firstLineDetectionPattern(Optional.ofNullable(firstLineDetectionPattern).map(Pattern::compile).orElse(null))
                .lastLineDetectionPattern(Optional.ofNullable(lastLineDetectionPattern).map(Pattern::compile).orElse(null))
                .allowBlankLines(allowBlankLines)
                .multipleLines(multiLine)
                .padLines(padLines)
                .build();
    }

    /**
     * Returns the {@link HeaderDefinition} which corresponds to this enumeration instance.
     *
     * @return The header definition.
     */
    public HeaderDefinition getDefinition() {
        return definition;
    }

    private static final Map<String, HeaderDefinition> DEFINITIONS = new HashMap<>(values().length);

    static {
        for (HeaderType type : values()) {
            DEFINITIONS.put(type.getDefinition().getType(), type.getDefinition());
        }
    }

    /**
     * Returns the header definitions of every default definitions declared by this enumeration as a map using the
     * header type name as key.
     *
     * @return The default definitions declared by this enumeration.
     */
    public static Map<String, HeaderDefinition> defaultDefinitions() {
        return Collections.unmodifiableMap(DEFINITIONS);
    }
}
