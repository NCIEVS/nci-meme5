/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.model.helpers;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

/**
 * Hibernate search field bridge for a map of state history entries. Returns the
 * key (state) with the most recent (max) date value.
 */
@SuppressWarnings("rawtypes")
public class MaxStateHistoryBridge implements ValueBridge<Map, String> {

  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public String toIndexedValue(Map value,
    ValueBridgeToIndexedValueContext context) {
    if (value != null) {
      final Map<String, Date> map = (Map<String, Date>) value;
      final Iterator<? extends Map.Entry<String, Date>> it =
          map.entrySet().iterator();
      String max = "";
      long maxTime = 0L;
      while (it.hasNext()) {
        final Map.Entry<String, Date> entry = it.next();
        final String key = entry.getKey();
        final Date d = entry.getValue();
        if (d.getTime() > maxTime) {
          maxTime = d.getTime();
          max = key;
        }
      }
      return max;
    }
    return null;
  }
}