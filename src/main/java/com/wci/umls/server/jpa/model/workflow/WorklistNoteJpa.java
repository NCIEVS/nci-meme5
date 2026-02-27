/**
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.model.workflow;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;

import com.wci.umls.server.jpa.model.helpers.ObjectToStringBridge;
import com.wci.umls.server.helpers.Note;
import com.wci.umls.server.jpa.model.content.AbstractNote;
import com.wci.umls.server.model.workflow.Worklist;

/**
 * JPA enabled implementation of {@link Note} connected to a {@link Worklist}.
 * NOTE: the worklist is not exposed through the API, it exists to separate
 * notes by type and avoid a table
 * 
 */
@Entity
@Table(name = "worklist_notes")
//@Audited
@XmlRootElement(name = "worklistNote")
public class WorklistNoteJpa extends AbstractNote {

  /** The Worklist. */
  @ManyToOne(targetEntity = WorklistJpa.class, optional = false)
  private Worklist worklist;

  /**
   * The default constructor.
   */
  public WorklistNoteJpa() {
    // n/a
  }

  /**
   * Instantiates a new note jpa.
   *
   * @param note the note
   */
  public WorklistNoteJpa(WorklistNoteJpa note) {
    super(note);
    worklist = note.getWorklist();
  }

  /**
   * Returns the worklist.
   *
   * @return the worklist
   */
  @XmlTransient
  public Worklist getWorklist() {
    return worklist;
  }

  /**
   * Sets the worklist.
   *
   * @param worklist the worklist
   */
  public void setWorklist(Worklist worklist) {
    this.worklist = worklist;
  }

  /**
   * Returns the worklist id.
   *
   * @return the worklist id
   */
  @XmlElement
  @GenericField(searchable = Searchable.YES,
      valueBridge = @ValueBridgeRef(type = ObjectToStringBridge.class))
  @IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "worklist")))
  public Long getWorklistId() {
    return (worklist != null) ? worklist.getId() : 0;
  }

  /**
   * Sets the worklist id.
   *
   * @param worklistId the worklist id
   */
  @SuppressWarnings("unused")
  private void setWorklistId(Long worklistId) {
    if (worklist == null) {
      worklist = new WorklistJpa();
    }
    worklist.setId(worklistId);
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result
        + ((getWorklistId() == null) ? 0 : getWorklistId().hashCode());
    return result;
  }

  /* see superclass */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    WorklistNoteJpa other = (WorklistNoteJpa) obj;
    if (getWorklistId() == null) {
      if (other.getWorklistId() != null)
        return false;
    } else if (!getWorklistId().equals(other.getWorklistId()))
      return false;
    return true;
  }

  /* see superclass */
  @Override
  public String toString() {
    return "WorklistNoteJpa [worklist=" + worklist + ", getLastModified()="
        + getLastModified() + ", getLastModifiedBy()=" + getLastModifiedBy()
        + ", getClass()=" + getClass() + ", toString()=" + super.toString()
        + "]";
  }

}
