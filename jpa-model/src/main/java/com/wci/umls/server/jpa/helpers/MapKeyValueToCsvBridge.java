/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.helpers;

import java.util.Iterator;
import java.util.Map;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

/**
 * Hibernate search field bridge for the key/values of a map.
 */
@SuppressWarnings("rawtypes")
public class MapKeyValueToCsvBridge implements ValueBridge<Map, String> {

  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public String toIndexedValue(Map value,
    ValueBridgeToIndexedValueContext context) {
    if (value != null) {
      final StringBuilder buf = new StringBuilder();
      final Iterator<Map.Entry<?, ?>> it = value.entrySet().iterator();
      while (it.hasNext()) {
        final Map.Entry<?, ?> entry = it.next();
        final String key = entry.getKey().toString();
        final String v = entry.getValue().toString();
        buf.append(key).append("=").append(v);
        if (it.hasNext())
          buf.append(" ");
      }
      return buf.toString();
    }
    return null;
  }
}