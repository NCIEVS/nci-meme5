/*
 *    Copyright 2022 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.helpers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.annotations.IdGeneratorType;

/**
 * ID generation that uses existing ID if set, otherwise generates new ID
 * using a table-based sequence with pooled-lo optimizer.
 */
@IdGeneratorType(UseExistingOrGenerateIdGenerator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface UseExistingOrGenerateId {
  String sequenceName() default "table_generator";
  int initialValue() default 1;
  int incrementSize() default 1;
  String optimizer() default "pooled-lo";
}
