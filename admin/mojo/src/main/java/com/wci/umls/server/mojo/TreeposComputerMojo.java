/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.mojo;

import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.jpa.services.SecurityServiceJpa;
import com.wci.umls.server.rest.client.ContentClientRest;
import com.wci.umls.server.rest.impl.ContentServiceRestImpl;
import com.wci.umls.server.services.SecurityService;

/**
 * Goal which loads an RF2 Snapshot of SNOMED CT data into a database.
 * 
 * See admin/remover/pom.xml for a sample execution.
 */
@Mojo(name = "compute-tree-positions", defaultPhase = LifecyclePhase.PACKAGE)
public class TreeposComputerMojo extends AbstractMojo {

  /**
   * Name of terminology to be loaded.
   */
  @Parameter
  private String terminology;

  /**
   * The version.
   */
  @Parameter
  private String version;

  /**
   * Whether to run this mojo against an active server.
   */
  @Parameter
  private boolean server = false;

  /**
   * Instantiates a {@link TreeposComputerMojo} from the specified parameters.
   * 
   */
  public TreeposComputerMojo() {
    // do nothing
  }

  /* see superclass */
  @Override
  public void execute() throws MojoFailureException {
    getLog().info("Starting computation of tree positions");
    getLog().info("  terminology = " + terminology);
    getLog().info("  version = " + version);

    try {

      Properties properties = ConfigUtility.getConfigProperties();

      boolean serverRunning = ConfigUtility.isServerActive();

      getLog()
          .info("Server status detected:  " + (!serverRunning ? "DOWN" : "UP"));

      if (serverRunning && !server) {
        throw new MojoFailureException(
            "Mojo expects server to be down, but server is running");
      }

      if (!serverRunning && server) {
        throw new MojoFailureException(
            "Mojo expects server to be running, but server is down");
      }

      // authenticate
      SecurityService service = new SecurityServiceJpa();
      String authToken =
          service.authenticate(properties.getProperty("admin.user"),
              properties.getProperty("admin.password")).getAuthToken();
      service.close();

      if (!serverRunning) {
        getLog().info("Running directly");

        ContentServiceRestImpl contentService = new ContentServiceRestImpl();
        contentService.computeTreePositions(terminology, version, authToken);
      } else {
        getLog().info("Running against server");

        // invoke the client
        ContentClientRest client = new ContentClientRest(properties);
        client.computeTreePositions(terminology, version, authToken);
      }

      // Clean-up
    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoFailureException("Unexpected exception:", e);
    }
  }
}
