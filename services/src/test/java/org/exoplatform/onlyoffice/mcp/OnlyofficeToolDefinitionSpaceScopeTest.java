/*
 * Copyright (C) 2003-2026 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.onlyoffice.mcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.springframework.beans.factory.ObjectProvider;

import io.meeds.mcp.server.constant.Origin;
import io.meeds.mcp.server.model.ActingIdentity;
import io.meeds.mcp.server.model.SimpleToolDefinition;
import io.meeds.mcp.server.model.SpaceScopeDeclaration;
import io.meeds.mcp.server.model.SpaceScopeRefusedException;
import io.meeds.mcp.server.plugin.SpaceScopeResolver;
import io.meeds.mcp.server.service.McpToolSpaceScopeService;
import io.meeds.mcp.server.util.McpToolUtils;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Pins the {@code space_scope} declarations the ONLYOFFICE add-on ships: which
 * office tools a call limited to one space may use, and how.
 * <p>
 * Every tool reaches a document by its node id, declared as an object argument
 * of the {@code document} resolver in the {@code hex32} format. A write takes
 * its operations (or its target format, or its placeholder values) and nothing
 * that names another place: the create mode, the destination folder and the
 * chat attachment are not accepted, so the result lands next to the document it
 * comes from. The images an {@code add_image} operation names live inside the
 * opaque {@code operations} string and are checked by the tool itself.
 */
public class OnlyofficeToolDefinitionSpaceScopeTest {

  /** An argument that names another object or place. */
  private static final Pattern                   REFERENCE_SHAPED = Pattern.compile("(^|_)ids?$|[a-z]Ids?$|(?i:folder|space)");

  /** A document node id of the call's space. */
  private static final String                    NODE_ID          = "be0688cd7f00010134384ab7e1b15a48";

  /** A document node id of another space. */
  private static final String                    OTHER_NODE_ID    = "00000000000000000000000000000001";

  /** The call's space. */
  private static final long                      SPACE_ID         = 12L;

  /** Reads by node id: the id's argument. */
  private static final Map<String, List<String>> READS            = Map.of("get_document_editor_link", List.of("document_id"),
                                                                           "get_spreadsheet_cells", List.of("document_id", "sheet", "range"));

  /** Writes by node id: the id's argument first, then the other arguments a call may carry. */
  private static final Map<String, List<String>> WRITES           = Map.of("edit_document",
                                                                           List.of("document_id", "operations"),
                                                                           "edit_presentation",
                                                                           List.of("document_id", "operations"),
                                                                           "edit_spreadsheet",
                                                                           List.of("document_id", "operations"),
                                                                           "convert_document",
                                                                           List.of("document_id", "target_format"),
                                                                           "export_document_as_pdf",
                                                                           List.of("document_id"),
                                                                           "generate_document",
                                                                           List.of("template_document_id", "name", "data"));

  /**
   * Exactly the listed tools declare a space scope, each in the expected
   * shape: object kind, {@code document} resolver, {@code hex32}, well formed
   * for its kind, a write with no reference-shaped argument but its object.
   */
  @Test
  public void exactlyTheListedToolsAreDeclaredInTheExpectedShape() {
    Map<String, SimpleToolDefinition> all = shippedTools();
    Set<String> declared = all.entrySet()
                              .stream()
                              .filter(e -> e.getValue().getSpaceScope() != null)
                              .map(Map.Entry::getKey)
                              .collect(Collectors.toSet());
    Set<String> expected = new HashSet<>(READS.keySet());
    expected.addAll(WRITES.keySet());
    assertEquals(expected, declared);
    assertEquals(expected, all.keySet());
    READS.forEach((name, args) -> assertObjectDeclaration(all.get(name), name, args, false));
    WRITES.forEach((name, args) -> assertObjectDeclaration(all.get(name), name, args, true));
  }

