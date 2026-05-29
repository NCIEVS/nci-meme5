/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.test.helpers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.rules.TestName;

import com.wci.umls.server.model.algo.ValidationResult;
import com.wci.umls.server.jpa.algo.action.AbstractMolecularAction;

/**
 * Support for integration tests
 */
public class IntegrationUnitSupport {

  /** The name. */
  @Rule
  public TestName name = new TestName();

  /**
   * Returns a unique display name for data created by a test.
   *
   * @param prefix the readable prefix
   * @return the unique name
   */
  protected String uniqueTestName(String prefix) {
    return prefix + " " + uniqueTestToken("");
  }

  /**
   * Returns a compact unique token for test data identifiers.
   *
   * @param prefix the token prefix
   * @return the unique token
   */
  protected String uniqueTestToken(String prefix) {
    final String method =
        name == null || name.getMethodName() == null ? "unknown"
            : name.getMethodName();
    final String safePrefix = prefix == null ? "" : prefix;
    return safePrefix + getClass().getSimpleName() + "_" + method + "_"
        + System.currentTimeMillis();
  }

  /**
   * Returns a numeric unique token for test data identifiers.
   *
   * @return the unique token
   */
  @SuppressWarnings("static-method")
  protected String uniqueNumericToken() {
    return Long.toString(System.currentTimeMillis());
  }

  /**
   * Returns a unique token that remains one term in search analyzers.
   *
   * @param prefix the token prefix
   * @return the unique token
   */
  protected String uniqueSearchToken(String prefix) {
    final String method =
        name == null || name.getMethodName() == null ? "unknown"
            : name.getMethodName();
    final String safePrefix = prefix == null ? "" : prefix;
    final String raw =
        safePrefix + getClass().getSimpleName() + method
            + System.currentTimeMillis();
    return raw.replaceAll("[^A-Za-z0-9]", "");
  }

  /**
   * Returns the method.
   *
   * @param match the match
   * @param file the file
   * @return the method
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @SuppressWarnings("static-method")
  public String getMethodText(String match, Path file) throws IOException {
    final List<String> lines = Files.lines(file).collect(Collectors.toList());
    final StringBuilder sb = new StringBuilder();
    boolean inMethod = false;
    for (final String line : lines) {

      // Method start
      if (line.contains(match)) {
        inMethod = true;
      }

      if (inMethod) {
        sb.append(line);
      }
      // Method end
      // TODO: this could be better, relies on formatter
      if (inMethod && line.startsWith("  }")) {
        inMethod = false;
        break;
      }

    }
    return sb.toString();

  }

  /**
   * Test action preconditions.
   *
   * @param action the action
   * @return the validation result
   * @throws Exception the exception
   */
  @SuppressWarnings("static-method")
  public ValidationResult checkActionPreconditions(
    AbstractMolecularAction action) throws Exception {

    action.beginTransaction();
    action.initialize(action.getProject(), action.getConceptId(),
        action.getConceptId2(), action.getLastModified(), false);
    final ValidationResult validationResult = action.checkPreconditions();
    action.rollback();
    action.close();

    return validationResult;

  }
}
