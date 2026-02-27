/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.model.meta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

import com.wci.umls.server.jpa.model.helpers.ObjectToStringBridge;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;

import com.wci.umls.server.model.meta.AtomIdentity;

/**
 * JPA and JAXB enabled implementation of {@link AtomIdentity}.
 */
@Entity
@Table(name = "atom_identity", uniqueConstraints = {
    @UniqueConstraint(columnNames = {
        "stringClassId", "terminology", "terminologyId", "id"
    }), @UniqueConstraint(columnNames = {
        "conceptId", "terminology", "terminologyId", "id"
    }), @UniqueConstraint(columnNames = {
        "descriptorId", "terminology", "terminologyId", "id"
    }), @UniqueConstraint(columnNames = {
        "codeId", "terminology", "terminologyId", "id"
    })
})
@XmlRootElement(name = "atomIdentity")
@Indexed
public class AtomIdentityJpa implements AtomIdentity {

  /** The id. */
  @Id
  @GenericField(searchable = Searchable.YES,
      valueBridge = @ValueBridgeRef(type = ObjectToStringBridge.class))
  private Long id;

  /** The string class id. */
  @Column(nullable = false)
  @KeywordField(searchable = Searchable.YES)
  private String stringClassId;

  /** The terminology. */
  @Column(nullable = false)
  @KeywordField(searchable = Searchable.YES)
  private String terminology;

  /** The terminology id. */
  @Column(nullable = false)
  @KeywordField(searchable = Searchable.YES)
  private String terminologyId;

  /** The term type. */
  @Column(nullable = false)
  @KeywordField(searchable = Searchable.YES)
  private String termType;

  /** The code id. */
  @Column(nullable = false)
  @KeywordField(searchable = Searchable.YES)
  private String codeId;

  /** The concept id. */
  @Column(nullable = false)
  @KeywordField(searchable = Searchable.YES)
  private String conceptId;

  /** The descriptor id. */
  @Column(nullable = false)
  @KeywordField(searchable = Searchable.YES)
  private String descriptorId;

  /**
   * Instantiates an empty {@link AtomIdentityJpa}.
   */
  public AtomIdentityJpa() {
    //
  }

  /**
   * Instantiates a {@link AtomIdentityJpa} from the specified parameters.
   *
   * @param identity the identity
   */
  public AtomIdentityJpa(AtomIdentity identity) {
    id = identity.getId();
    stringClassId = identity.getStringClassId();
    terminology = identity.getTerminology();
    terminologyId = identity.getTerminologyId();
    termType = identity.getTermType();
    codeId = identity.getCodeId();
    conceptId = identity.getConceptId();
    descriptorId = identity.getDescriptorId();

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
  public String getStringClassId() {
    return stringClassId;
  }

  /* see superclass */
  @Override
  public void setStringClassId(String stringClassId) {
    this.stringClassId = stringClassId;
  }

  /* see superclass */
  @Override
  public String getTermType() {
    return termType;
  }

  /* see superclass */
  @Override
  public void setTermType(String termType) {
    this.termType = termType;
  }

  /* see superclass */
  @Override
  public String getCodeId() {
    return codeId;
  }

  /* see superclass */
  @Override
  public void setCodeId(String codeId) {
    this.codeId = codeId;

  }

  /* see superclass */
  @Override
  public String getConceptId() {
    return conceptId;
  }

  /* see superclass */
  @Override
  public void setConceptId(String conceptId) {
    this.conceptId = conceptId;
  }

  /* see superclass */
  @Override
  public String getDescriptorId() {
    return descriptorId;
  }

  /* see superclass */
  @Override
  public void setDescriptorId(String descriptorId) {
    this.descriptorId = descriptorId;
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
        + ((stringClassId == null) ? 0 : stringClassId.hashCode());
    result =
        prime * result + ((terminology == null) ? 0 : terminology.hashCode());
    result = prime * result
        + ((terminologyId == null) ? 0 : terminologyId.hashCode());
    result = prime * result + ((termType == null) ? 0 : termType.hashCode());
    result = prime * result + ((codeId == null) ? 0 : codeId.hashCode());
    result = prime * result + ((conceptId == null) ? 0 : conceptId.hashCode());
    result =
        prime * result + ((descriptorId == null) ? 0 : descriptorId.hashCode());

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
    AtomIdentityJpa other = (AtomIdentityJpa) obj;
    if (stringClassId == null) {
      if (other.stringClassId != null)
        return false;
    } else if (!stringClassId.equals(other.stringClassId))
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
    if (termType == null) {
      if (other.termType != null)
        return false;
    } else if (!termType.equals(other.termType))
      return false;
    if (codeId == null) {
      if (other.codeId != null)
        return false;
    } else if (!codeId.equals(other.codeId))
      return false;
    if (conceptId == null) {
      if (other.conceptId != null)
        return false;
    } else if (!conceptId.equals(other.conceptId))
      return false;
    if (descriptorId == null) {
      if (other.descriptorId != null)
        return false;
    } else if (!descriptorId.equals(other.descriptorId))
      return false;
    return true;
  }

  /* see superclass */
  @Override
  @KeywordField(searchable = Searchable.YES)
  @IndexingDependency(derivedFrom = {
      @ObjectPath(@PropertyValue(propertyName = "stringClassId")),
      @ObjectPath(@PropertyValue(propertyName = "terminology")),
      @ObjectPath(@PropertyValue(propertyName = "terminologyId")),
      @ObjectPath(@PropertyValue(propertyName = "termType")),
      @ObjectPath(@PropertyValue(propertyName = "codeId")),
      @ObjectPath(@PropertyValue(propertyName = "conceptId")),
      @ObjectPath(@PropertyValue(propertyName = "descriptorId"))
  })
  public String getIdentityCode() {
    return stringClassId + terminology + terminologyId + termType + codeId
        + conceptId + descriptorId;
  }

  /* see superclass */
  @Override
  public void setIdentityCode(String identityCode) {
    // n/a
  }

  /* see superclass */
  @Override
  public String toString() {
    return "AtomIdentityJpa [id=" + id + ", stringClassId=" + stringClassId
        + ", terminology=" + terminology + ", terminologyId=" + terminologyId
        + ", termType=" + termType + ", code=" + codeId + ", conceptId="
        + conceptId + ", descriptorId=" + descriptorId + "]";
  }

}
