/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.rest.impl;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.wci.umls.server.jpa.services.MetadataServiceJpa;
import com.wci.umls.server.services.MetadataService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Periodic keep-alive task for metadata database connections.
 */
@Component
@ConditionalOnProperty(name = "termserver.admin.task",
    havingValue = "false", matchIfMissing = true)
public class TermServerKeepAlive {

  /** The timer. */
  private Timer timer;

  /**
   * Starts the keep-alive timer.
   */
  @PostConstruct
  public void start() {
    Logger.getLogger(getClass()).info("TERM SERVER APPLICATION START");
    timer = new Timer("term-server-keep-alive", true);
    Calendar today = Calendar.getInstance();
    today.set(Calendar.HOUR_OF_DAY, 2);
    today.set(Calendar.MINUTE, 0);
    today.set(Calendar.SECOND, 0);
    timer.scheduleAtFixedRate(new InitializationTask(), today.getTime(), 6 * 60 * 60 * 1000);
  }

  /**
   * Stops the keep-alive timer.
   */
  @PreDestroy
  public void stop() {
    if (timer != null) {
      timer.cancel();
    }
  }

  /** Initialization task. */
  class InitializationTask extends TimerTask {

    /* see superclass */
    @Override
    public void run() {
      try {
        Logger.getLogger(getClass()).info("  PING");
        if (new ConfigureServiceRestImpl().isConfigured()) {
          MetadataService service = new MetadataServiceJpa();
          service.getRootTerminologies();
          service.close();
        }
      } catch (Exception e) {
        stop();
        e.printStackTrace();
        Logger.getLogger(getClass()).error("Error running the metadata keep-alive process.");
      }
    }
  }
}
