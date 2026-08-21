/*
 *    Copyright 2026 West Coast Informatics, LLC
 */
package com.wci.umls.server.rest.impl;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Routes Angular UI20 entry URLs back to the packaged index page.
 */
public class Ui20RoutingFilter implements Filter {

  /** UI20 root path. */
  private static final String UI20_ROOT = "/ui20";

  /** UI20 index path. */
  private static final String UI20_INDEX = "/ui20/index.html";

  /* see superclass */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response,
    FilterChain chain) throws IOException, ServletException {

    final HttpServletRequest httpRequest = (HttpServletRequest) request;
    final String contextPath = httpRequest.getContextPath();
    final String requestUri = httpRequest.getRequestURI();
    final String path = requestUri.substring(contextPath.length());

    if (UI20_ROOT.equals(path)) {
      ((HttpServletResponse) response).sendRedirect(contextPath + UI20_ROOT + "/");
      return;
    }

    if (shouldForwardToIndex(path)) {
      request.getRequestDispatcher(UI20_INDEX).forward(request, response);
      return;
    }

    chain.doFilter(request, response);
  }

  /**
   * Indicates whether the path should be forwarded to the UI20 index.
   *
   * @param path the context-relative request path
   * @return true if this is an Angular entry or route path
   */
  private boolean shouldForwardToIndex(String path) {
    if (!path.startsWith(UI20_ROOT + "/") || UI20_INDEX.equals(path)) {
      return false;
    }

    final String routePath = path.substring((UI20_ROOT + "/").length());
    return routePath.isEmpty() || !lastPathSegment(routePath).contains(".");
  }

  /**
   * Returns the last path segment.
   *
   * @param path the path
   * @return the last path segment
   */
  private String lastPathSegment(String path) {
    final int index = path.lastIndexOf('/');
    return index >= 0 ? path.substring(index + 1) : path;
  }

  /* see superclass */
  @Override
  public void destroy() {
    // do nothing
  }

  /* see superclass */
  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // do nothing
  }
}
