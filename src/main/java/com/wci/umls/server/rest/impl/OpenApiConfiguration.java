/*
 *    Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.rest.impl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

/**
 * OpenAPI metadata for Springdoc.
 */
@Configuration
public class OpenApiConfiguration {

  /** The authorization security scheme name. */
  private static final String AUTHORIZATION_HEADER = "authorizationHeader";

  /** The OpenAPI schema reference for PFS request bodies. */
  private static final String PFS_PARAMETER_REF =
      "#/components/schemas/PfsParameterJpa";

  /** The OpenAPI schema reference for the PFS interface request bodies. */
  private static final String PFS_PARAMETER_INTERFACE_REF =
      "#/components/schemas/PfsParameter";

  /** Operation ids that accept simple note bodies. */
  private static final Set<String> NOTE_BODY_OPERATION_IDS = Set.of(
      "addConceptNote", "addAtomNote", "addCodeNote", "addDescriptorNote",
      "addChecklistNote", "addWorklistNote");

  /** Operation ids that support the "name" PFS sort example. */
  private static final Set<String> NAME_SORT_OPERATION_IDS =
      Set.of("findProjects", "findAssignedUsersForProject",
          "findUnassignedUsersForProject", "findUsers", "findReports",
          "findSourceDataFiles", "findSourceData");

  /** Operation ids that accept either a Lucene query or a JPQL select query. */
  private static final Set<String> GENERAL_QUERY_OPERATION_IDS =
      Set.of("findConceptsForGeneralQuery", "findCodesForGeneralQuery",
          "findDescriptorsForGeneralQuery");

  /** OpenAPI paths that should not appear in interactive Swagger docs. */
  private static final Set<String> HIDDEN_PATHS = Set.of("/project/exception",
      "/configure/destroy", "/configure/configure", "/process/testquery");

  /** Operation ids that are real workflows but risky from Swagger UI. */
  private static final Set<String> USE_WITH_CARE_OPERATION_IDS = Set.of(
      "reloadConfigProperties", "luceneReindex",
      "computeExpressionIndexes", "loadTerminologySimple",
      "loadTerminologyRrf", "loadTerminologyRf2Delta",
      "loadTerminologyRf2Snapshot", "loadTerminologyRf2Full",
      "loadTerminologyClaml", "loadTerminologyOwl", "removeTerminology",
      "loadFromSourceData", "removeFromSourceData", "cancelFromSourceData",
      "prepareProcess", "executeProcess", "restartProcess", "stepProcess",
      "cancelProcess", "computeChecklist", "stampWorklist",
      "stampChecklist", "recomputeConceptStatus", "runAutofix");

  /** The tag for administrative workflows that should be used with care. */
  private static final String ADMIN_USE_WITH_CARE_TAG =
      "Admin / Use with care";

  /** Warning text for risky interactive operations. */
  private static final String USE_WITH_CARE_WARNING =
      "Use with care: this operation can modify data, start or cancel "
          + "background work, or rebuild indexes. Prefer the application UI "
          + "or a known runbook unless you are intentionally testing this "
          + "workflow.";

