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

package io.korandoru.hawkeye.core.document;

import io.korandoru.hawkeye.core.header.HeaderType;
import io.korandoru.hawkeye.core.mapping.Mapping;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public enum DocumentType {
    ACTIONSCRIPT("as", HeaderType.JAVADOC_STYLE, true, false),
    ADA_BODY("adb", HeaderType.DOUBLEDASHES_STYLE, true, false),
    ADA_SPEC("ads", HeaderType.DOUBLEDASHES_STYLE, true, false),
    ASCII_DOC("adoc", HeaderType.ASCIIDOC_STYLE, true, false),
    ASP("asp", HeaderType.ASP, true, false),
    ASPECTJ("aj", HeaderType.JAVADOC_STYLE, true, false),
    ASSEMBLER("asm", HeaderType.SEMICOLON_STYLE, true, false),
    C("c", HeaderType.JAVADOC_STYLE, true, false),
    CC("cc", HeaderType.JAVADOC_STYLE, true, false),
    CLOJURE("clj", HeaderType.SEMICOLON_STYLE, true, false),
    CLOJURESCRIPT("cljs", HeaderType.SEMICOLON_STYLE, true, false),
    COLDFUSION_COMPONENT("cfc", HeaderType.DYNASCRIPT3_STYLE, true, false),
    COLDFUSION_ML("cfm", HeaderType.DYNASCRIPT3_STYLE, true, false),
    CPP("cpp", HeaderType.JAVADOC_STYLE, true, false),
    CSHARP("cs", HeaderType.JAVADOC_STYLE, true, false),
    CSS("css", HeaderType.JAVADOC_STYLE, true, false),
    DELPHI("pas", HeaderType.BRACESSTAR_STYLE, true, false),
    DOCKERFILE("Dockerfile", HeaderType.SCRIPT_STYLE, false, true),
    DOXIA_APT("apt", HeaderType.DOUBLETILDE_STYLE, true, false),
    DOXIA_FAQ("fml", HeaderType.XML_STYLE, true, false),
    DTD("dtd", HeaderType.XML_STYLE, true, false),
    EDITORCONFIG(".editorconfig", HeaderType.SCRIPT_STYLE, false, true),
    EIFFEL("e", HeaderType.DOUBLEDASHES_STYLE, true, false),
    ERLANG("erl", HeaderType.PERCENT3_STYLE, true, false),
    ERLANG_HEADER("hrl", HeaderType.PERCENT3_STYLE, true, false),
    FORTRAN("f", HeaderType.EXCLAMATION_STYLE, true, false),
    FREEMARKER("ftl", HeaderType.FTL, true, false),
    GO("go", HeaderType.SLASHSTAR_STYLE, true, false),
    GROOVY("groovy", HeaderType.SLASHSTAR_STYLE, true, false),
    GSP("GSP", HeaderType.XML_STYLE, true, false),
    H("h", HeaderType.JAVADOC_STYLE, true, false),
    HAML("haml", HeaderType.HAML_STYLE, true, false),
    HTM("htm", HeaderType.XML_STYLE, true, false),
    HTML("html", HeaderType.XML_STYLE, true, false),
    JAVA("java", HeaderType.SLASHSTAR_STYLE, true, false),
    JAVAFX("fx", HeaderType.SLASHSTAR_STYLE, true, false),
    JAVASCRIPT("js", HeaderType.SLASHSTAR_STYLE, true, false),
    JSP("jsp", HeaderType.DYNASCRIPT_STYLE, true, false),
    JSPX("jspx", HeaderType.XML_STYLE, true, false),
    KML("kml", HeaderType.XML_STYLE, true, false),
    KOTLIN("kt", HeaderType.SLASHSTAR_STYLE, true, false),
    LISP("el", HeaderType.EXCLAMATION3_STYLE, true, false),
    LUA("lua", HeaderType.LUA, true, false),
    MUSTACHE("mustache", HeaderType.MUSTACHE_STYLE, true, false),
    MVEL("mv", HeaderType.MVEL_STYLE, true, false),
    MXML("mxml", HeaderType.XML_STYLE, true, false),
    PERL("pl", HeaderType.SCRIPT_STYLE, true, false),
    PERL_MODULE("pm", HeaderType.SCRIPT_STYLE, true, false),
    PHP("php", HeaderType.PHP, true, false),
    POM("pom", HeaderType.XML_STYLE, true, false),
    PROPERTIES("properties", HeaderType.SCRIPT_STYLE, true, false),
    PYTHON("py", HeaderType.SCRIPT_STYLE, true, false),
    RUBY("rb", HeaderType.SCRIPT_STYLE, true, false),
    RUST("rs", HeaderType.DOUBLESLASH_STYLE, true, false),
    SCALA("scala", HeaderType.SLASHSTAR_STYLE, true, false),
    SCAML("scaml", HeaderType.HAML_STYLE, true, false),
    SCSS("scss", HeaderType.JAVADOC_STYLE, true, false),
    SHELL("sh", HeaderType.SCRIPT_STYLE, true, false),
    SPRING_FACTORIES("spring.factories", HeaderType.SCRIPT_STYLE, false, false),
    SQL("sql", HeaderType.DOUBLEDASHES_STYLE, true, false),
    TAGX("tagx", HeaderType.XML_STYLE, true, false),
    TEX_CLASS("cls", HeaderType.PERCENT_STYLE, true, false),
    TEX_STYLE("sty", HeaderType.PERCENT_STYLE, true, false),
    TEX("tex", HeaderType.PERCENT_STYLE, true, false),
    TLD("tld", HeaderType.XML_STYLE, true, false),
    TOML("toml", HeaderType.SCRIPT_STYLE, true, false),
    TS("ts", HeaderType.SLASHSTAR_STYLE, true, false),
    TXT("txt", HeaderType.TEXT, true, false),
    UNKNOWN("", HeaderType.UNKNOWN, true, true),
    VB("bas", HeaderType.HAML_STYLE, true, false),
    VBA("vba", HeaderType.APOSTROPHE_STYLE, true, false),
    VELOCITY("vm", HeaderType.SHARPSTAR_STYLE, true, false),
    WINDOWS_BATCH("bat", HeaderType.BATCH, true, false),
    WINDOWS_SHELL("cmd", HeaderType.BATCH, true, false),
    WSDL("wsdl", HeaderType.XML_STYLE, true, false),
    XHTML("xhtml", HeaderType.XML_STYLE, true, false),
    XML("xml", HeaderType.XML_STYLE, true, false),
    XSD("xsd", HeaderType.XML_STYLE, true, false),
    XSL("xsl", HeaderType.XML_STYLE, true, false),
    XSLT("xslt", HeaderType.XML_STYLE, true, false),
    YAML("yaml", HeaderType.SCRIPT_STYLE, true, false),
    YML("yml", HeaderType.SCRIPT_STYLE, true, false),
    ;
    private static final Set<Mapping> MAPPINGS = new HashSet<>();

    static {
        for (DocumentType type : values()) {
            final String headerType = type.defaultHeaderType.name().toLowerCase();
            if (type.extension) {
                MAPPINGS.add(new Mapping.Extension(type.patten, headerType));
            }
            if (type.filename) {
                MAPPINGS.add(new Mapping.Filename(type.patten, headerType));
            }
        }
    }

    private final String patten;
    private final HeaderType defaultHeaderType;
    private final boolean extension;
    private final boolean filename;


    DocumentType(String patten, HeaderType defaultHeaderType, boolean extension, boolean filename) {
        this.patten = patten;
        this.defaultHeaderType = defaultHeaderType;
        this.extension = extension;
        this.filename = filename;
    }

    public HeaderType getDefaultHeaderType() {
        return defaultHeaderType;
    }

    public static Set<Mapping> defaultMapping() {
        return Collections.unmodifiableSet(MAPPINGS);
    }
}
