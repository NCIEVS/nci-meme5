/**
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.model.helpers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

import com.wci.umls.server.helpers.ProxyTester;

/**
 * Automates JUnit testing of equals and hashcode methods.
 */
public class IndexedFieldTester extends ProxyTester {

  /**
   * Constructs a new getter/setter tester to test objects of a particular
   * class.
   *
   * @param obj Object fto test.
   */
  public IndexedFieldTester(Object obj) {
    super(obj);
  }

  /**
   * Test analyzed indexed fields.
   *
   * @return true, if successful
   * @throws Exception the exception
   */
  public boolean testAnalyzedIndexedFields() throws Exception {
    Logger.getLogger(getClass())
        .debug("Test analyzed indexed fields - " + clazz.getName());

    final Map<String, Boolean> analyzedFieldsMap = getAnalyzedFieldsMap(clazz);
    for (final String field : includes == null ? new HashSet<String>()
        : includes) {
      boolean found = false;
      if (analyzedFieldsMap.containsKey(field)) {
        found = analyzedFieldsMap.get(field);
      }
      if (!found) {
        Logger.getLogger(getClass())
            .info("  " + field + " is not defined as analyzed");
        return false;
      }
    }
    for (final String field : analyzedFieldsMap.keySet()) {
      if (analyzedFieldsMap.get(field)
          && (includes == null || !includes.contains(field))) {
        Logger.getLogger(getClass())
            .info("  " + field + " should be in the include list as analyzed");
        return false;
      }
    }

    return true;
  }

  /**
   * Test analyzed indexed fields.
   *
   * @return true, if successful
   * @throws Exception the exception
   */
  public boolean testNotAnalyzedIndexedFields() throws Exception {
    Logger.getLogger(getClass())
        .debug("Test not analyzed indexed fields - " + clazz.getName());

    final Map<String, Boolean> analyzedFieldsMap = getAnalyzedFieldsMap(clazz);
    for (final String field : includes) {
      boolean found = true;
      if (analyzedFieldsMap.containsKey(field)) {
        found = analyzedFieldsMap.get(field);
      }
      if (found) {
        Logger.getLogger(getClass())
            .info("  " + field + " is defined as analyzed");
        return false;
      }
    }
    for (final String field : analyzedFieldsMap.keySet()) {
      if (!analyzedFieldsMap.get(field) && !includes.contains(field)) {
        Logger.getLogger(getClass()).info(
            "  " + field + " should be in the include list as not analyzed");
        return false;
      }
    }

    return true;
  }

  /**
   * Returns the name analyzed pairs from annotation. Checks both field-level
   * and method-level annotations (HS6 style: annotations on fields, with
   * computed getters keeping annotations on methods).
   *
   * @param clazz the clazz
   * @return the name analyzed pairs from annotation
   */
  @SuppressWarnings("static-method")
  private Map<String, Boolean> getAnalyzedFieldsMap(Class<?> clazz) {

    // initialize the name->analyzed pair map
    final Map<String, Boolean> nameAnalyzedPairs = new HashMap<>();

    // Check field-level annotations (primary location in HS6)
    // Check all fields including inherited ones from superclasses
    Class<?> currentClass = clazz;
    while (currentClass != null && currentClass != Object.class) {
      for (final Field f : currentClass.getDeclaredFields()) {
        checkFieldAnnotations(f, nameAnalyzedPairs);
      }
      currentClass = currentClass.getSuperclass();
    }

    // Also check method-level annotations (for computed getters)
    // clazz.getMethods() already includes inherited methods
    checkMethodAnnotations(clazz, nameAnalyzedPairs);

    return nameAnalyzedPairs;
  }

  /**
   * Check method annotations and add to the map.
   *
   * @param clazz the class to check
   * @param nameAnalyzedPairs the name analyzed pairs map
   */
  @SuppressWarnings("static-method")
  private void checkMethodAnnotations(Class<?> clazz, Map<String, Boolean> nameAnalyzedPairs) {
    // Check method-level annotations (for computed getters)
    for (final Method m : clazz.getMethods()) {

      // Look at "get" and "is" methods
      String fieldName = null;
      if (m.getName().startsWith("get")) {
        fieldName = m.getName().substring(3);
        fieldName =
            fieldName.substring(0, 1).toLowerCase() + fieldName.substring(1);
      } else if (m.getName().startsWith("is")) {
        fieldName = m.getName().substring(2);
        fieldName =
            fieldName.substring(0, 1).toLowerCase() + fieldName.substring(1);
      } else {
        continue;
      }

      // check for @FullTextField annotations (analyzed) - use getAnnotationsByType for repeatable
      for (final FullTextField ann : m.getAnnotationsByType(FullTextField.class)) {
        final String name =
            (ann.name() != null && !ann.name().isEmpty()) ? ann.name()
                : fieldName;
        nameAnalyzedPairs.put(name.toLowerCase(), true);
      }

      // check for @KeywordField annotations (not analyzed) - use getAnnotationsByType for repeatable
      for (final KeywordField ann : m.getAnnotationsByType(KeywordField.class)) {
        final String name =
            (ann.name() != null && !ann.name().isEmpty()) ? ann.name()
                : fieldName;
        nameAnalyzedPairs.put(name.toLowerCase(), false);
      }

      // check for @GenericField annotations (not analyzed) - use getAnnotationsByType for repeatable
      for (final GenericField ann : m.getAnnotationsByType(GenericField.class)) {
        final String name =
            (ann.name() != null && !ann.name().isEmpty()) ? ann.name()
                : fieldName;
        nameAnalyzedPairs.put(name.toLowerCase(), false);
      }
    }
  }

  /**
   * Check field annotations and add to the map.
   *
   * @param f the field
   * @param nameAnalyzedPairs the name analyzed pairs map
   */
  @SuppressWarnings("static-method")
  private void checkFieldAnnotations(Field f, Map<String, Boolean> nameAnalyzedPairs) {
      String fieldName = f.getName();

      // check for @FullTextField annotations (analyzed) - use getAnnotationsByType for repeatable
      for (final FullTextField ann : f.getAnnotationsByType(FullTextField.class)) {
        final String name =
            (ann.name() != null && !ann.name().isEmpty()) ? ann.name()
                : fieldName;
        nameAnalyzedPairs.put(name.toLowerCase(), true);
      }

      // check for @KeywordField annotations (not analyzed) - use getAnnotationsByType for repeatable
      for (final KeywordField ann : f.getAnnotationsByType(KeywordField.class)) {
        final String name =
            (ann.name() != null && !ann.name().isEmpty()) ? ann.name()
                : fieldName;
        nameAnalyzedPairs.put(name.toLowerCase(), false);
      }

      // check for @GenericField annotations (not analyzed) - use getAnnotationsByType for repeatable
      for (final GenericField ann : f.getAnnotationsByType(GenericField.class)) {
        final String name =
            (ann.name() != null && !ann.name().isEmpty()) ? ann.name()
                : fieldName;
        nameAnalyzedPairs.put(name.toLowerCase(), false);
      }
  }

}
