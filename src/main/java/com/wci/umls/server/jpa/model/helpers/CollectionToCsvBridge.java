/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.model.helpers;

import java.util.Collection;
import java.util.Iterator;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

/**
 * Hibernate search field bridge for a collection.
 */
@SuppressWarnings("rawtypes")
public class CollectionToCsvBridge implements ValueBridge<Collection, String> {

  /* see superclass */
  @Override
  public String toIndexedValue(Collection value,
    ValueBridgeToIndexedValueContext context) {
    if (value != null) {
      final StringBuilder buf = new StringBuilder();
      final Iterator<?> it = value.iterator();
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