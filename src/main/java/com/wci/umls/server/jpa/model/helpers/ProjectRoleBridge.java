/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.model.helpers;

import java.util.Map;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

import com.wci.umls.server.model.algo.Project;
import com.wci.umls.server.model.algo.UserRole;

/**
 * Hibernate search field bridge for searching project/role combinations. For
 * example, "projectRoleMap:10ADMIN"
 */
@SuppressWarnings("rawtypes")
public class ProjectRoleBridge implements ValueBridge<Map, String> {

  /* see superclass */
  @SuppressWarnings("unchecked")
  @Override
  public String toIndexedValue(Map value,
    ValueBridgeToIndexedValueContext context) {
    if (value != null) {
      final StringBuilder buf = new StringBuilder();
      final Map<Project, UserRole> map = (Map<Project, UserRole>) value;
      for (final Map.Entry<Project, UserRole> entry : map.entrySet()) {
        buf.append(entry.getKey().getId()).append(entry.getValue().toString())
            .append(",");
      }
      return buf.toString();
    }
    return null;
  }
}