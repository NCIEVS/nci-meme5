/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.model.helpers;

import java.util.Map;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

import com.wci.umls.server.helpers.HasId;

/**
 * Hibernate search field bridge for a map of {@link HasId} -&gt; anything.
 */
@SuppressWarnings("rawtypes")
public class MapIdBridge implements ValueBridge<Map, String> {

  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public String toIndexedValue(Map value,
    ValueBridgeToIndexedValueContext context) {
    if (value != null) {
      final StringBuilder buf = new StringBuilder();
      final Map<HasId, ?> map = (Map<HasId, ?>) value;
      for (final HasId item : map.keySet()) {
        buf.append(item.getId()).append(" ");
      }
      return buf.toString();
    }
    return null;
  }
}