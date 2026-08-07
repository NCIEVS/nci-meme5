/*
 *    Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.rest.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.wci.umls.server.helpers.PropertyUtility;
import com.wci.umls.server.jpa.algo.maint.ServerRestartAlgorithm;
import com.wci.umls.server.jpa.services.ProcessServiceJpa;
import com.wci.umls.server.model.algo.ProcessExecution;

/**
 * Coordinates intentional server restarts and pending process auto-resume.
 */
@Component
@ConditionalOnProperty(name = "termserver.admin.task",
    havingValue = "false", matchIfMissing = true)
public class ServerRestartCoordinator {

  /** Auto-resume enabled property. */
  public static final String AUTO_RESUME_ENABLED_PROPERTY =
      "server.restart.auto.resume.enabled";

  /** Exit enabled property. */
  public static final String EXIT_ENABLED_PROPERTY =
      "server.restart.exit.enabled";

  /** Shutdown delay property. */
  public static final String SHUTDOWN_DELAY_MS_PROPERTY =
      "server.restart.shutdown.delay.ms";

  /** The logger. */
  private static final Logger LOGGER =
      Logger.getLogger(ServerRestartCoordinator.class);

  /** Prevents duplicate shutdown requests. */
  private static final AtomicBoolean RESTART_REQUESTED =
      new AtomicBoolean(false);

  /** Spring application context. */
  private static ConfigurableApplicationContext applicationContext;

  /**
   * Instantiates the coordinator.
   *
   * @param context the application context
   */
  @Autowired
  public ServerRestartCoordinator(final ConfigurableApplicationContext context) {
    applicationContext = context;
  }

  /**
   * Resumes process executions that requested a server restart before shutdown.
   *
   * @param event the application ready event
   */
  @EventListener
  public void resumePendingServerRestarts(final ApplicationReadyEvent event) {
    if (!isEnabled(AUTO_RESUME_ENABLED_PROPERTY)) {
      LOGGER.info("Server Restart auto-resume is disabled.");
      return;
    }

    final List<ProcessExecution> pendingExecutions = new ArrayList<>();
    ProcessServiceJpa processService = null;
    try {
      processService = new ProcessServiceJpa();
      @SuppressWarnings("unchecked")
      final List<Number> ids = processService.getEntityManager()
          .createNativeQuery(
              "select ProcessExecutionJpa_id "
                  + "from processexecutionjpa_executioninfo "
                  + "where executionInfo_KEY = ?1 and executionInfo = ?2")
          .setParameter(1, ServerRestartAlgorithm.RESTART_PENDING_KEY)
          .setParameter(2, "true").getResultList();
      for (final Number id : ids) {
        final ProcessExecution execution =
            processService.getProcessExecution(id.longValue());
        if (execution != null && execution.getStartDate() != null
            && execution.getFinishDate() != null
            && execution.getFailDate() != null) {
          pendingExecutions.add(execution);
        }
      }
    } catch (Exception e) {
      LOGGER.error("Unable to find pending Server Restart executions.", e);
      return;
    } finally {
      if (processService != null) {
        try {
          processService.close();
        } catch (Exception e) {
          LOGGER.warn("Unable to close process service.", e);
        }
      }
    }

    for (final ProcessExecution execution : pendingExecutions) {
      try {
        LOGGER.info("Auto-resuming process execution " + execution.getId()
            + " after Server Restart.");
        new ProcessServiceRestImpl().restartProcessInternal(
            execution.getProject().getId(), execution.getId(), true);
      } catch (Exception e) {
        LOGGER.error("Unable to auto-resume process execution "
            + execution.getId() + " after Server Restart.", e);
      }
    }
  }

  /**
   * Requests an intentional application restart.
   */
  public static void requestRestart() {
    if (!RESTART_REQUESTED.compareAndSet(false, true)) {
      LOGGER.info("Server Restart already requested.");
      return;
    }

    final Thread shutdownThread =
        new Thread(ServerRestartCoordinator::shutdownApplication,
            "server-restart-shutdown");
    shutdownThread.setDaemon(false);
    shutdownThread.start();
  }

  /**
   * Shuts the application down after a short delay.
   */
  private static void shutdownApplication() {
    try {
      Thread.sleep(getShutdownDelayMs());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOGGER.warn("Server Restart shutdown delay interrupted.", e);
    }

    if (!isEnabled(EXIT_ENABLED_PROPERTY)) {
      LOGGER.warn("Server Restart requested, but application exit is disabled.");
      return;
    }

    final ConfigurableApplicationContext context = applicationContext;
    if (context == null) {
      LOGGER.error("Server Restart requested, but application context is not "
          + "available. The application will not exit.");
      return;
    }

    LOGGER.info("Server Restart exiting application for systemd restart.");
    final int exitCode = SpringApplication.exit(context, () -> 0);
    System.exit(exitCode);
  }

  /**
   * Indicates whether a boolean property is enabled.
   *
   * @param propertyName the property name
   * @return true if enabled
   */
  private static boolean isEnabled(final String propertyName) {
    return Boolean.parseBoolean(
        PropertyUtility.getProperties().getProperty(propertyName));
  }

  /**
   * Returns the shutdown delay.
   *
   * @return the shutdown delay in milliseconds
   */
  private static long getShutdownDelayMs() {
    final Properties properties = PropertyUtility.getProperties();
    return Long.parseLong(properties.getProperty(SHUTDOWN_DELAY_MS_PROPERTY));
  }
}
