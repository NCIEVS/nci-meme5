/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.model.meta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;

import com.wci.umls.server.jpa.model.helpers.ObjectToStringBridge;
import com.wci.umls.server.model.meta.AttributeIdentity;
import com.wci.umls.server.model.meta.IdType;

/**
 * JPA and JAXB enabled implementation of {@link AttributeIdentity}.
 */
@Entity
@Table(name = "attribute_identity", uniqueConstraints = @UniqueConstraint(columnNames = {
    "componentId", "componentTerminology", "componentType", "id"
}))
@XmlRootElement(name = "attributeIdentity")
@Indexed
public class AttributeIdentityJpa implements AttributeIdentity {

  /** The id. */
  @Id
  @GenericField(searchable = Searchable.YES,
      valueBridge = @ValueBridgeRef(type = ObjectToStringBridge.class))
  private Long id;

  /** The attribute name. */
  @Column(nullable = false)
  @KeywordField(searchable = Searchable.YES)
  private String name;

  /** The terminology id. */
  @Column(nullable = false)
  @KeywordField(searchable = Searchable.YES)
  private String terminologyId;

  /** The terminology. */
  @Column(nullable = false)
  @KeywordField(searchable = Searchable.YES)
  private String terminology;

  /** The component id. */
  @Column(nullable = false)
  @KeywordField(searchable = Searchable.YES)
  private String componentId;

  /** The component type. */
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  @GenericField(searchable = Searchable.YES,
      valueBridge = @ValueBridgeRef(type = ObjectToStringBridge.class))
  private IdType componentType;

  /** The component terminology. */
  @Column(nullable = false)
  @KeywordField(searchable = Searchable.YES)
  private String componentTerminology;

  /** The attribute value hash code. */
  @Column(nullable = false)
  @KeywordField(searchable = Searchable.YES)
  private String hashcode;

  /**
   * Instantiates an empty {@link AttributeIdentityJpa}.
   */
  public AttributeIdentityJpa() {
    //
  }

  /**
   * Instantiates a {@link AttributeIdentityJpa} from the specified parameters.
   *
   * @param identity the identity
   */
  public AttributeIdentityJpa(AttributeIdentity identity) {
    id = identity.getId();
    name = identity.getName();
    hashcode = identity.getHashcode();
    terminologyId = identity.getTerminologyId();
    terminology = identity.getTerminology();
    componentId = identity.getComponentId();
    componentType = identity.getComponentType();
    componentTerminology = identity.getComponentTerminology();
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
  public String getTerminologyId() {
    return terminologyId;
  }

  /* see superclass */
  @Override
  public void setTerminologyId(String terminologyId) {
    this.terminologyId = terminologyId;
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
  public String getComponentId() {
    return componentId;
  }

  /* see superclass */
  @Override
  public void setComponentId(String componentId) {
    this.componentId = componentId;
  }

  /* see superclass */
  @Override
  public IdType getComponentType() {
    return componentType;
  }

  /* see superclass */
  @Override
  public void setComponentType(IdType componentType) {
    this.componentType = componentType;
  }

  /* see superclass */
  @Override
  public String getComponentTerminology() {
    return componentTerminology;
  }

  /* see superclass */
  @Override
  public void setComponentTerminology(String componentTerminology) {
    this.componentTerminology = componentTerminology;
  }

  /* see superclass */
  @Override
  public String getHashcode() {
    return hashcode;
  }

  /* see superclass */
  @Override
  public void setHashcode(String hashcode) {
    this.hashcode = hashcode;
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
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime * result + ((componentId == null) ? 0 : componentId.hashCode());
    result = prime * result + ((componentTerminology == null) ? 0
        : componentTerminology.hashCode());
    result = prime * result
        + ((componentType == null) ? 0 : componentType.hashCode());
    result = prime * result + ((hashcode == null) ? 0 : hashcode.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result =
        prime * result + ((terminology == null) ? 0 : terminology.hashCode());
    result = prime * result
        + ((terminologyId == null) ? 0 : terminologyId.hashCode());
    result = prime * result + ((hashcode == null) ? 0 : hashcode.hashCode());
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
    AttributeIdentityJpa other = (AttributeIdentityJpa) obj;
    if (componentId == null) {
      if (other.componentId != null)
        return false;
    } else if (!componentId.equals(other.componentId))
      return false;
    if (componentTerminology == null) {
      if (other.componentTerminology != null)
        return false;
    } else if (!componentTerminology.equals(other.componentTerminology))
      return false;
    if (componentType != other.componentType)
      return false;
    if (hashcode == null) {
      if (other.hashcode != null)
        return false;
    } else if (!hashcode.equals(other.hashcode))
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (terminology == null) {
      if (other.terminology != null)
        return false;
    } else if (!terminology.equals(other.terminology))
      return false;
    if (terminologyId == null) {
      if (other.terminologyId != null)
        return false;
    } else if (!terminologyId.equals(other.terminologyId))
      return false;
    return true;
  }

  /* see superclass */
  @Override
  @KeywordField(name = "identityCode", searchable = Searchable.YES)
  @IndexingDependency(derivedFrom = {
      @ObjectPath(@PropertyValue(propertyName = "componentId")),
      @ObjectPath(@PropertyValue(propertyName = "componentTerminology")),
      @ObjectPath(@PropertyValue(propertyName = "hashcode")),
      @ObjectPath(@PropertyValue(propertyName = "name")),
      @ObjectPath(@PropertyValue(propertyName = "terminology")),
      @ObjectPath(@PropertyValue(propertyName = "terminologyId"))
  })
  public String getIdentityCode() {
    return componentId + componentTerminology + hashcode + name + terminology
        + terminologyId;
  }

  /* see superclass */
  @Override
  public void setIdentityCode(String identityCode) {
    // n/a
  }

  /* see superclass */
  @Override
  public String toString() {
    return "AttributeIdentityJpa [id=" + id + ", name=" + name
        + ", terminologyId=" + terminologyId + ", terminology=" + terminology
        + ", componentId=" + componentId + ", componentType=" + componentType
        + ", componentTerminology=" + componentTerminology + "]";
  }

}