  /**
   * Returns the OpenAPI metadata.
   *
   * @param deployTitle the deployment title
   * @param feedbackEmail the feedback email
   * @param deployLink the deployment link
   * @param contextPath the servlet context path
   * @return the OpenAPI metadata
   */
  @Bean
  public OpenAPI termServerOpenApi(
    @Value("${deploy.title:NCI-META Terminology Maintenance}") String deployTitle,
    @Value("${deploy.feedback.email:info@westcoastinformatics.com}") String feedbackEmail,
    @Value("${deploy.link:https://www.westcoastinformatics.com}") String deployLink,
    @Value("${server.servlet.context-path:/}") String contextPath) {

    return new OpenAPI()
        .info(new Info().title(deployTitle + " API").version("1.0.1")
            .description(
                "API documentation for the " + deployTitle + " REST services.")
            .contact(new Contact().name("API Support").url(deployLink)
                .email(feedbackEmail)))
        .addServersItem(new Server().description("Current Instance")
            .url(normalizeContextPath(contextPath)))
        .components(new Components().addSecuritySchemes(AUTHORIZATION_HEADER,
            new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER).name("Authorization")
                .description(
                    "Auth token passed in the Authorization header")))
        .addSecurityItem(new SecurityRequirement().addList(AUTHORIZATION_HEADER));
  }

  /**
   * Adjusts generated OpenAPI operations for the legacy Authorization header.
   *
   * @return the OpenAPI customizer
   */
  @Bean
  public OpenApiCustomizer termServerOperationCustomizer() {
    return openApi -> {
      if (openApi.getPaths() != null) {
        removeHiddenPaths(openApi);
        openApi.getPaths().forEach((path, pathItem) -> pathItem.readOperations()
            .forEach(operation -> {
              customizeVisibility(path, operation);
              customizeAuthorization(path, operation);
              customizeParameterExamples(path, operation);
              customizePfsRequestBody(operation);
              customizeCommonRequestBody(path, operation);
            }));
      }
      customizeTags(openApi);
      customizePfsSchema(openApi);
    };
  }

  /**
   * Removes paths that are not intended for interactive API use.
   *
   * @param openApi the OpenAPI model
   */
  private void removeHiddenPaths(OpenAPI openApi) {
    openApi.getPaths().entrySet()
        .removeIf(entry -> isHiddenPath(entry.getKey()));
  }

  /**
   * Indicates whether the path should be hidden from Swagger.
   *
   * @param path the OpenAPI path
   * @return true if the path should be hidden
   */
  private boolean isHiddenPath(String path) {
    return path.startsWith("/test") || HIDDEN_PATHS.contains(path);
  }

  /**
   * Applies visibility warnings/grouping to operations that are risky from
   * Swagger UI.
   *
   * @param path the OpenAPI path
   * @param operation the operation
   */
  private void customizeVisibility(String path, Operation operation) {
    if (!isUseWithCareOperation(operation)) {
      return;
    }
    operation.setTags(Collections.singletonList(ADMIN_USE_WITH_CARE_TAG));
    operation.setSummary(withPrefix(operation.getSummary(), "Use with care: "));
    operation.setDescription(withWarning(operation.getDescription()));
    operation.addExtension("x-use-with-care", true);
  }

  /**
   * Indicates whether the operation is risky from interactive Swagger.
   *
   * @param operation the operation
   * @return true if the operation should be grouped as use-with-care
   */
  private boolean isUseWithCareOperation(Operation operation) {
    return operation.getOperationId() != null
        && USE_WITH_CARE_OPERATION_IDS.contains(operation.getOperationId());
  }

  /**
   * Prefixes text if it does not already have the prefix.
   *
   * @param text the text
   * @param prefix the prefix
   * @return the prefixed text
   */
  private String withPrefix(String text, String prefix) {
    final String value = text == null ? "" : text;
    return value.startsWith(prefix) ? value : prefix + value;
  }

  /**
   * Adds the use-with-care warning to an operation description.
   *
   * @param description the description
   * @return the description with warning
   */
  private String withWarning(String description) {
    if (description != null && description.contains(USE_WITH_CARE_WARNING)) {
      return description;
    }
    if (description == null || description.isBlank()) {
      return USE_WITH_CARE_WARNING;
    }
    return USE_WITH_CARE_WARNING + "\n\n" + description;
  }

  /**
   * Defines the tag order and descriptions used by Swagger UI.
   *
   * @param openApi the OpenAPI model
   */
  private void customizeTags(OpenAPI openApi) {
    openApi.setTags(List.of(
        new Tag().name("Security")
            .description("Authentication and user security operations."),
        new Tag().name("Project")
            .description("Project metadata, roles, validation checks, and users."),
        new Tag().name("Content")
            .description("Terminology content lookup, search, and validation."),
        new Tag().name("Metadata")
            .description("Terminology metadata lookup and maintenance."),
        new Tag().name("Workflow")
            .description("Workflow, worklist, checklist, and tracking operations."),
        new Tag().name("Process")
            .description("Process configuration, execution, and logs."),
        new Tag().name("Source Data")
            .description("Source data files, uploads, and import configuration."),
        new Tag().name("Report").description("Reporting operations."),
        new Tag().name("History")
            .description("Terminology release history operations."),
        new Tag().name("Simple Edit")
            .description("Basic terminology content editing operations."),
        new Tag().name("Meta Editing")
            .description("Metathesaurus editing support operations."),
        new Tag().name("Inversion").description("Inversion support operations."),
        new Tag().name("Configure")
            .description("Read-only application configuration helpers."),
        new Tag().name(ADMIN_USE_WITH_CARE_TAG)
            .description("Administrative workflows exposed for deliberate use.")));
  }

  /**
   * Removes duplicate Authorization parameters and applies security.
   *
   * @param path the path
   * @param operation the operation
   */
  private void customizeAuthorization(String path, Operation operation) {
    if (operation.getParameters() != null) {
      operation.getParameters().removeIf(parameter -> "header".equals(parameter.getIn())
          && "Authorization".equalsIgnoreCase(parameter.getName()));
    }
    if ("/security/authenticate/{username}".equals(path)) {
      operation.setSecurity(Collections.emptyList());
      return;
    }
    operation.setSecurity(Collections.singletonList(
        new SecurityRequirement().addList(AUTHORIZATION_HEADER)));
  }

  /**
   * Applies examples for common high-use parameters when annotations did not
   * already provide one.
   *
   * @param path the OpenAPI path
   * @param operation the operation
   */
  private void customizeParameterExamples(String path, Operation operation) {
    if (operation.getParameters() == null) {
      return;
    }
    operation.getParameters().forEach(parameter -> {
      customizeGeneralQueryParameter(operation, parameter);
      if (parameter.getExample() != null || parameter.getExamples() != null) {
        return;
      }
      final Object example = parameterExample(path, operation, parameter);
      if (example != null) {
        parameter.setExample(example);
      }
    });
  }

  /**
   * Corrects general-query parameter metadata so Swagger does not send both
   * Lucene and JPQL values by default.
   *
   * @param operation the operation
   * @param parameter the parameter
   */
  private void customizeGeneralQueryParameter(Operation operation,
    Parameter parameter) {
    if (operation.getOperationId() == null
        || !GENERAL_QUERY_OPERATION_IDS.contains(operation.getOperationId())) {
      return;
    }
    if ("JPQL".equals(parameter.getName())) {
      parameter.setRequired(false);
      parameter.setExample("");
      parameter.setDescription(
          "Optional JPQL select query. Leave blank when using Lucene query.");
    } else if ("query".equals(parameter.getName())) {
      parameter.setRequired(false);
      if (parameter.getDescription() == null
          || !parameter.getDescription().contains("Lucene")) {
        parameter.setDescription(
            "Optional Lucene query. Leave JPQL blank when using this.");
      }
    }
  }

  /**
   * Returns an example value for common high-use parameters.
   *
   * @param path the OpenAPI path
   * @param operation the operation
   * @param parameter the parameter
   * @return the example, or null
   */
  private Object parameterExample(String path, Operation operation,
    Parameter parameter) {
    final String name = parameter.getName();
    if ("projectId".equals(name)) {
      return 39751;
    } else if ("terminology".equals(name)) {
      return terminologyExample(path);
    } else if ("version".equals(name)) {
      return versionExample(path);
    } else if ("process".equals(name)) {
      return "BETA";
    } else if ("query".equals(name)) {
      return queryExample(operation);
    }
    return null;
  }

  /**
   * Returns an example terminology for a path.
   *
   * @param path the OpenAPI path
   * @return the example
   */
  private String terminologyExample(String path) {
    if (path.contains("/descriptor")) {
      return "MSH";
    } else if (path.contains("/code")) {
      return "SNOMEDCT_US";
    }
    return "NCIMTH";
  }

  /**
   * Returns an example version for a path.
   *
   * @param path the OpenAPI path
   * @return the example
   */
  private String versionExample(String path) {
    if (path.contains("/descriptor")) {
      return "2015_2014_09_08";
    } else if (path.contains("/code")) {
      return "2014_09_01";
    }
    return "latest";
  }

  /**
   * Returns an example query for an operation.
   *
   * @param operation the operation
   * @return the example, or null
   */
  private String queryExample(Operation operation) {
    final String operationId = operation.getOperationId();
    if ("findProjects".equals(operationId)) {
      return "id:[* TO *]";
    } else if ("findAssignedUsersForProject".equals(operationId)
        || "findUnassignedUsersForProject".equals(operationId)
        || "findUsers".equals(operationId)) {
      return "userName:DSS";
    } else if ("findReports".equals(operationId)) {
      return "name:*";
    } else if ("findSourceDataFiles".equals(operationId)
        || "findSourceData".equals(operationId)) {
      return "SNOMEDCT";
    } else if (operationId != null
        && (operationId.startsWith("findConcept")
            || operationId.startsWith("findDescriptor")
            || operationId.startsWith("findCode")
            || operationId.startsWith("getConcept"))) {
      return "aspirin";
    }
    return null;
  }

  /**
   * Applies safe, useful examples for PFS request bodies.
   *
   * @param operation the operation
   */
  private void customizePfsRequestBody(Operation operation) {
    if (operation.getRequestBody() == null
        || operation.getRequestBody().getContent() == null) {
      return;
    }
    operation.getRequestBody().getContent().values().stream()
        .filter(mediaType -> isPfsSchema(mediaType.getSchema()))
        .forEach(mediaType -> {
          mediaType.setExample(null);
          mediaType.setExamples(pfsExamples(operation));
        });
  }

  /**
   * Applies safe examples/defaults to the reusable PFS schema.
   *
   * @param openApi the OpenAPI model
   */
  @SuppressWarnings("unchecked")
  private void customizePfsSchema(OpenAPI openApi) {
    if (openApi.getComponents() == null
        || openApi.getComponents().getSchemas() == null) {
      return;
    }
    final Schema<Object> schema =
        openApi.getComponents().getSchemas().get("PfsParameterJpa");
    if (schema != null) {
      customizePfsSchemaProperties(schema);
    }
    final Schema<Object> interfaceSchema =
        openApi.getComponents().getSchemas().get("PfsParameter");
    if (interfaceSchema != null) {
      customizePfsSchemaProperties(interfaceSchema);
    }
  }

  /**
   * Applies safe examples/defaults to a PFS schema.
   *
   * @param schema the PFS schema
   */
  private void customizePfsSchemaProperties(Schema<Object> schema) {
    schema.setExample(pfsExample());
    if (schema.getProperties() == null) {
      return;
    }
    setPropertyExample(schema, "maxResults", 25);
    setPropertyExample(schema, "startIndex", 0);
    setPropertyExample(schema, "ascending", true);
    setPropertyExample(schema, "activeOnly", false);
    setPropertyExample(schema, "inactiveOnly", false);
    setPropertyExample(schema, "sortFields", Collections.emptyList());
    setPropertyExample(schema, "queryRestriction", "obsolete:false");
    setPropertyDescription(schema, "sortField",
        "Optional indexed sort field for this endpoint, such as name.");
    setPropertyDescription(schema, "queryRestriction",
        "Optional Lucene query fragment to AND with the main query.");
  }

  /**
   * Sets a property example when the property exists.
   *
   * @param schema the parent schema
   * @param propertyName the property name
   * @param example the example value
   */
  @SuppressWarnings("unchecked")
  private void setPropertyExample(Schema<Object> schema, String propertyName,
    Object example) {
    final Schema<Object> property =
        (Schema<Object>) schema.getProperties().get(propertyName);
    if (property != null) {
      property.setExample(example);
    }
  }

  /**
   * Sets a property description when the property exists.
   *
   * @param schema the parent schema
   * @param propertyName the property name
   * @param description the property description
   */
  @SuppressWarnings("unchecked")
  private void setPropertyDescription(Schema<Object> schema,
    String propertyName, String description) {
    final Schema<Object> property =
        (Schema<Object>) schema.getProperties().get(propertyName);
    if (property != null && property.getDescription() == null) {
      property.setDescription(description);
    }
  }

  /**
   * Indicates whether a media type schema represents PFS parameters.
   *
   * @param schema the schema
   * @return true if the schema is PFS
   */
  private boolean isPfsSchema(Schema<?> schema) {
    return schema != null && (PFS_PARAMETER_REF.equals(schema.get$ref())
        || PFS_PARAMETER_INTERFACE_REF.equals(schema.get$ref()));
  }

  /**
   * Applies curated examples for common non-PFS request bodies.
   *
   * @param path the OpenAPI path
   * @param operation the operation
   */
  private void customizeCommonRequestBody(String path, Operation operation) {
    final String operationId = operation.getOperationId();
    if (operationId == null) {
      return;
    }
    if ("luceneReindex".equals(operationId)) {
      setRequestBodyExample(operation, "ProjectJpa");
    } else if (NOTE_BODY_OPERATION_IDS.contains(operationId)) {
      setRequestBodyExample(operation, "Sample note");
    } else if ("updateRelationshipType".equals(operationId)) {
      setRequestBodyExample(operation, relationshipTypeExample());
    } else if ("updateAdditionalRelationshipType".equals(operationId)) {
      setRequestBodyExample(operation, additionalRelationshipTypeExample());
    } else if ("addRelationshipType".equals(operationId)) {
      setRequestBodyExample(operation, relationshipTypeListExample());
    } else if ("addAdditionalRelationshipType".equals(operationId)) {
      setRequestBodyExample(operation, additionalRelationshipTypeListExample());
    } else if ("getProcessProgress".equals(operationId)
        && "/workflow/lookup/progress".equals(path)) {
      setRequestBodyExample(operation, 39751);
    } else if ("getBulkProcessProgress".equals(operationId)) {
      setRequestBodyExample(operation, new String[] {
          "BETA"
      });
    } else if (isTerminologyLoadOperation(operationId)) {
      setRequestBodyExample(operation, "/path/to/input");
    }
  }

  /**
   * Returns a relationship type request example.
   *
   * @return the example
   */
  private Map<String, Object> relationshipTypeExample() {
    final Map<String, Object> example = abbreviationExample("RO",
        "Related to");
    example.put("hierarchical", false);
    return example;
  }

  /**
   * Returns an additional relationship type request example.
   *
   * @return the example
   */
  private Map<String, Object> additionalRelationshipTypeExample() {
    final Map<String, Object> example =
        abbreviationExample("has_ingredient", "Has ingredient");
    example.put("hierarchical", false);
    example.put("groupingType", true);
    return example;
  }

  /**
   * Returns a relationship type pair list request example.
   *
   * @return the example
   */
  private Map<String, Object> relationshipTypeListExample() {
    final Map<String, Object> list = new LinkedHashMap<>();
    list.put("objects", new Map[] {
        relationshipTypeExample(),
        relationshipTypeExample("RO_INVERSE", "Inverse related to")
    });
    list.put("totalCount", 2);
    return list;
  }

  /**
   * Returns a relationship type request example.
   *
   * @param abbreviation the abbreviation
   * @param expandedForm the expanded form
   * @return the example
   */
  private Map<String, Object> relationshipTypeExample(String abbreviation,
    String expandedForm) {
    final Map<String, Object> example =
        abbreviationExample(abbreviation, expandedForm);
    example.put("hierarchical", false);
    return example;
  }

  /**
   * Returns an additional relationship type pair list request example.
   *
   * @return the example
   */
  private Map<String, Object> additionalRelationshipTypeListExample() {
    final Map<String, Object> list = new LinkedHashMap<>();
    list.put("objects", new Map[] {
        additionalRelationshipTypeExample(),
        additionalRelationshipTypeExample("ingredient_of", "Ingredient of")
    });
    list.put("totalCount", 2);
    return list;
  }

  /**
   * Returns an additional relationship type request example.
   *
   * @param abbreviation the abbreviation
   * @param expandedForm the expanded form
   * @return the example
   */
  private Map<String, Object> additionalRelationshipTypeExample(
    String abbreviation, String expandedForm) {
    final Map<String, Object> example =
        abbreviationExample(abbreviation, expandedForm);
    example.put("hierarchical", false);
    example.put("groupingType", true);
    return example;
  }

  /**
   * Returns common abbreviation fields.
   *
   * @param abbreviation the abbreviation
   * @param expandedForm the expanded form
   * @return the example
   */
  private Map<String, Object> abbreviationExample(String abbreviation,
    String expandedForm) {
    final Map<String, Object> example = new LinkedHashMap<>();
    example.put("abbreviation", abbreviation);
    example.put("expandedForm", expandedForm);
    example.put("terminology", "NCIMTH");
    example.put("version", "latest");
    example.put("published", false);
    example.put("publishable", true);
    return example;
  }

  /**
   * Indicates whether the operation is a terminology load operation.
   *
   * @param operationId the operation id
   * @return true if the operation accepts an input path body
   */
  private boolean isTerminologyLoadOperation(String operationId) {
    return "loadTerminologySimple".equals(operationId)
        || "loadTerminologyRrf".equals(operationId)
        || "loadTerminologyRf2Delta".equals(operationId)
        || "loadTerminologyRf2Snapshot".equals(operationId)
        || "loadTerminologyRf2Full".equals(operationId)
        || "loadTerminologyClaml".equals(operationId)
        || "loadTerminologyOwl".equals(operationId);
  }

  /**
   * Sets the same example on each media type of an operation request body.
   *
   * @param operation the operation
   * @param example the request body example
   */
  private void setRequestBodyExample(Operation operation, Object example) {
    if (operation.getRequestBody() == null
        || operation.getRequestBody().getContent() == null) {
      return;
    }
    operation.getRequestBody().getContent().values()
        .forEach(mediaType -> mediaType.setExample(example));
  }

  /**
   * Returns a safe PFS example.
   *
   * @return the example
   */
  private Map<String, Object> pfsExample() {
    final Map<String, Object> example = new LinkedHashMap<>();
    example.put("maxResults", 25);
    example.put("startIndex", 0);
    return example;
  }

  /**
   * Returns safe named PFS examples.
   *
   * @return the examples
   */
  private Map<String, Example> pfsExamples(Operation operation) {
    final Map<String, Example> examples = new LinkedHashMap<>();
    examples.put("firstPage",
        new Example().summary("First page").value(pfsExample()));
    if (supportsNameSort(operation)) {
      examples.put("sortedByName",
          new Example().summary("Sorted by name").value(pfsSortedExample()));
    }
    examples.put("restrictedToNonObsolete", new Example()
        .summary("Restricted to non-obsolete").value(pfsRestrictedExample()));
    return examples;
  }

  /**
   * Indicates whether the operation supports the name sort example.
   *
   * @param operation the operation
   * @return true if name sort is supported
   */
  private boolean supportsNameSort(Operation operation) {
    return operation.getOperationId() != null
        && NAME_SORT_OPERATION_IDS.contains(operation.getOperationId());
  }

  /**
   * Returns a PFS example with sorting.
   *
   * @return the example
   */
  private Map<String, Object> pfsSortedExample() {
    final Map<String, Object> example = pfsExample();
    example.put("sortField", "name");
    example.put("ascending", true);
    return example;
  }

  /**
   * Returns a PFS example with a Lucene query restriction.
   *
   * @return the example
   */
  private Map<String, Object> pfsRestrictedExample() {
    final Map<String, Object> example = pfsExample();
    example.put("queryRestriction", "obsolete:false");
    return example;
  }

  /**
   * Returns a valid OpenAPI server URL for the servlet context path.
   *
   * @param contextPath the context path
   * @return the server URL
   */
  private String normalizeContextPath(String contextPath) {
    if (contextPath == null || contextPath.isBlank()) {
      return "/";
    }
    if (!contextPath.startsWith("/")) {
      contextPath = "/" + contextPath;
    }
    return contextPath.endsWith("/") && contextPath.length() > 1
        ? contextPath.substring(0, contextPath.length() - 1) : contextPath;
  }
}
