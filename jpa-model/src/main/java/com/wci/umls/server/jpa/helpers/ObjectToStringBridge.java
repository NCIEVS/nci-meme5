/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.helpers;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

/**
 * Hibernate search field bridge for an object that can be turned into a string.
 */
public class ObjectToStringBridge implements ValueBridge<Object, String> {

  /* see superclass */
  @SuppressWarnings("rawtypes")
  @Override
  public String toIndexedValue(Object value,
    ValueBridgeToIndexedValueContext context) {
    if (value != null) {
      if (value.getClass().isEnum()) {
        return ((Enum) value).name();
      }
      return value.toString();
    }
    return null;
  }
}