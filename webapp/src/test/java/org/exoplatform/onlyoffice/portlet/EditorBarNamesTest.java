/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <gnu.org/licenses>.
 */
package org.exoplatform.onlyoffice.portlet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class EditorBarNamesTest {

  private static final Path    EDITOR_SCRIPT     = Path.of("src/main/webapp/js/onlyoffice.js");

  private static final Path    EDIT_IN_PLACE_SCRIPT = Path.of("src/main/webapp/js/eip.js");

  // markup concatenated with a value, whatever its name and quotes: "<span>" + value, or value + '</span>'
  private static final Pattern MARKUP_BEFORE_VALUE = Pattern.compile("(\"[^\"]*<[^\"]*\"|'[^']*<[^']*'|`[^`]*<[^`]*`)\\s*\\+\\s*[A-Za-z_$]");

  private static final Pattern VALUE_BEFORE_MARKUP = Pattern.compile("[A-Za-z_$][\\w$.]*\\s*\\+\\s*(\"[^\"]*<|'[^']*<|`[^`]*<)");

  // a template literal holding both markup and a value
  private static final Pattern MARKUP_TEMPLATE     = Pattern.compile("`[^`]*(<[^`]*\\$\\{|\\$\\{[^`]*<)");

  // a value parsed as markup
  private static final Pattern VALUE_AS_MARKUP     = Pattern.compile("\\.html\\(\\s*[A-Za-z_$]");

  @Test
  void theEditorBarShowsTheDocumentAndFolderNamesAsText() throws Exception {
    String initBar = initBar();
    assertFalse(MARKUP_BEFORE_VALUE.matcher(initBar).find(), "a value is concatenated into the editor bar's markup");
    assertFalse(VALUE_BEFORE_MARKUP.matcher(initBar).find(), "a value is concatenated into the editor bar's markup");
    assertFalse(VALUE_AS_MARKUP.matcher(initBar).find(), "a value is parsed as the editor bar's markup");
    assertFalse(MARKUP_TEMPLATE.matcher(initBar).find(), "a value is written into the editor bar's markup");
    // intentional source pins: the title and each folder are written as text
    assertTrue(initBar.contains(".text(title + \" \")"));
    assertTrue(initBar.contains("document.createTextNode(folder)"));
  }

  @Test
  void theTitleEditingFieldTakesTheTitleAsItsValue() throws Exception {
    String editInPlace = Files.readString(EDIT_IN_PLACE_SCRIPT, StandardCharsets.UTF_8);
    assertFalse(editInPlace.contains("${documentTitle}"), "the title is written into the field's markup");
    assertTrue(editInPlace.contains("input.value = documentTitle;"));
  }

  private static String initBar() throws Exception {
    String script = Files.readString(EDITOR_SCRIPT, StandardCharsets.UTF_8);
    int start = script.indexOf("this.initBar = function(config) {");
    int end = script.indexOf("this.loadVersions = function", start);
    assertTrue(start >= 0 && end > start);
    return script.substring(start, end);
  }

}
