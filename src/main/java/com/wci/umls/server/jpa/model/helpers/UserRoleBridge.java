/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.model.helpers;

import java.util.Map;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

import com.wci.umls.server.model.algo.User;
import com.wci.umls.server.model.algo.UserRole;

/**
 * Hibernate search field bridge for searching user/role combinations. For
 * example, "userRoleMap:user1ADMIN"
 */
@SuppressWarnings("rawtypes")
public class UserRoleBridge implements ValueBridge<Map, String> {

  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public String toIndexedValue(Map value,
    ValueBridgeToIndexedValueContext context) {
    if (value != null) {
      final StringBuilder buf = new StringBuilder();
      final Map<User, UserRole> map = (Map<User, UserRole>) value;
      for (final Map.Entry<User, UserRole> entry : map.entrySet()) {
        buf.append(entry.getKey().getUserName())
            .append(entry.getValue().toString()).append(",");
      }
      return buf.toString();
    }
    return null;
  }
}