package com.wci.umls.server.jpa.helpers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.annotations.IdGeneratorType;

/**
 * ID generation that uses existing ID if set, otherwise generates new ID
 * using a table-based sequence.
 */
@IdGeneratorType(UseExistingOrGenerateIdGenerator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface UseExistingOrGeneratedId {
  /** Table name for sequence storage */
  String table() default "table_generator";

  /** Column name for sequence value */
  String valueColumn() default "next_val";

  /** Segment column name (identifies which sequence) */
  String segmentColumn() default "sequence_name";

  /** Segment value (row identifier in shared table) */
  String segmentValue() default "Entity";

  /** Initial value if sequence doesn't exist */
  int initialValue() default 1;

  /** Allocation size (how many IDs to reserve at once) */
  int allocationSize() default 50;
}
