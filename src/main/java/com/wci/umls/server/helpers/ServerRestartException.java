/**
 * Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.helpers;

/**
 * Represents an intentional application restart requested by a process step.
 */
@SuppressWarnings("serial")
public class ServerRestartException extends Exception {

  /**
   * Instantiates a {@link ServerRestartException}.
   *
   * @param message the message
   */
  public ServerRestartException(String message) {
    super(message);
  }

  /**
   * Instantiates a {@link ServerRestartException}.
   *
   * @param message the message
   * @param t the cause
   */
  public ServerRestartException(String message, Exception t) {
    super(message, t);
  }
}
