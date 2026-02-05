/*
 *    Copyright 2022 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.helpers;

import java.lang.reflect.Member;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumSet;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.jdbc.AbstractReturningWork;

import com.wci.umls.server.helpers.HasId;

/**
 * Custom ID generator using table-based sequences.
 * Uses existing ID if set, otherwise generates new ID from table sequence.
 */
public class UseExistingOrGenerateIdGenerator implements BeforeExecutionGenerator {

  private String tableName = "table_generator";
  private String valueColumn = "next_val";
  private String segmentColumn = "sequence_name";
  private String segmentValue = "Entity";
  private int initialValue = 1;
  private int allocationSize = 50;

  // Pooled-lo optimizer state
  private long hiValue = -1;
  private long loValue = 0;

  /**
   * No-arg constructor required by Hibernate as fallback.
   * Uses default values matching the annotation defaults.
   */
  public UseExistingOrGenerateIdGenerator() {
    // Uses default field values
  }

  /**
   * Constructor for @IdGeneratorType pattern.
   */
  public UseExistingOrGenerateIdGenerator(
      UseExistingOrGeneratedId config,
      Member annotatedMember,
      GeneratorCreationContext context) {

    this.tableName = config.table();
    this.valueColumn = config.valueColumn();
    this.segmentColumn = config.segmentColumn();
    this.segmentValue = config.segmentValue();
    this.initialValue = config.initialValue();
    this.allocationSize = config.allocationSize();
  }

  @Override
  public EnumSet<EventType> getEventTypes() {
    return EnumSet.of(EventType.INSERT);
  }

  @Override
  public boolean allowAssignedIdentifiers() {
    return true;
  }

  @Override
  public Object generate(SharedSessionContractImplementor session, Object owner,
      Object currentValue, EventType eventType) {

    if (owner == null) {
      throw new IllegalArgumentException("Owner entity is null");
    }

    // If ID already set, use it
    if (currentValue != null) {
      return currentValue;
    }
    if (owner instanceof HasId) {
      Long existingId = ((HasId) owner).getId();
      if (existingId != null) {
        return existingId;
      }
    }

    // Generate new ID using pooled-lo algorithm
    return generateNextId(session);
  }

  private synchronized long generateNextId(SharedSessionContractImplementor session) {
    // If we have IDs remaining in current pool, use one
    if (loValue < allocationSize && hiValue >= 0) {
      return hiValue + loValue++;
    }

    // Need to get next hi value from database
    hiValue = getNextHiValue(session);
    loValue = 1;
    return hiValue;
  }

  private long getNextHiValue(SharedSessionContractImplementor session) {
    return session.doReturningWork(new AbstractReturningWork<Long>() {
      @Override
      public Long execute(Connection connection) throws SQLException {
        // Read current value
        String selectSql = "SELECT " + valueColumn + " FROM " + tableName
            + " WHERE " + segmentColumn + " = ?";

        long currentValue;
        try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
          ps.setString(1, segmentValue);
          try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
              currentValue = rs.getLong(1);
            } else {
              // Row doesn't exist, insert it
              insertInitialRow(connection);
              currentValue = initialValue;
            }
          }
        }

        // Update to next value
        long nextValue = currentValue + allocationSize;
        String updateSql = "UPDATE " + tableName + " SET " + valueColumn + " = ? "
            + "WHERE " + segmentColumn + " = ? AND " + valueColumn + " = ?";

        try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
          ps.setLong(1, nextValue);
          ps.setString(2, segmentValue);
          ps.setLong(3, currentValue);
          int updated = ps.executeUpdate();
          if (updated == 0) {
            // Concurrent modification, retry
            throw new SQLException("Concurrent sequence update, retry");
          }
        }

        return currentValue;
      }

      private void insertInitialRow(Connection connection) throws SQLException {
        String insertSql = "INSERT INTO " + tableName + " (" + segmentColumn + ", "
            + valueColumn + ") VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
          ps.setString(1, segmentValue);
          ps.setLong(2, initialValue + allocationSize);
          ps.executeUpdate();
        }
      }
    });
  }
}
