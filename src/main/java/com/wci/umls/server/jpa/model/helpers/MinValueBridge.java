/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.model.helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

/**
 * Hibernate search field bridge for a collection that returns the minimum
 * (sorted) value.
 */
@SuppressWarnings("rawtypes")
public class MinValueBridge implements ValueBridge<Collection, String> {

  /* see superclass */
  @Override
  public String toIndexedValue(Collection value,
    ValueBridgeToIndexedValueContext context) {
    if (value != null) {
      final List<?> values = new ArrayList<>(value);
      Collections.sort(values,
          (a1, a2) -> a1.toString().compareTo(a2.toString()));
      if (values.size() > 0) {
        return values.get(0).toString();
      }
    }
    return null;
  }
}