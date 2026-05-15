/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.rest.impl;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Converts legacy JAX-RS web exceptions into Spring MVC responses.
 */
@RestControllerAdvice
public class RestExceptionHandler {

  /**
   * Handles legacy web application exceptions.
   *
   * @param e the exception
   * @return the response entity
   */
  @ExceptionHandler(WebApplicationException.class)
  public ResponseEntity<Object> handleWebApplicationException(WebApplicationException e) {
    Response response = e.getResponse();
    int statusCode = response != null ? response.getStatus()
        : HttpStatus.INTERNAL_SERVER_ERROR.value();
    Object body = response != null ? response.getEntity() : null;
    if (body == null) {
      body = e.getMessage();
    }
    return ResponseEntity.status(statusCode).body(body);
  }
}
