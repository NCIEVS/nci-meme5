/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.model;

import java.util.Date;

import com.wci.umls.server.jpa.model.helpers.ObjectToStringBridge;
import com.wci.umls.server.model.algo.AlgorithmInfo;
import com.wci.umls.server.model.algo.ProcessInfo;
import com.wci.umls.server.model.algo.Project;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlTransient;

import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;

/**
 * JPA and JAXB enabled implementation of {@link ProcessInfo}.
 * @param <T> the algorithm info type (e.g. config or execution)
 */
//@Audited
@MappedSuperclass
@XmlSeeAlso({
    ProcessConfigJpa.class, ProcessExecutionJpa.class
})
public abstract class AbstractProcessInfo<T extends AlgorithmInfo<?>>
    implements ProcessInfo<T> {

  /** The id. */
  @TableGenerator(name = "EntityIdGenProcess", table = "table_generator_process", pkColumnValue = "Entity")
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "EntityIdGenProcess")
  private Long id;

  /** The last modified. */
  @Column(nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  @GenericField(searchable = Searchable.YES, sortable = Sortable.YES)
  private Date lastModified;

  /** The last modified. */
  @Column(nullable = false)
  @KeywordField(searchable = Searchable.YES)
  private String lastModifiedBy;

  /** The last modified. */
  @Column(nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date timestamp = new Date();

  /** The name. */
  @Column(nullable = false)
  @FullTextField()
  @KeywordField(name = "nameSort", sortable = Sortable.YES)
  private String name;

  /** The description. */
  @Column(nullable = false)
  @FullTextField()
  private String description;

  /** The terminology. */
  @Column(nullable = false)
  @KeywordField(searchable = Searchable.YES)
  private String terminology;

  /** The version. */
  @Column(nullable = false)
  @KeywordField(searchable = Searchable.YES)
  private String version;

  /** The project. */
  @ManyToOne(targetEntity = ProjectJpa.class, optional = false)
  private Project project;

  /** The feedback email. */
  @Column(nullable = true)
  private String feedbackEmail;

  /**
   * Instantiates an empty {@link AbstractProcessInfo}.
   */
  public AbstractProcessInfo() {
    // n/a
  }

  /**
   * Instantiates a {@link AbstractProcessInfo} from the specified parameters.
   *
   * @param info the config
   */
  public AbstractProcessInfo(ProcessInfo<?> info) {
    id = info.getId();
    timestamp = info.getTimestamp();
    lastModified = info.getLastModified();
    lastModifiedBy = info.getLastModifiedBy();
    name = info.getName();
    description = info.getDescription();
    feedbackEmail = info.getFeedbackEmail();
    project = info.getProject();
    terminology = info.getTerminology();
    version = info.getVersion();
  }

  /* see superclass */
  @Override
  public Long getId() {
    return id;
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
  public String getName() {
    return name;
  }

  /* see superclass */
  @Override
  public void setName(String name) {
    this.name = name;
  }

  /* see superclass */
  @Override
  public String getDescription() {
    return description;
  }

  /* see superclass */
  @Override
  public void setDescription(String description) {
    this.description = description;
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
  @XmlTransient
  public Project getProject() {
    return project;
  }

  /* see superclass */
  @Override
  public void setProject(Project project) {
    this.project = project;
  }

  /**
   * Returns the project id. For JPA and JAXB.
   *
   * @return the project id
   */
  @GenericField(searchable = Searchable.YES,
          valueBridge = @ValueBridgeRef(type = ObjectToStringBridge.class))
  @IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "project")))
  public Long getProjectId() {
    return project == null ? null : project.getId();
  }

  /**
   * Sets the project id.
   *
   * @param projectId the project id
   */
  public void setProjectId(Long projectId) {
    if (project == null) {
      project = new ProjectJpa();
    }
    project.setId(projectId);
  }

  /* see superclass */
  @Override
  public String getFeedbackEmail() {
    return feedbackEmail;
  }

  /* see superclass */
  @Override
  public void setFeedbackEmail(String feedbackEmail) {
    this.feedbackEmail = feedbackEmail;
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime * result + ((description == null) ? 0 : description.hashCode());
    result = prime * result
        + ((feedbackEmail == null) ? 0 : feedbackEmail.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result
        + ((getProjectId() == null) ? 0 : getProjectId().hashCode());
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
    @SuppressWarnings("rawtypes")
    AbstractProcessInfo other = (AbstractProcessInfo) obj;
    if (description == null) {
      if (other.description != null)
        return false;
    } else if (!description.equals(other.description))
      return false;
    if (feedbackEmail == null) {
      if (other.feedbackEmail != null)
        return false;
    } else if (!feedbackEmail.equals(other.feedbackEmail))
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (getProjectId() == null) {
      if (other.getProjectId() != null)
        return false;
    } else if (!getProjectId().equals(other.getProjectId()))
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
    return "AbstractProcessInfo [id=" + id + ", lastModified=" + lastModified
        + ", lastModifiedBy=" + lastModifiedBy + ", timestamp=" + timestamp
        + ", name=" + name + ", description=" + description + ", terminology="
        + terminology + ", version=" + version + ", project=" + getProjectId()
        + ", feedbackEmail=" + feedbackEmail + "]";
  }

}
