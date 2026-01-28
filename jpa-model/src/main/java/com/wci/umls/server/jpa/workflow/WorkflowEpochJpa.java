package com.wci.umls.server.jpa.workflow;

import java.util.Date;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.UniqueConstraint;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;

import com.wci.umls.server.jpa.helpers.ObjectToStringBridge;

import com.wci.umls.server.Project;
import com.wci.umls.server.jpa.ProjectJpa;
import com.wci.umls.server.model.workflow.WorkflowBin;
import com.wci.umls.server.model.workflow.WorkflowEpoch;

/**
 * JAXB and JPA enabled implementation of a {@link WorkflowEpoch}.
 */
@Entity
@Table(name = "workflow_epochs", uniqueConstraints = @UniqueConstraint(columnNames = {
    "name", "project_id"
}))
@Indexed
@XmlRootElement(name = "workflowEpoch")
public class WorkflowEpochJpa implements WorkflowEpoch {

  /** The id. */
  @TableGenerator(name = "EntityIdGenWorkflow", table = "table_generator_wf", pkColumnValue = "Entity")
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "EntityIdGenWorkflow")
  private Long id;

  /** The last modified. */
  @Column(nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastModified = new Date();

  /** The last modified. */
  @Column(nullable = false)
  private String lastModifiedBy;

  /** The timestamp. */
  @Column(nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date timestamp = null;

  /** The name. */
  @Column(nullable = false)
  private String name;

  /** The active. */
  @Column(nullable = false)
  private boolean active;

  /** The workflow bins. */
  @OneToMany(targetEntity = WorkflowBinJpa.class)
  @IndexedEmbedded(targetType = WorkflowBinJpa.class, includeEmbeddedObjectId = true)
  @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
  @CollectionTable(name = "workflow_epochs_workflow_bins", joinColumns = @JoinColumn(name = "workflow_epochs_id"))
  private List<WorkflowBin> workflowBins = null;

  /** The project. */
  @ManyToOne(targetEntity = ProjectJpa.class, optional = false)
  private Project project;

  /**
   * Instantiates an empty {@link WorkflowEpochJpa}.
   */
  public WorkflowEpochJpa() {
    // do nothing
  }

  /**
   * Instantiates a {@link WorkflowEpochJpa} from the specified parameters.
   *
   * @param epoch the workflow epoch
   */
  public WorkflowEpochJpa(WorkflowEpoch epoch) {
    id = epoch.getId();
    lastModified = epoch.getLastModified();
    lastModifiedBy = epoch.getLastModifiedBy();
    timestamp = epoch.getTimestamp();
    name = epoch.getName();
    active = epoch.isActive();
    project = epoch.getProject();
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

  /* see superclass */
  @Override
  public void setLastModified(Date lastModified) {
    this.lastModified = lastModified;
  }

  /* see superclass */
  @Override
  @KeywordField(searchable = Searchable.YES)
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
  @KeywordField(searchable = Searchable.YES)
  public String getName() {
    return name;
  }

  /* see superclass */
  @Override
  public void setName(String name) {
    this.name = name;
  }

  /* see superclass */
  @GenericField(searchable = Searchable.YES,
      valueBridge = @ValueBridgeRef(type = ObjectToStringBridge.class))
  @Override
  public boolean isActive() {
    return active;
  }

  /* see superclass */
  @Override
  public void setActive(boolean active) {
    this.active = active;
  }

  /* see superclass */
  @XmlTransient
  @Override
  public Project getProject() {
    return project;
  }

  /* see superclass */
  @Override
  public void setProject(Project project) {
    this.project = project;
  }

  /**
   * Returns the project id.
   *
   * @return the project id
   */
  @GenericField(searchable = Searchable.YES)
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
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (active ? 1231 : 1237);
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((project == null) ? 0 : project.hashCode());
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
    WorkflowEpochJpa other = (WorkflowEpochJpa) obj;
    if (active != other.active)
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (project == null) {
      if (other.project != null)
        return false;
    } else if (!project.equals(other.project))
      return false;
    return true;
  }

  /* see superclass */
  @Override
  public String toString() {
    return "WorkflowEpochJpa [lastModified=" + lastModified
        + ", lastModifiedBy=" + lastModifiedBy + ", timestamp=" + timestamp
        + ", name=" + name + ", active=" + active + "]";
  }

}
