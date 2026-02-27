/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.model.helpers;

import java.util.Map;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

import com.wci.umls.server.model.algo.User;

/**
 * Hibernate search field bridge for a map of {@link User} -&gt; anything.
 */
@SuppressWarnings("rawtypes")
public class UserMapUserNameBridge implements ValueBridge<Map, String> {

  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public String toIndexedValue(Map value,
    ValueBridgeToIndexedValueContext context) {
    if (value != null) {
      final StringBuilder buf = new StringBuilder();
      final Map<User, ?> map = (Map<User, ?>) value;
      for (final User item : map.keySet()) {
        buf.append(item.getUserName()).append(" ");
      }
      return buf.toString();
    }
    return null;
  }
}