  /**
   * Through the real space scope service and the shipped definitions, with a
   * resolver that puts one node in the call's space and another elsewhere: a
   * read runs for a node of the space, a write for an approved proposal only,
   * and a node of another space, an uppercase or padded id, a missing id, a
   * destination folder and an argument the declaration doesn't list are
   * refused.
   */
  @Test
  public void aCallLimitedToASpaceReachesTheDocumentsOfThatSpaceOnly() {
    Map<String, SimpleToolDefinition> all = shippedTools();
    McpToolSpaceScopeService service = new McpToolSpaceScopeService(providerOf(resolver()));
    ActingIdentity reader = ActingIdentity.agent("agent-docs", "DOCS", Origin.MENTION, SPACE_ID);
    ActingIdentity proposal = ActingIdentity.agent("agent-docs", "DOCS", Origin.PROPOSAL, SPACE_ID);

    READS.forEach((name, args) -> checkTool(service, all.get(name), name, args, reader));
    WRITES.forEach((name, args) -> checkTool(service, all.get(name), name, args, proposal));
    WRITES.forEach((name, args) -> {
      String input = inputOf(args.get(0), NODE_ID);
      assertThrows(name, SpaceScopeRefusedException.class, () -> service.enforce(name, all.get(name), input, reader));
    });
    for (String name : List.of("edit_document", "edit_presentation", "edit_spreadsheet")) {
      assertThrows(name, SpaceScopeRefusedException.class,
                   () -> service.enforce(name, all.get(name), "{\"operations\":\"[]\"}", proposal));
      assertThrows(name, SpaceScopeRefusedException.class,
                   () -> service.enforce(name,
                                         all.get(name),
                                         "{\"parent_folder_id\":\"%s\",\"name\":\"n\",\"operations\":\"[]\"}".formatted(NODE_ID),
                                         proposal));
      assertThrows(name, SpaceScopeRefusedException.class,
                   () -> service.enforce(name,
                                         all.get(name),
                                         "{\"document_id\":\"%s\",\"parent_folder_id\":\"%s\",\"operations\":\"[]\"}"
                                                                                                                        .formatted(NODE_ID,
                                                                                                                                   NODE_ID),
                                         proposal));
    }
    for (String name : List.of("convert_document", "export_document_as_pdf", "generate_document")) {
      String idArg = WRITES.get(name).get(0);
      String folderArg = "export_document_as_pdf".equals(name) ? "parent_folder_id" : "folder_id";
      String input = "{\"%s\":\"%s\",\"%s\":\"%s\"}".formatted(idArg, NODE_ID, folderArg, NODE_ID);
      assertThrows(name, SpaceScopeRefusedException.class, () -> service.enforce(name, all.get(name), input, proposal));
    }
    assertFalse(all.isEmpty());
  }

  /**
   * Checks one declared tool against the real service.
   *
   * @param service  the real space scope service
   * @param tool     the tool's shipped definition
   * @param name     the tool name
   * @param args     the arguments the tool declares, the node id's first
   * @param identity the acting identity that may run the tool
   */
  private static void checkTool(McpToolSpaceScopeService service,
                                SimpleToolDefinition tool,
                                String name,
                                List<String> args,
                                ActingIdentity identity) {
    String idArg = args.get(0);
    String sent = inputOf(idArg, NODE_ID);
    assertEquals(name, sent, service.enforce(name, tool, sent, identity));
    for (String input : List.of(inputOf(idArg, OTHER_NODE_ID),
                                inputOf(idArg, "7"),
                                "{\"%s\":7}".formatted(idArg),
                                inputOf(idArg, NODE_ID.toUpperCase()),
                                inputOf(idArg, " " + NODE_ID),
                                inputOf(idArg, "00000000000000000000000000000002"),
                                "{}")) {
      assertThrows(name + " " + input, SpaceScopeRefusedException.class, () -> service.enforce(name, tool, input, identity));
    }
    String withExtra = "{\"%s\":\"%s\",\"base64\":\"AAAA\"}".formatted(idArg, NODE_ID);
    assertThrows(name, SpaceScopeRefusedException.class, () -> service.enforce(name, tool, withExtra, identity));
    for (String other : args.subList(1, args.size())) {
      String withOther = "{\"%s\":\"%s\",\"%s\":\"x\"}".formatted(idArg, NODE_ID, other);
      assertEquals(name, withOther, service.enforce(name, tool, withOther, identity));
    }
  }

