/*
 *    Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.admin;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import com.wci.umls.Application;
import com.wci.umls.server.helpers.PropertyUtility;

/**
 * Spring-backed entry point for administrative commands.
 */
public final class GeneratorApplication {

  /** Property indicating an admin task is running. */
  public static final String ADMIN_TASK_PROPERTY = "termserver.admin.task";

  /** Logger. */
  private static final Logger LOG =
      Logger.getLogger(GeneratorApplication.class.getName());

  /** Command to admin main class mapping. */
  private static final Map<String, Class<?>> COMMANDS =
      new LinkedHashMap<>();

  static {
    COMMANDS.put("createDb", UpdateDb.class);
    COMMANDS.put("updateDb", UpdateDb.class);
    COMMANDS.put("flywayDb", FlywayDb.class);
    COMMANDS.put("adHoc", AdHoc.class);
    COMMANDS.put("reindex", LuceneReindex.class);
    COMMANDS.put("reindexEcl", LuceneReindexEcl.class);
    COMMANDS.put("loadRrfSingle", RrfSingleLoader.class);
    COMMANDS.put("loadRrfMulti", RrfMultiLoader.class);
    COMMANDS.put("loadRrfUmls", RrfUmlsLoader.class);
    COMMANDS.put("loadRf2Full", Rf2FullLoader.class);
    COMMANDS.put("loadRf2Snapshot", Rf2SnapshotLoader.class);
    COMMANDS.put("loadRf2Delta", Rf2DeltaLoader.class);
    COMMANDS.put("loadSimple", SimpleLoader.class);
    COMMANDS.put("loadClaml", ClamlLoader.class);
    COMMANDS.put("loadOwl", OwlLoader.class);
    COMMANDS.put("transitiveClosure", TransitiveClosureComputer.class);
    COMMANDS.put("treePositions", TreeposComputer.class);
    COMMANDS.put("generateData", GenerateData.class);
    COMMANDS.put("removeTerminology", TerminologyRemover.class);
    COMMANDS.put("removeSourceData", SourceDataRemover.class);
    COMMANDS.put("loadRrfSourceData", RrfSourceDataLoader.class);
  }

  /**
   * Instantiates an empty {@link GeneratorApplication}.
   */
  private GeneratorApplication() {
    // n/a
  }

  /**
   * Application entry point.
   *
   * @param args command and optional command arguments
   */
  public static void main(String[] args) {
    configureCatalinaBase();
    try {
      run(args);
      System.exit(0);
    } catch (Throwable e) {
      LOG.severe("Unexpected admin application error: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Provides a Tomcat-style base directory before Log4j initializes appenders.
   */
  private static void configureCatalinaBase() {
    if (System.getProperty("catalina.base") != null
        && !System.getProperty("catalina.base").isBlank()) {
      return;
    }
    String base = System.getenv("CATALINA_BASE");
    if (base == null || base.isBlank()) {
      base = System.getenv("APP_DIR");
    }
    if (base == null || base.isBlank()) {
      base = System.getProperty("user.dir");
    }
    System.setProperty("catalina.base", base);
    new File(base, "logs").mkdirs();
  }

  /**
   * Runs an admin command with the Spring property bridge initialized.
   *
   * @param args command and optional command arguments
   * @throws Exception the exception
   */
  static void run(String[] args) throws Exception {
    if (args == null || args.length == 0 || !COMMANDS.containsKey(args[0])) {
      throw new IllegalArgumentException(usage());
    }

    final String command = args[0];
    final String[] commandArgs = Arrays.copyOfRange(args, 1, args.length);

    System.setProperty(ADMIN_TASK_PROPERTY, "true");
    try (ConfigurableApplicationContext context =
        new SpringApplicationBuilder(Application.class)
            .web(WebApplicationType.NONE)
            .properties(ADMIN_TASK_PROPERTY + "=true")
            .run(args)) {

      // Force early validation that the Spring environment reached the bridge.
      PropertyUtility.getProperties();
      LOG.info("Starting admin command: " + command);
      invokeMain(COMMANDS.get(command), commandArgs);
    }
  }

  /**
   * Invokes an admin class main method.
   *
   * @param adminClass the admin class
   * @param args the command arguments
   * @throws Exception the exception
   */
  private static void invokeMain(Class<?> adminClass, String[] args)
    throws Exception {
    final Method main = adminClass.getMethod("main", String[].class);
    try {
      main.invoke(null, (Object) args);
    } catch (InvocationTargetException e) {
      final Throwable cause = e.getCause();
      if (cause instanceof Exception) {
        throw (Exception) cause;
      }
      if (cause instanceof Error) {
        throw (Error) cause;
      }
      throw new RuntimeException(cause);
    }
  }

  /**
   * Returns usage text.
   *
   * @return the usage text
   */
  private static String usage() {
    return "Usage: ... " + GeneratorApplication.class.getName()
        + " <command>\nCommands: " + String.join(", ", COMMANDS.keySet());
  }
}
