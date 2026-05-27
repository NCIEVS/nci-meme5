/**
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.services.handlers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.UUID;

import com.wci.umls.server.model.algo.User;
import com.wci.umls.server.model.algo.UserRole;
import com.wci.umls.server.helpers.LocalException;
import com.wci.umls.server.jpa.model.AbstractConfigurable;
import com.wci.umls.server.jpa.model.UserJpa;
import com.wci.umls.server.services.handlers.SecurityServiceHandler;

/**
 * Implements a security handler that authorizes via UTS authentication.
 */
public class UtsSecurityServiceHandler extends AbstractConfigurable
    implements SecurityServiceHandler {

  /** The properties. */
  private Properties properties;

  /**
   * Instantiates an empty {@link UtsSecurityServiceHandler}.
   */
  public UtsSecurityServiceHandler() {
    // do nothing
  }

  /* see superclass */
  @Override
  public User authenticate(String username, String password) throws Exception {

    final String licenseCode = properties.getProperty("license.code");
    if (licenseCode == null) {
      throw new Exception("License code must be specified.");
    }
    final String charset = StandardCharsets.UTF_8.name();
    String data = URLEncoder.encode("licenseCode", charset) + "="
        + URLEncoder.encode(licenseCode, charset);
    data += "&" + URLEncoder.encode("user", charset) + "="
        + URLEncoder.encode(username, charset);
    data += "&" + URLEncoder.encode("password", charset) + "="
        + URLEncoder.encode(password, charset);

    final String urlProp = properties.getProperty("url");
    if (urlProp == null) {
      throw new Exception("URL must be specified.");
    }

    URL url = new URL(urlProp);
    URLConnection conn = url.openConnection();
    conn.setDoOutput(true);
    boolean authenticated = false;
    try (OutputStreamWriter wr =
        new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8)) {
      wr.write(data);
      wr.flush();

      try (BufferedReader rd = new BufferedReader(
          new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
        String line;
        while ((line = rd.readLine()) != null) {
          if (line.toLowerCase().contains("true")) {
            authenticated = true;
          }
        }
      }
    }

    if (!authenticated) {
      throw new LocalException("Username or password invalid.");
    }

    /*
     * Synchronize the information sent back from ITHSDO with the User object.
     * Add a new user if there isn't one matching the username If there is, load
     * and update that user and save the changes
     */
    String authUserName = username;
    String authEmail = "test@example.com";
    String authGivenName = "UTS User - " + username;
    String authSurname = "";

    User returnUser = new UserJpa();
    returnUser.setName(authGivenName + " " + authSurname);
    returnUser.setEmail(authEmail);
    returnUser.setUserName(authUserName);
    returnUser.setApplicationRole(UserRole.VIEWER);
    return returnUser;

  }

  /* see superclass */
  @Override
  public boolean timeoutUser(String user) {
    return true;
  }

  /* see superclass */
  @Override
  public String computeTokenForUser(String user) {
    String token = UUID.randomUUID().toString();
    return token;
  }

  /* see superclass */
  @Override
  public void setProperties(Properties properties) {
    this.properties = properties;
  }

  /* see superclass */
  @Override
  public String getName() {
    return "UTS Security Service Handler";
  }

}
