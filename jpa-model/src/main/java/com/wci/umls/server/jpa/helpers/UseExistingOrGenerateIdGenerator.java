/*
 *    Copyright 2022 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.helpers;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

import com.wci.umls.server.helpers.HasId;

/**
 * Generator to create a unique ID for the object. If the object's ID is already
 * set, keep it.
 */
public class UseExistingOrGenerateIdGenerator extends SequenceStyleGenerator {

  /* see superclass */
  @Override
  public Object generate(SharedSessionContractImplementor session,
    Object object) {
    if (object == null) {
      throw new IllegalArgumentException("Object passed to generate is null");
    }
    // NOTE: this may throw a cast class exception if things don't implement
    // HasId - this generator should ONLY be used where a class does implement
    // HasId.
    final Long id = ((HasId) object).getId();
    return id != null ? id : super.generate(session, object);
  }
}