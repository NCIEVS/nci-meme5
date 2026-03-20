/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.model.actions;

import com.wci.umls.server.jpa.model.helpers.ObjectToStringBridge;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.UniqueConstraint;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;

import com.wci.umls.server.model.actions.AtomicAction;
import com.wci.umls.server.model.actions.MolecularAction;
import com.wci.umls.server.model.meta.IdType;

/**
 * JPA and JAXB enabled implementation of a {@link AtomicAction}.
 */
@Entity
@Table(name = "atomic_actions", uniqueConstraints = @UniqueConstraint(columnNames = {
    "objectId", "id"
}))
@Indexed
@XmlRootElement(name = "atomicActions")
public class AtomicActionJpa implements AtomicAction {

  /** The id. */
  @TableGenerator(name = "EntityIdGenAction", table = "table_generator_action", pkColumnValue = "Entity")
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "EntityIdGenAction")
  @JoinColumn(nullable = false)
  private Long id;

  /** The object id. */
  @Column(nullable = false)
  @GenericField(searchable = Searchable.YES,
      valueBridge = @ValueBridgeRef(type = ObjectToStringBridge.class))
  private Long objectId;

  /** The old value. */
  @Column(nullable = true)
  @KeywordField(searchable = Searchable.YES)
  @KeywordField(name = "oldValueSort", sortable = Sortable.YES)
  private String oldValue;

  /** The new value. */
  @Column(nullable = true)
  @KeywordField(searchable = Searchable.YES)
  private String newValue;

  /** The field. */
  @Column(nullable = false)
  @KeywordField(searchable = Searchable.YES)
  @KeywordField(name = "fieldSort", sortable = Sortable.YES)
  private String field;

  /** The type. */
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  @GenericField(searchable = Searchable.YES, valueBridge = @ValueBridgeRef(type = ObjectToStringBridge.class))
  @KeywordField(name = "idTypeSort", sortable = Sortable.YES)
  private IdType idType;

  /** The class name. */
  @Column(nullable = false)
  @KeywordField(searchable = Searchable.YES)
  private String className;

  /** The collection class name. */
  @Column(nullable = true)
  private String collectionClassName;

  /** The molecular action. */
  @ManyToOne(targetEntity = MolecularActionJpa.class, optional = false)
  @JoinColumn(nullable = false)
  private MolecularAction molecularAction;

  /**
   * Instantiates a new atomic action jpa.
   */
  public AtomicActionJpa() {
    // do nothing
  }

  /**
   * Instantiates a new atomic action jpa.
   *
   * @param action the atomic action
   */
  public AtomicActionJpa(AtomicAction action) {
    super();
    id = action.getId();
    oldValue = action.getOldValue();
    newValue = action.getNewValue();
    field = action.getField();
    idType = action.getIdType();
    className = action.getClassName();
    collectionClassName = action.getCollectionClassName();
    objectId = action.getObjectId();
    molecularAction = action.getMolecularAction();
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

  /*
   * see superclass
   */
  @Override
  // Simply transient, no need to refer the id back - never needed for
  // serialization
  @XmlTransient
  public MolecularAction getMolecularAction() {
    return molecularAction;
  }

  /* see superclass */
  @Override
  public void setMolecularAction(MolecularAction molecularAction) {
    this.molecularAction = molecularAction;
  }

  /**
   * Returns the molecular action id. For Lucene and JAXB.
   *
   * @return the molecular action id
   */
  @GenericField(searchable = Searchable.YES,
      valueBridge = @ValueBridgeRef(type = ObjectToStringBridge.class))
  @IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "molecularAction")))
  public Long getMolecularActionId() {
    return molecularAction == null ? null : molecularAction.getId();
  }

  /**
   * Sets the molecular action id. For JAXB.
   *
   * @param molecularActionId the molecular action id
   */
  public void setMolecularActionId(Long molecularActionId) {
    if (molecularAction == null) {
      molecularAction = new MolecularActionJpa();
    }
    molecularAction.setId(molecularActionId);
  }

  /* see superclass */
  @Override
  public IdType getIdType() {
    return idType;
  }

  /* see superclass */
  @Override
  public void setIdType(IdType idType) {
    this.idType = idType;
  }

  /* see superclass */
  @Override
  public String getClassName() {
    return className;
  }

  /* see superclass */
  @Override
  public void setClassName(String className) {
    this.className = className;
  }

  /* see superclass */
  @Override
  public String getCollectionClassName() {
    return collectionClassName;
  }

  /* see superclass */
  @Override
  public void setCollectionClassName(String collectionClassName) {
    this.collectionClassName = collectionClassName;
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
  public String getField() {
    return field;
  }

  /* see superclass */
  @Override
  public void setField(String field) {
    this.field = field;
  }

  /* see superclass */
  @Override
  public String getOldValue() {
    return oldValue;
  }

  /* see superclass */
  @Override
  public void setOldValue(String oldValue) {
    this.oldValue = oldValue;
  }

  /* see superclass */
  @Override
  public String getNewValue() {
    return newValue;
  }

  /* see superclass */
  @Override
  public void setNewValue(String newValue) {
    this.newValue = newValue;
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((field == null) ? 0 : field.hashCode());
    result = prime * result + ((idType == null) ? 0 : idType.hashCode());
    result = prime * result + ((className == null) ? 0 : className.hashCode());
    result = prime * result
        + ((collectionClassName == null) ? 0 : collectionClassName.hashCode());
    result = prime * result + ((newValue == null) ? 0 : newValue.hashCode());
    result = prime * result + ((objectId == null) ? 0 : objectId.hashCode());
    result = prime * result + ((oldValue == null) ? 0 : oldValue.hashCode());
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
    AtomicActionJpa other = (AtomicActionJpa) obj;
    if (field == null) {
      if (other.field != null)
        return false;
    } else if (!field.equals(other.field))
      return false;
    if (idType != other.idType)
      return false;
    if (className == null) {
      if (other.className != null)
        return false;
    } else if (!className.equals(other.className))
      return false;
    if (collectionClassName == null) {
      if (other.collectionClassName != null)
        return false;
    } else if (!collectionClassName.equals(other.collectionClassName))
      return false;
    if (newValue == null) {
      if (other.newValue != null)
        return false;
    } else if (!newValue.equals(other.newValue))
      return false;
    if (objectId == null) {
      if (other.objectId != null)
        return false;
    } else if (!objectId.equals(other.objectId))
      return false;
    if (oldValue == null) {
      if (other.oldValue != null)
        return false;
    } else if (!oldValue.equals(other.oldValue))
      return false;
    return true;
  }

  /* see superclass */
  @Override
  public String toString() {
    return "AtomicActionJpa [id=" + id + ", objectId=" + objectId
        + ", className=" + className + ", collectionClassName="
        + collectionClassName + ", oldValue=" + oldValue + ", newValue="
        + newValue + ", field=" + field + ", idType=" + idType + "]";
  }

}