  /**
   * @param arg the argument name
   * @param id  its value
   * @return the call's arguments
   */
  private static String inputOf(String arg, String id) {
    return "{\"%s\":\"%s\"}".formatted(arg, id);
  }

  /**
   * Checks the shape of a by-id declaration against the tool's own schema.
   *
   * @param tool  the tool's definition
   * @param name  the tool name
   * @param args  the arguments the tool declares, the node id's first
   * @param write true when the tool is a write
   */
  private static void assertObjectDeclaration(SimpleToolDefinition tool, String name, List<String> args, boolean write) {
    assertNotNull(name, tool);
    SpaceScopeDeclaration scope = tool.getSpaceScope();
    assertEquals(name, write, !McpToolSpaceScopeService.isReadTool(tool));
    if (write) {
      assertTrue(name, tool.isRequireApproval());
      assertNotNull(name, tool.getAnnotations());
      assertEquals(name, Boolean.FALSE, tool.getAnnotations().readOnlyHint());
    }
    assertTrue(name, scope.isWellFormedFor(write));
    assertTrue(name, scope.isObjectKind());
    assertTrue(name, scope.isHexId());
    assertEquals(name, "hex32", scope.getIdFormat());
    assertEquals(name, "document", scope.getResolver());
    assertEquals(name, args.get(0), scope.getObjectArg());
    assertEquals(name, args.subList(1, args.size()), scope.getAllowedArgs() == null ? List.of() : scope.getAllowedArgs());
    assertTrue(name, scope.getFalseOnlyArgs() == null || scope.getFalseOnlyArgs().isEmpty());
    assertTrue(name, scope.getWhenAbsent() == null);
    assertFalse(name, scope.isSpaceAdmin());
    JsonNode schema = JsonMapper.builder().build().readTree(tool.getInputSchema());
    Set<String> properties = new HashSet<>();
    schema.path("properties").propertyNames().forEach(properties::add);
    assertTrue(name + " " + properties, properties.containsAll(args));
    if (write) {
      for (String allowed : scope.getAllowedArgs() == null ? List.<String> of() : scope.getAllowedArgs()) {
        assertFalse(name + " " + allowed, REFERENCE_SHAPED.matcher(allowed).find());
      }
    } else {
      assertEquals(name, properties, new HashSet<>(args));
    }
    assertFalse(name, args.stream().skip(1).anyMatch(p -> p.startsWith("attachment_object") || p.equals("url")));
  }

  /**
   * @return a resolver putting {@code NODE_ID} in the call's space and
   *         {@code OTHER_NODE_ID} in another one
   */
  private static SpaceScopeResolver resolver() {
    SpaceScopeResolver resolver = mock(SpaceScopeResolver.class);
    when(resolver.getName()).thenReturn("document");
    when(resolver.spaceOf(NODE_ID)).thenReturn(SPACE_ID);
    when(resolver.spaceOf(OTHER_NODE_ID)).thenReturn(13L);
    return resolver;
  }

  /**
   * @param resolver the only contributed resolver
   * @return the provider of the contributed resolvers
   */
  @SuppressWarnings("unchecked")
  private static ObjectProvider<SpaceScopeResolver> providerOf(SpaceScopeResolver resolver) {
    ObjectProvider<SpaceScopeResolver> provider = mock(ObjectProvider.class);
    when(provider.orderedStream()).thenAnswer(invocation -> Stream.of(resolver));
    return provider;
  }

  /**
   * @return every shipped office tool definition, by name
   */
  private static Map<String, SimpleToolDefinition> shippedTools() {
    try {
      URL classes = OnlyofficeMcpTool.class.getProtectionDomain().getCodeSource().getLocation();
      URL url = new File(new File(classes.toURI()), "ai-tool-definitions.json").toURI().toURL();
      List<SimpleToolDefinition> tools = McpToolUtils.parseToolDefinitions(url);
      return tools.stream().collect(Collectors.toMap(SimpleToolDefinition::getName, tool -> tool));
    } catch (Exception e) {
      throw new IllegalStateException("The office tool definitions couldn't be read", e);
    }
  }

}
