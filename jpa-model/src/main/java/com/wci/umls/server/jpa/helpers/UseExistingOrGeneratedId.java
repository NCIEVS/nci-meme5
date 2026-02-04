package com.wci.umls.server.jpa.helpers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.annotations.IdGeneratorType;

/** The Interface UseExistingOrGeneratedId. */
@IdGeneratorType(UseExistingOrGenerateIdGenerator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface UseExistingOrGeneratedId {
  // n/a
}
