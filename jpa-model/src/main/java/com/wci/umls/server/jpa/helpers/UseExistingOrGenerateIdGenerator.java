/*
 *    Copyright 2022 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.helpers;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.util.EnumSet;
import java.util.Properties;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.id.OptimizableGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

/** Generator to create a unique ID for the object. If the object's ID is already set, keep it. */
public class UseExistingOrGenerateIdGenerator implements BeforeExecutionGenerator {

  private final SequenceStyleGenerator sequenceGenerator;

  /**
   * Instantiates a new use existing or generate id generator.
   *
   * @param config the config
   * @param idMember the id member
   * @param creationContext the creation context
   */
  @SuppressWarnings("resource")
  public UseExistingOrGenerateIdGenerator(
      final UseExistingOrGeneratedId config,
      final Member idMember,
      final GeneratorCreationContext creationContext) {

    this.sequenceGenerator = new SequenceStyleGenerator();

    // Get the @SequenceGenerator annotation if present
    jakarta.persistence.SequenceGenerator seqGenAnnotation = null;
    if (idMember instanceof AnnotatedElement) {
      seqGenAnnotation =
          ((AnnotatedElement) idMember).getAnnotation(jakarta.persistence.SequenceGenerator.class);
    }

    final Properties properties = new Properties();
    if (seqGenAnnotation != null) {
      properties.setProperty(
          SequenceStyleGenerator.SEQUENCE_PARAM, seqGenAnnotation.sequenceName());
      properties.setProperty(
          OptimizableGenerator.INITIAL_PARAM, String.valueOf(seqGenAnnotation.initialValue()));
      properties.setProperty(
          OptimizableGenerator.INCREMENT_PARAM, String.valueOf(seqGenAnnotation.allocationSize()));
    } else {
      // Fallback defaults
      properties.setProperty(SequenceStyleGenerator.SEQUENCE_PARAM, "table_generator");
      properties.setProperty(OptimizableGenerator.INITIAL_PARAM, "1");
      properties.setProperty(OptimizableGenerator.INCREMENT_PARAM, "50");
    }

    properties.setProperty(OptimizableGenerator.OPT_PARAM, "pooled-lo");

    sequenceGenerator.configure(
        creationContext.getProperty().getType(), properties, creationContext.getServiceRegistry());
  }

  @Override
  public Object generate(
      final SharedSessionContractImplementor session,
      final Object owner,
      final Object currentValue,
      final EventType eventType) {

    if (owner == null) {
      throw new IllegalArgumentException("Object passed to generate is null");
    }

    // If ID is already set, use it
    if (currentValue != null) {
      return currentValue;
    }

    // Otherwise generate a new one
    return sequenceGenerator.generate(session, owner);
  }

  @Override
  public EnumSet<EventType> getEventTypes() {
    return EnumSet.of(EventType.INSERT);
  }

  //  /* see superclass */
  //  @Override
  //  public Object generate(SharedSessionContractImplementor session,
  //    Object object) {
  //    if (object == null) {
  //      throw new IllegalArgumentException("Object passed to generate is null");
  //    }
  //    // NOTE: this may throw a cast class exception if things don't implement
  //    // HasId - this generator should ONLY be used where a class does implement
  //    // HasId.
  //    final Long id = ((HasId) object).getId();
  //    return id != null ? id : super.generate(session, object);
  //  }
}
