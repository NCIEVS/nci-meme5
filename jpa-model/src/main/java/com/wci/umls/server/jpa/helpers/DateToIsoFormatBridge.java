/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.helpers;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

/**
 * Hibernate search field bridge for converting a Date to ISO format string
 * like "2024-12-11T03:10:27.000Z" for indexing and range queries.
 */
public class DateToIsoFormatBridge implements ValueBridge<Date, String> {

  /* see superclass */
  @Override
  public final String toIndexedValue(final Date value,
    final ValueBridgeToIndexedValueContext context) {

    if (value != null) {
      return DateTimeFormatter.ISO_INSTANT
          .format(Instant.ofEpochMilli(value.getTime()));
    }
    return null;
  }
}
