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
import com.wci.umls.server.model.workflow.Checklist;

/**
 * JPA enabled implementation of {@link Note} connected to a {@link Checklist}.
 * NOTE: the checklist is not exposed through the API, it exists to separate
 * notes by type and avoid a table
 * 
 */
@Entity
@Table(name = "checklist_notes")
//@Audited
@XmlRootElement(name = "checklistNote")
public class ChecklistNoteJpa extends AbstractNote {

  /** The Checklist. */
  @ManyToOne(targetEntity = ChecklistJpa.class, optional = false)
  private Checklist checklist;

  /**
   * The default constructor.
   */
  public ChecklistNoteJpa() {
    // n/a
  }

  /**
   * Instantiates a new note jpa.
   *
   * @param note the note
   */
  public ChecklistNoteJpa(ChecklistNoteJpa note) {
    super(note);
    checklist = note.getChecklist();
  }

  /**
   * Returns the checklist.
   *
   * @return the checklist
   */
  @XmlTransient
  public Checklist getChecklist() {
    return checklist;
  }

  /**
   * Sets the checklist.
   *
   * @param checklist the checklist
   */
  public void setChecklist(Checklist checklist) {
    this.checklist = checklist;
  }

  /**
   * Returns the checklist id.
   *
   * @return the checklist id
   */
  @XmlElement
  @GenericField(searchable = Searchable.YES,
      valueBridge = @ValueBridgeRef(type = ObjectToStringBridge.class))
  @IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "checklist")))
  public Long getChecklistId() {
    return (checklist != null) ? checklist.getId() : 0;
  }

  /**
   * Sets the checklist id.
   *
   * @param checklistId the checklist id
   */
  @SuppressWarnings("unused")
  private void setChecklistId(Long checklistId) {
    if (checklist == null) {
      checklist = new ChecklistJpa();
    }
    checklist.setId(checklistId);
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((getChecklistId() == null) ? 0 : getChecklistId().hashCode());
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
    ChecklistNoteJpa other = (ChecklistNoteJpa) obj;
    if (getChecklistId() == null) {
      if (other.getChecklistId() != null)
        return false;
    } else if (!getChecklistId().equals(other.getChecklistId()))
      return false;
    return true;
  }

  /* see superclass */
  @Override
  public String toString() {
    return "ChecklistNoteJpa [checklist=" + checklist + ", getLastModified()="
        + getLastModified() + ", getLastModifiedBy()=" + getLastModifiedBy()
        + ", getClass()=" + getClass() + ", toString()=" + super.toString()
        + "]";
  }

}
