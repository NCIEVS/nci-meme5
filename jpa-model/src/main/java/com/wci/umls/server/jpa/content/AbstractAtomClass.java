/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.content;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import jakarta.xml.bind.annotation.XmlElement;

import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

import com.wci.umls.server.helpers.Branch;
import com.wci.umls.server.jpa.helpers.ObjectToStringBridge;
import com.wci.umls.server.model.content.AtomClass;
import com.wci.umls.server.model.workflow.WorkflowStatus;

/**
 * Abstract JPA and JAXB enabled implementation of {@link AtomClass}.
 */
@MappedSuperclass
public abstract class AbstractAtomClass extends AbstractComponent
    implements AtomClass {

  /** The name. */
  @Column(nullable = false, length = 4000)
  private String name;

  /** branched to tracking. */
  @Column(nullable = true)
  private String branchedTo;

  /** The workflow status. */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private WorkflowStatus workflowStatus;

  /**
   * Instantiates an empty {@link AbstractAtomClass}.
   */
  public AbstractAtomClass() {
    // do nothing
  }

  /**
   * Instantiates a {@link AbstractAtomClass} from the specified parameters.
   *
   * @param atomClass the atom
   * @param collectionCopy the deep copy
   */
  public AbstractAtomClass(AtomClass atomClass, boolean collectionCopy) {
    super(atomClass);
    name = atomClass.getName();
    workflowStatus = atomClass.getWorkflowStatus();
    branchedTo = atomClass.getBranchedTo();
  }

  /* see superclass */
  @Override
  @FullTextField(analyzer = "noStopWord")
  @KeywordField(name = "nameSort", sortable = Sortable.YES)
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
  @GenericField(searchable = Searchable.YES,
      valueBridge = @ValueBridgeRef(type = ObjectToStringBridge.class))
  public WorkflowStatus getWorkflowStatus() {
    return workflowStatus;
  }

  /* see superclass */
  @Override
  public void setWorkflowStatus(WorkflowStatus workflowStatus) {
    this.workflowStatus = workflowStatus;

  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((name == null) ? 0 : name.hashCode());
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
    AbstractAtomClass other = (AbstractAtomClass) obj;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    return true;
  }

  /* see superclass */
  @Override
  @KeywordField(searchable = Searchable.YES)
  public String getBranchedTo() {
    return branchedTo;
  }

  /* see superclass */
  @Override
  public void setBranchedTo(String branchedTo) {
    this.branchedTo = branchedTo;
  }

  /* see superclass */
  @Override
  public void addBranchedTo(String newBranch) {
    if (newBranch.indexOf(Branch.SEPARATOR) != -1) {
      throw new IllegalArgumentException(
          "New branches may not have a comma in them.");
    }
    branchedTo += newBranch + Branch.SEPARATOR;
  }

  /* see superclass */
  @Override
  public void removeBranchedTo(String closedBranch) {
    if (closedBranch.indexOf(Branch.SEPARATOR) != -1) {
      throw new IllegalArgumentException(
          "New branches may not have a comma in them.");
    }
    final int index = branchedTo.indexOf(closedBranch);
    if (index != -1) {
      branchedTo = branchedTo.substring(0, index - 1)
          + branchedTo.substring(index + closedBranch.length() + 1);
    }

  }

  /* see superclass */
  @Override
  public String toString() {
    return getClass().getSimpleName() + " [" + super.toString() + ", name="
        + name + "]";
  }

}