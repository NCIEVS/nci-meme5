/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.model.content;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

import com.wci.umls.server.helpers.HasLastModified;
import com.wci.umls.server.jpa.model.helpers.DateToIsoFormatBridge;

/**
 * Abstract implementation of {@link HasLastModified} for use with JPA.
 */
//@Audited
@MappedSuperclass
public abstract class AbstractHasLastModified implements HasLastModified {

  /**
   * The id. - leave for subclasses because id generators may need to be
   * different
   */

  /** the timestamp. */
  @Column(nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  @GenericField(searchable = Searchable.YES, sortable = Sortable.YES,
      valueBridge = @ValueBridgeRef(type = DateToIsoFormatBridge.class))
  protected Date timestamp = null;

  /** The last modified. */
  @Column(nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  @GenericField(searchable = Searchable.YES, sortable = Sortable.YES,
      valueBridge = @ValueBridgeRef(type = DateToIsoFormatBridge.class))
  protected Date lastModified = null;

  /** The last modified. */
  @Column(nullable = false)
  @KeywordField(searchable = Searchable.YES)
  protected String lastModifiedBy;

  /**
   * Instantiates an empty {@link AbstractHasLastModified}.
   */
  public AbstractHasLastModified() {
    // do nothing
  }

  /**
   * Instantiates a {@link AbstractHasLastModified} from the specified
   * parameters.
   *
   * @param component the component
   */
  public AbstractHasLastModified(HasLastModified component) {
    setId(component.getId());
    timestamp = component.getTimestamp();
    lastModified = component.getLastModified();
    lastModifiedBy = component.getLastModifiedBy();
  }

  /* see superclass */
  @Override
  public Date getTimestamp() {
    return timestamp;
  }

  /* see superclass */
  @Override
  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  /* see superclass */
  @Override
  public Date getLastModified() {
    return lastModified;
  }

  /**
   * Returns the last modified in yyyymmdd format.
   *
   * @XmlTransient
   * @Field(name = "lastModifiedYYYYMMDD", index = Index.YES, analyze =
   *             Analyze.NO, store = Store.NO) public String
   *             getLastModifiedYYYYMMDD() { return lastModified == null ? null
   *             : ConfigUtility.DATE_FORMAT.format(lastModified); }
   */

  /* see superclass */
  @Override
  public void setLastModified(Date lastModified) {
    this.lastModified = lastModified;
  }

  /* see superclass */
  @Override
  public String getLastModifiedBy() {
    return lastModifiedBy;
  }

  /* see superclass */
  @Override
  public void setLastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }

}