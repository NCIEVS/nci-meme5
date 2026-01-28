/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.helpers;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

import com.wci.umls.server.helpers.LogEntry;

/**
 * The JPA and JAXB enabled implementation of the log entry object.
 */
@Entity
@Table(name = "log_entries")
@Indexed
@XmlRootElement(name = "logEntry")
public class LogEntryJpa implements LogEntry {

  /** The id. */
  @TableGenerator(name = "EntityIdGen", table = "table_generator", pkColumnValue = "Entity")
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "EntityIdGen")
  private Long id;

  /** The last modified. */
  @Column(nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  @GenericField(searchable = Searchable.YES, sortable = Sortable.YES)
  private Date lastModified = new Date();

  /** The last modified. */
  @Column(nullable = false)
  @KeywordField(searchable = Searchable.YES)
  private String lastModifiedBy;

  /** The message. */
  @Column(nullable = false, length = 4000)
  @FullTextField()
  private String message;

  /** The object id. */
  @Column(nullable = true)
  @GenericField(searchable = Searchable.YES)
  private Long objectId;

  /** The project id. */
  @Column(nullable = true)
  @GenericField(searchable = Searchable.YES)
  private Long projectId;

  /** The terminology. */
  @Column(nullable = true)
  @KeywordField(searchable = Searchable.YES)
  private String terminology;

  /** The version. */
  @Column(nullable = true)
  @KeywordField(searchable = Searchable.YES)
  private String version;

  /** The from id type. */
  @Column(nullable = true)
  @KeywordField(searchable = Searchable.YES)
  private String activityId;

  /** The from id type. */
  @Column(nullable = true)
  @KeywordField(searchable = Searchable.YES)
  private String workId;

  /** the timestamp. */
  @Column(nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date timestamp = null;

  /**
   * The default constructor.
   */

  public LogEntryJpa() {
    // do nothing
  }

  /**
   * Instantiates a {@link LogEntryJpa} from the specified parameters.
   *
   * @param logEntry the log entry
   */
  public LogEntryJpa(LogEntry logEntry) {
    id = logEntry.getId();
    lastModified = logEntry.getLastModified();
    lastModifiedBy = logEntry.getLastModifiedBy();
    message = logEntry.getMessage();
    objectId = logEntry.getObjectId();
    projectId = logEntry.getProjectId();
    terminology = logEntry.getTerminology();
    version = logEntry.getVersion();
    activityId = logEntry.getActivityId();
    workId = logEntry.getWorkId();
    timestamp = logEntry.getTimestamp();
  }

  /* see superclass */
  @Override
  public Long getId() {
    return this.id;
  }

  /* see superclass */
  @Override
  public void setId(Long id) {
    this.id = id;
  }

  /* see superclass */
  @Override
  public Date getLastModified() {
    return lastModified;
  }

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

  /* see superclass */
  @Override
  public String getMessage() {
    return message;
  }

  /* see superclass */
  @Override
  public void setMessage(String message) {
    if (message.length() > 4000) {
      this.message = message.substring(0, 4000);
    } else {
      this.message = message;
    }
  }

  /* see superclass */
  @Override
  public Long getObjectId() {
    return objectId;
  }

  /* see superclass */
  @Override
  public void setObjectId(Long objectId) {
    this.objectId = objectId;
  }

  /* see superclass */
  @Override
  public Long getProjectId() {
    return projectId;
  }

  /* see superclass */
  @Override
  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  /* see superclass */
  @Override
  public String getActivityId() {
    return activityId;
  }

  /* see superclass */
  @Override
  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

  /* see superclass */
  @Override
  public String getWorkId() {
    return workId;
  }

  /* see superclass */
  @Override
  public void setWorkId(String workId) {
    this.workId = workId;
  }

  /* see superclass */
  @Override
  public String getVersion() {
    return version;
  }

  /* see superclass */
  @Override
  public void setVersion(String version) {
    this.version = version;
  }

  /* see superclass */
  @Override
  public String getTerminology() {
    return terminology;
  }

  /* see superclass */
  @Override
  public void setTerminology(String terminology) {
    this.terminology = terminology;
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime * result + ((activityId == null) ? 0 : activityId.hashCode());
    result = prime * result + ((workId == null) ? 0 : workId.hashCode());
    result = prime * result + ((message == null) ? 0 : message.hashCode());
    result = prime * result + ((objectId == null) ? 0 : objectId.hashCode());
    result = prime * result + ((projectId == null) ? 0 : projectId.hashCode());
    result =
        prime * result + ((terminology == null) ? 0 : terminology.hashCode());
    result = prime * result + ((version == null) ? 0 : version.hashCode());
    return result;
  }

  /* see superclass */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    LogEntryJpa other = (LogEntryJpa) obj;
    if (message == null) {
      if (other.message != null)
        return false;
    } else if (!message.equals(other.message))
      return false;
    if (activityId == null) {
      if (other.activityId != null)
        return false;
    } else if (!activityId.equals(other.activityId))
      return false;
    if (workId == null) {
      if (other.workId != null)
        return false;
    } else if (!workId.equals(other.workId))
      return false;
    if (objectId == null) {
      if (other.objectId != null)
        return false;
    } else if (!objectId.equals(other.objectId))
      return false;
    if (projectId == null) {
      if (other.projectId != null)
        return false;
    } else if (!projectId.equals(other.projectId))
      return false;
    if (terminology == null) {
      if (other.terminology != null)
        return false;
    } else if (!terminology.equals(other.terminology))
      return false;
    if (version == null) {
      if (other.version != null)
        return false;
    } else if (!version.equals(other.version))
      return false;
    return true;
  }

  /* see superclass */
  @Override
  public String toString() {
    return "LogEntryJpa [id=" + id + ", lastModified=" + lastModified
        + ", lastModifiedBy=" + lastModifiedBy + ", message=" + message
        + ", objectId=" + objectId + ", projectId=" + projectId
        + ", terminology=" + terminology + ", version=" + version
        + ", activityId=" + activityId + ", workId=" + workId + ", timestamp="
        + timestamp + "]";
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

}
