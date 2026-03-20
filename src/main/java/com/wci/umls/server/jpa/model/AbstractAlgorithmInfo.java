/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


import com.wci.umls.server.jpa.model.helpers.ObjectToStringBridge;
import com.wci.umls.server.model.algo.AlgorithmInfo;
import com.wci.umls.server.model.algo.AlgorithmParameter;
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
import jakarta.persistence.Transient;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlTransient;

import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;


/**
 * JPA and JAXB enabled implementation of {@link AlgorithmInfo}.
 * @param <T> the process info type (e.g. config or execution)
 */
//@Audited
@MappedSuperclass
@XmlSeeAlso({
    AlgorithmConfigJpa.class, AlgorithmExecutionJpa.class
})
public abstract class AbstractAlgorithmInfo<T extends ProcessInfo<?>>
    implements AlgorithmInfo<T> {

  /** The id. */
  @TableGenerator(name = "EntityIdGenProcess", table = "table_generator_process", pkColumnValue = "Entity")
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "EntityIdGenProcess")
  private Long id;

  /** The last modified. */
  @Column(nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastModified;

  /** The last modified. */
  @Column(nullable = false)
  private String lastModifiedBy;

  /** The last modified. */
  @Column(nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date timestamp = new Date();

  /** The name. */
  @Column(nullable = false)
  private String name;

  /** The description. */
  @Column(nullable = false)
  private String description;

  /** The algorithm key. */
  @Column(nullable = false)
  private String algorithmKey;

  /** The project. */
  @ManyToOne(targetEntity = ProjectJpa.class, optional = false)
  private Project project;

  /** parameters. */
  @Transient
  private List<AlgorithmParameter> parameters = new ArrayList<>();

  /**
   * Instantiates an empty {@link AbstractAlgorithmInfo}.
   */
  public AbstractAlgorithmInfo() {
    // n/a
  }

  /**
   * Instantiates a {@link AbstractAlgorithmInfo} from the specified parameters.
   *
   * @param info the config
   */
  public AbstractAlgorithmInfo(AlgorithmInfo<?> info) {
    id = info.getId();
    timestamp = info.getTimestamp();
    lastModified = info.getLastModified();
    lastModifiedBy = info.getLastModifiedBy();
    name = info.getName();
    description = info.getDescription();
    project = info.getProject();
    parameters = new ArrayList<>(info.getParameters());
    algorithmKey = info.getAlgorithmKey();

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
  public String getAlgorithmKey() {
    return algorithmKey;
  }

  /* see superclass */
  @Override
  public void setAlgorithmKey(String algorithmKey) {
    this.algorithmKey = algorithmKey;

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
  @XmlElement(type = AlgorithmParameterJpa.class)
  public List<AlgorithmParameter> getParameters() {
    if (parameters == null) {
      parameters = new ArrayList<>();
    }
    return parameters;
  }

  /* see superclass */
  @Override
  public void setParameters(List<AlgorithmParameter> parameters) {
    this.parameters = parameters;
  }


  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime * result + ((algorithmKey == null) ? 0 : algorithmKey.hashCode());
    result =
        prime * result + ((description == null) ? 0 : description.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result
        + ((getProjectId() == null) ? 0 : getProjectId().hashCode());
   

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
    AbstractAlgorithmInfo other = (AbstractAlgorithmInfo) obj;
    if (algorithmKey == null) {
      if (other.algorithmKey != null)
        return false;
    } else if (!algorithmKey.equals(other.algorithmKey))
      return false;
    if (description == null) {
      if (other.description != null)
        return false;
    } else if (!description.equals(other.description))
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
   
    return true;
  }

  @Override
  public String toString() {
    return "AbstractAlgorithmInfo [id=" + id + ", lastModified=" + lastModified
        + ", lastModifiedBy=" + lastModifiedBy + ", timestamp=" + timestamp
        + ", name=" + name + ", description=" + description + ", algorithmKey="
        + algorithmKey + "]";
  }

}
