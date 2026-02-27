/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.rest.impl;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector;

import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

/** Custom ObjectMapperProvider. */
@Provider
public class ObjectMapperProvider implements ContextResolver<ObjectMapper> {

  /** The default object mapper. */
  final ObjectMapper defaultObjectMapper;

  /** Instantiates an empty {@link ObjectMapperProvider}. */
  public ObjectMapperProvider() {
    defaultObjectMapper = createDefaultMapper();
  }

  /* see superclass */
  @Override
  public ObjectMapper getContext(final Class<?> type) {
    return defaultObjectMapper;
  }

  /**
   * Creates the combined object mapper.
   *
   * @return the object mapper
   */
  private static ObjectMapper createDefaultMapper() {
    ObjectMapper mapper = new ObjectMapper();
    AnnotationIntrospector introspector =
        new JakartaXmlBindAnnotationIntrospector(mapper.getTypeFactory());
    mapper.setAnnotationIntrospector(introspector);

    // final AnnotationIntrospector jacksonIntrospector =
    // new JacksonAnnotationIntrospector();
    // return AnnotationIntrospector.pair(jacksonIntrospector,
    // jaxbIntrospector);
    return mapper;
  }
}
