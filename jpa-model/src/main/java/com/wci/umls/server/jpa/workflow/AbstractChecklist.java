/*
 * Copyright 2022 West Coast Informatics - All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains the property of West Coast Informatics
 * The intellectual and technical concepts contained herein are proprietary to
 * West Coast Informatics and may be covered by U.S. and Foreign Patents, patents in process,
 * and are protected by trade secret or copyright law.  Dissemination of this information
 * or reproduction of this material is strictly forbidden.
 */
package com.wci.umls.server.jpa.workflow;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Fields;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.bridge.builtin.LongBridge;

import com.wci.umls.server.Project;
import com.wci.umls.server.jpa.ProjectJpa;
import com.wci.umls.server.jpa.content.AbstractHasLastModified;
import com.wci.umls.server.jpa.helpers.SplitUnderscoreBridge;
import com.wci.umls.server.model.workflow.Checklist;
import com.wci.umls.server.model.workflow.TrackingRecord;
import com.wci.umls.server.model.workflow.Worklist;

/**
 * Abstract JPA-enabled implementation of a {@link Checklist} or a
 * {@link Worklist}.
 */
@MappedSuperclass
@XmlSeeAlso({
    ChecklistJpa.class, WorklistJpa.class
})
public abstract class AbstractChecklist extends AbstractHasLastModified implements Checklist {

  /** The id. */
  @TableGenerator(name = "EntityIdGenWorkflow", table = "table_generator_wf",
      pkColumnValue = "Entity")
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "EntityIdGenWorkflow")
  private Long id;

  /** The name. */
  @Column(nullable = false, length = 255)
  private String name;

  /** The description. */
  @Column(nullable = false, length = 4000)
  private String description;

  /** The project. */
  @ManyToOne(targetEntity = ProjectJpa.class, optional = false)
  private Project project;

  /**
   * The stats - intended only for JAXB serialization and reporting, not
   * persisted.
   */
  @Transient
  private Map<String, Integer> stats = new HashMap<>();

  /**
   * Instantiates an empty {@link AbstractChecklist}.
   */
  public AbstractChecklist() {
    // do nothing
  }

  /**
   * Instantiates a {@link AbstractChecklist} from the specified parameters.
   *
   * @param checklist the checklist
   * @param collectionCopy the deep copy
   */
  public AbstractChecklist(Checklist checklist, boolean collectionCopy) {
    super(checklist);
    id = checklist.getId();
    timestamp = checklist.getTimestamp();
    lastModified = checklist.getLastModified();
    lastModifiedBy = checklist.getLastModifiedBy();
    name = checklist.getName();
    description = checklist.getDescription();
    project = checklist.getProject();
    stats = new HashMap<>(checklist.getStats());
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
  @Fields({
      @Field(name = "name", index = Index.YES, store = Store.NO, analyze = Analyze.YES,
          analyzer = @Analyzer(definition = "noStopWord"),
          bridge = @FieldBridge(impl = SplitUnderscoreBridge.class)),
      @Field(name = "nameSort", index = Index.YES, analyze = Analyze.NO, store = Store.NO)
  })
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
  @XmlTransient
  public Project getProject() {
    return project;
  }

  /* see superclass */
  @Override
  public void setProject(Project project) {
    this.project = project;
  }

  @XmlElement
  @FieldBridge(impl = LongBridge.class)
  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.NO)
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
  public Map<String, Integer> getStats() {
    if (stats == null) {
      stats = new HashMap<>();
    }
    return stats;
  }

  /* see superclass */
  @Override
  public void setStats(Map<String, Integer> stats) {
    this.stats = stats;
  }

  /**
   * Returns the concept ids. For indexing ONLY.
   *
   * @return the concept ids
   */
  @Field(name = "conceptIds", index = Index.YES, analyze = Analyze.YES, store = Store.NO)
  private String getConceptIds() {
    final StringBuilder sb = new StringBuilder();
    for (final TrackingRecord r : getTrackingRecords()) {
      for (final Long conceptId : r.getOrigConceptIds()) {
        sb.append(conceptId).append(" ");
      }
    }
    return sb.toString();
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((description == null) ? 0 : description.hashCode());
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
    AbstractChecklist other = (AbstractChecklist) obj;
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
    return "AbstractChecklist [id=" + id + ", lastModified=" + getLastModified()
        + ", lastModifiedBy=" + getLastModifiedBy() + ", timestamp=" + getTimestamp() + ", name="
        + name + ", description=" + description + ", project=" + project + "]";
  }

}
