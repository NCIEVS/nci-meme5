/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.helpers;

import java.util.Iterator;
import java.util.Map;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

/**
 * Hibernate search field bridge for the keys of a map.
 */
@SuppressWarnings("rawtypes")
public class MapKeyToCsvBridge implements ValueBridge<Map, String> {

  /* see superclass */
  @Override
  public String toIndexedValue(Map value,
    ValueBridgeToIndexedValueContext context) {
    if (value != null) {
      final StringBuilder buf = new StringBuilder();
      final Iterator<?> it = value.keySet().iterator();
      while (it.hasNext()) {
        final String next = it.next().toString();
        buf.append(next);
        if (it.hasNext())
          buf.append(" ");
      }
      return buf.toString();
    }
    return null;
  }
}