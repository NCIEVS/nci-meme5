/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.meta;

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

import com.wci.umls.server.jpa.helpers.ObjectToStringBridge;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;

import com.wci.umls.server.model.meta.SemanticTypeComponentIdentity;

/**
 * JPA and JAXB enabled implementation of {@link SemanticTypeComponentIdentity}.
 */
@Entity
@Table(name = "sty_identity", uniqueConstraints = @UniqueConstraint(columnNames = {
    "conceptTerminologyId", "semanticType", "terminology"
}))
@XmlRootElement(name = "styIdentity")
@Indexed
public class SemanticTypeComponentIdentityJpa
    implements SemanticTypeComponentIdentity {

  /** The id. */
  @Id
  @GenericField(searchable = Searchable.YES,
      valueBridge = @ValueBridgeRef(type = ObjectToStringBridge.class))
  private Long id;

  /** The concept terminology id. */
  @Column(nullable = false)
  @KeywordField(searchable = Searchable.YES)
  private String conceptTerminologyId;

  /** The terminology. */
  @Column(nullable = false)
  @KeywordField(searchable = Searchable.YES)
  private String terminology;

  /** The semantic type. */
  @Column(nullable = false)
  @KeywordField(searchable = Searchable.YES)
  private String semanticType;

  /**
   * Instantiates an empty {@link SemanticTypeComponentIdentityJpa}.
   */
  public SemanticTypeComponentIdentityJpa() {
    // n/a
  }

  /**
   * Instantiates a {@link SemanticTypeComponentIdentityJpa} from a collection
   * of fields expected to exist in a semanticTypeComponentIdentity.txt file.
   * 
   * id|conceptTerminologyId|terminology|semanticType
   * 
   * @param fields the fields
   */
  public SemanticTypeComponentIdentityJpa(String[] fields) {
    id = Long.valueOf(fields[0]);
    conceptTerminologyId = fields[1];
    terminology = fields[2];
    semanticType = fields[3];
  }

  /**
   * Instantiates a {@link SemanticTypeComponentIdentityJpa} from the specified
   * parameters.
   *
   * @param identity the identity
   */
  public SemanticTypeComponentIdentityJpa(
      SemanticTypeComponentIdentity identity) {
    super();
    id = identity.getId();
    terminology = identity.getTerminology();
    conceptTerminologyId = identity.getConceptTerminologyId();
    semanticType = identity.getSemanticType();
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
  public String getConceptTerminologyId() {
    return conceptTerminologyId;
  }

  /* see superclass */
  @Override
  public void setConceptTerminologyId(String conceptTerminologyId) {
    this.conceptTerminologyId = conceptTerminologyId;
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((conceptTerminologyId == null) ? 0
        : conceptTerminologyId.hashCode());
    result =
        prime * result + ((semanticType == null) ? 0 : semanticType.hashCode());
    result =
        prime * result + ((terminology == null) ? 0 : terminology.hashCode());
    return result;
  }

  /* see superclass */
  @Override
  public String getSemanticType() {
    return semanticType;
  }

  /* see superclass */
  @Override
  public void setSemanticType(String semanticType) {
    this.semanticType = semanticType;
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
    SemanticTypeComponentIdentityJpa other =
        (SemanticTypeComponentIdentityJpa) obj;
    if (conceptTerminologyId == null) {
      if (other.conceptTerminologyId != null)
        return false;
    } else if (!conceptTerminologyId.equals(other.conceptTerminologyId))
      return false;
    if (semanticType == null) {
      if (other.semanticType != null)
        return false;
    } else if (!semanticType.equals(other.semanticType))
      return false;
    if (terminology == null) {
      if (other.terminology != null)
        return false;
    } else if (!terminology.equals(other.terminology))
      return false;
    return true;
  }

  /* see superclass */
  @Override
  @KeywordField(searchable = Searchable.YES)
  @IndexingDependency(derivedFrom = {
      @ObjectPath(@PropertyValue(propertyName = "conceptTerminologyId")),
      @ObjectPath(@PropertyValue(propertyName = "semanticType")),
      @ObjectPath(@PropertyValue(propertyName = "terminology"))
  })
  public String getIdentityCode() {
    return conceptTerminologyId + semanticType + terminology;
  }

  /* see superclass */
  @Override
  public void setIdentityCode(String identityCode) {
    // n/a
  }

  /* see superclass */
  @Override
  public String toString() {
    return "SemanticTypeComponentIdentityJpa [id=" + id + ", terminology="
        + terminology + ", conceptTerminologyId=" + conceptTerminologyId
        + ", semanticType=" + semanticType + "]";
  }

}
