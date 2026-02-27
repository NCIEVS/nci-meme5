/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.model.helpers;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

/**
 * Hibernate search field bridge for a string with underscores.
 */
public class SplitUnderscoreBridge
    implements ValueBridge<String, String> {

  /* see superclass */
  @Override
  public String toIndexedValue(String value,
    ValueBridgeToIndexedValueContext context) {
    if (value != null) {
      return value.replaceAll("_", " ");
    }
    return null;
  }
}