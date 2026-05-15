/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.rest.impl;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Enables development-only REST endpoints outside PROD deployments.
 */
public class NonProdRestCondition implements Condition {

  /* see superclass */
  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    String deployMode = context.getEnvironment().getProperty("deploy.mode", "STANDARD");
    return deployMode == null || !deployMode.contains("PROD");
  }
}
