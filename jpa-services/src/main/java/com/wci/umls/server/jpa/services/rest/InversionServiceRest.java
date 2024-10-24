/*
 *    Copyright 2019 West Coast Informatics, LLC
 */
/*
 * 
 */
package com.wci.umls.server.jpa.services.rest;

import com.wci.umls.server.helpers.content.SourceIdRangeList;
import com.wci.umls.server.model.inversion.SourceIdRange;

/**
 * Represents a content available via a REST service.
 */
public interface InversionServiceRest {


  /**
   * Removes the sourceIdRange.
   *
   * @param sourceIdRangeId the sourceIdRange id
   * @param authToken the auth token
   * @throws Exception the exception
   */
  public void removeSourceIdRange(Long sourceIdRangeId, String authToken) throws Exception;


  /**
   * Gets the source id range.
   *
   * @param id the id
   * @param terminology the terminology
   * @param authToken the auth token
   * @return the source id range
   * @throws Exception the exception
   */
  public SourceIdRangeList getSourceIdRange(Long id, String terminology, 
    String authToken) throws Exception;


  /**
   * Request source id range.
   *
   * @param id the id
   * @param terminology the terminology
   * @param numberOfIds the number of ids
   * @param beginSourceId the number of ids
   * @param authToken the auth token
   * @return the source id range
   * @throws Exception the exception
   */
  public SourceIdRange requestSourceIdRange(Long id, String terminology,
   Integer numberOfIds, Long beginSourceId, String authToken) throws Exception;


  /**
   * Update source id range.
   *
   * @param id the id
   * @param terminology the terminology
   * @param numberOfIds the number of ids
   * @param beginSourceId the begin source id
   * @param authToken the auth token
   * @return the source id range
   * @throws Exception the exception
   */
  public SourceIdRange updateSourceIdRange(Long id, String terminology,
    Integer numberOfIds, Long beginSourceId, String authToken) throws Exception;


}
