/*
 *    Copyright 2022 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.helpers;

import java.lang.reflect.Member;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;

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

  /** Maximum number of attempts to reserve a sequence block. */
  private static final int MAX_SEQUENCE_RETRIES = 20;

  /** Base delay before retrying a collided sequence update. */
  private static final int BASE_RETRY_SLEEP_MILLIS = 10;

  /** Maximum delay before retrying a collided sequence update. */
  private static final int MAX_RETRY_SLEEP_MILLIS = 100;

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
        return reserveSequenceBlockWithRetry(connection);
      }
    });
  }

  /**
   * Reserves the next block of identifiers, retrying transient optimistic
   * sequence collisions.
   *
   * @param connection the connection
   * @return the first identifier in the reserved block
   * @throws SQLException if the block cannot be reserved
   */
  private long reserveSequenceBlockWithRetry(Connection connection)
    throws SQLException {

    SQLException lastException = null;
    for (int attempt = 1; attempt <= MAX_SEQUENCE_RETRIES; attempt++) {
      try {
        return reserveSequenceBlock(connection);
      } catch (ConcurrentSequenceUpdateException
          | SQLIntegrityConstraintViolationException e) {
        lastException = e;
      } catch (SQLException e) {
        if (!isDuplicateKey(e)) {
          throw e;
        }
        lastException = e;
      }

      if (attempt < MAX_SEQUENCE_RETRIES) {
        sleepBeforeRetry(attempt);
      }
    }

    throw new SQLException("Unable to reserve sequence block for " + tableName
        + "." + segmentValue + " after " + MAX_SEQUENCE_RETRIES + " attempts",
        lastException);
  }

  /**
   * Reserves one block of identifiers using an optimistic compare-and-swap
   * update.
   *
   * @param connection the connection
   * @return the first identifier in the reserved block
   * @throws SQLException if the sequence cannot be updated
   */
  private long reserveSequenceBlock(Connection connection) throws SQLException {

    final Long currentValue = readCurrentValue(connection);
    if (currentValue == null) {
      insertInitialRow(connection);
      return initialValue;
    }

    final long nextValue = currentValue + allocationSize;
    final String updateSql = "UPDATE " + tableName + " SET " + valueColumn
        + " = ? WHERE " + segmentColumn + " = ? AND " + valueColumn + " = ?";

    try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
      ps.setLong(1, nextValue);
      ps.setString(2, segmentValue);
      ps.setLong(3, currentValue);
      final int updated = ps.executeUpdate();
      if (updated == 0) {
        throw new ConcurrentSequenceUpdateException(
            "Concurrent sequence update for " + tableName + "."
                + segmentValue);
      }
    }

    return currentValue;
  }

  /**
   * Reads the current sequence value.
   *
   * @param connection the connection
   * @return the current sequence value, or null if no sequence row exists
   * @throws SQLException if the value cannot be read
   */
  private Long readCurrentValue(Connection connection) throws SQLException {

    final String selectSql = "SELECT " + valueColumn + " FROM " + tableName
        + " WHERE " + segmentColumn + " = ?";

    try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
      ps.setString(1, segmentValue);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getLong(1) : null;
      }
    }
  }

  /**
   * Inserts the initial sequence row.
   *
   * @param connection the connection
   * @throws SQLException if the row cannot be inserted
   */
  private void insertInitialRow(Connection connection) throws SQLException {

    final String insertSql = "INSERT INTO " + tableName + " ("
        + segmentColumn + ", " + valueColumn + ") VALUES (?, ?)";

    try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
      ps.setString(1, segmentValue);
      ps.setLong(2, initialValue + allocationSize);
      ps.executeUpdate();
    }
  }

  /**
   * Sleeps briefly before retrying a collided sequence update.
   *
   * @param attempt the attempt number
   * @throws SQLException if interrupted while waiting
   */
  private void sleepBeforeRetry(int attempt) throws SQLException {

    final int baseSleep = Math.min(MAX_RETRY_SLEEP_MILLIS,
        BASE_RETRY_SLEEP_MILLIS * attempt);
    final int jitter = ThreadLocalRandom.current()
        .nextInt(BASE_RETRY_SLEEP_MILLIS);

    try {
      Thread.sleep(baseSleep + jitter);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new SQLException("Interrupted while retrying sequence update", e);
    }
  }

  /**
   * Indicates whether the exception represents a duplicate-key row insert.
   *
   * @param e the exception
   * @return true if duplicate key
   */
  private boolean isDuplicateKey(SQLException e) {

    for (SQLException current = e; current != null;
        current = current.getNextException()) {
      if ("23000".equals(current.getSQLState())
          || current.getErrorCode() == 1062) {
        return true;
      }
    }
    return false;
  }

  /** Retryable optimistic sequence collision. */
  private static class ConcurrentSequenceUpdateException extends SQLException {

    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates an exception.
     *
     * @param message the message
     */
    ConcurrentSequenceUpdateException(String message) {
      super(message);
    }
  }
}
