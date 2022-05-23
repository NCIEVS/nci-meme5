/**
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.content;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.hibernate.envers.Audited;

import com.wci.umls.server.model.content.Atom;
import com.wci.umls.server.model.content.AtomTransitiveRelationship;
import com.wci.umls.server.model.content.Attribute;

/**
 * JPA-enabled implementation of {@link AtomTransitiveRelationship}.
 */
@Entity
@Table(name = "atom_transitive_rels", uniqueConstraints = @UniqueConstraint(columnNames = {
    "terminologyId", "terminology", "version", "id"
}))
@Audited
@XmlRootElement(name = "atomTransitiveRel")
public class AtomTransitiveRelationshipJpa extends AbstractTransitiveRelationship<Atom>
    implements AtomTransitiveRelationship {

  /** The super type. */
  @ManyToOne(targetEntity = AtomJpa.class, fetch = FetchType.EAGER, optional = false)
  @JoinColumn(nullable = false)
  private Atom superType;

  /** The sub type. */
  @ManyToOne(targetEntity = AtomJpa.class, fetch = FetchType.EAGER, optional = false)
  @JoinColumn(nullable = false)
  private Atom subType;

  /** The attributes. */
  @OneToMany(targetEntity = AttributeJpa.class)
  @JoinColumn(name = "attributes_id")
  @JoinTable(name = "atom_transitive_rels_attributes",
      joinColumns = @JoinColumn(name = "attributes_id"),
      inverseJoinColumns = @JoinColumn(name = "atom_transitive_rels_id"))
  private List<Attribute> attributes = null;

  /**
   * Instantiates an empty {@link AtomTransitiveRelationshipJpa}.
   */
  public AtomTransitiveRelationshipJpa() {
    // do nothing
  }

  /**
   * Instantiates a {@link AtomTransitiveRelationshipJpa} from the specified
   * parameters.
   *
   * @param relationship the relationship
   * @param collectionCopy the deep copy
   */
  public AtomTransitiveRelationshipJpa(AtomTransitiveRelationship relationship,
      boolean collectionCopy) {
    super(relationship, collectionCopy);
    superType = relationship.getSuperType();
    subType = relationship.getSubType();
    if (collectionCopy) {
      for (final Attribute attribute : relationship.getAttributes()) {
        getAttributes().add(new AttributeJpa(attribute));
      }
    }
  }

  /* see superclass */
  @Override
  @XmlElement(type = AttributeJpa.class)
  public List<Attribute> getAttributes() {
    if (attributes == null) {
      attributes = new ArrayList<>(1);
    }
    return attributes;
  }

  /* see superclass */
  @Override
  public void setAttributes(List<Attribute> attributes) {
    this.attributes = attributes;
  }

  /* see superclass */
  @Override
  public Attribute getAttributeByName(String name) {
    for (final Attribute attribute : getAttributes()) {
      // If there are more than one, this just returns the first.
      if (attribute.getName().equals(name)) {
        return attribute;
      }
    }
    return null;
  }

  @XmlTransient
  @Override
  public Atom getSuperType() {
    return superType;
  }

  @Override
  public void setSuperType(Atom ancestor) {
    this.superType = ancestor;
  }

  /**
   * Returns the super type id. For JAXB.
   *
   * @return the super type id
   */
  public Long getSuperTypeId() {
    return superType == null ? null : superType.getId();
  }

  /**
   * Sets the super type id.
   *
   * @param id the super type id
   */
  public void setSuperTypeId(Long id) {
    if (superType == null) {
      superType = new AtomJpa();
    }
    superType.setId(id);
  }

  /**
   * Returns the super type terminology id. For JAXB.
   *
   * @return the super type terminology id
   */
  public String getSuperTypeTerminologyId() {
    return superType == null ? "" : superType.getTerminologyId();
  }

  /**
   * Sets the super type terminology id.
   *
   * @param terminologyId the new super type terminology id
   */
  public void setSuperTypeTerminologyId(String terminologyId) {
    if (superType == null) {
      superType = new AtomJpa();
    }
    superType.setTerminologyId(terminologyId);
  }

  /**
   * Returns the super type terminology id. For JAXB.
   *
   * @return the super type terminology id
   */
  public String getSuperTypeTerminology() {
    return superType == null ? null : superType.getTerminology();
  }

  /**
   * Sets the super type terminology.
   *
   * @param terminology the super type terminology
   */
  public void setSuperTypeTerminology(String terminology) {
    if (superType == null) {
      superType = new AtomJpa();
    }
    superType.setTerminology(terminology);
  }

  /**
   * Returns the super type terminology. For JAXB.
   *
   * @return the super type terminology
   */
  public String getSuperTypeVersion() {
    return superType == null ? null : superType.getVersion();
  }

  /**
   * Sets the super type version.
   *
   * @param version the super type version
   */
  public void setSuperTypeVersion(String version) {
    if (superType == null) {
      superType = new AtomJpa();
    }
    superType.setVersion(version);
  }

  /**
   * Returns the super type term. For JAXB.
   *
   * @return the super type term
   */
  public String getSuperTypeName() {
    return superType == null ? null : superType.getName();
  }

  /**
   * Sets the super type term.
   *
   * @param term the super type term
   */
  public void setSuperTypeName(String term) {
    if (superType == null) {
      superType = new AtomJpa();
    }
    superType.setName(term);
  }

  @XmlTransient
  @Override
  public Atom getSubType() {
    return subType;
  }

  @Override
  public void setSubType(Atom descendant) {
    this.subType = descendant;
  }

  /**
   * Returns the sub type id. For JAXB.
   *
   * @return the sub type id
   */
  public Long getSubTypeId() {
    return subType == null ? null : subType.getId();
  }

  /**
   * Sets the sub type id.
   *
   * @param id the sub type id
   */
  public void setSubTypeId(Long id) {
    if (subType == null) {
      subType = new AtomJpa();
    }
    subType.setId(id);
  }

  /**
   * Returns the sub type terminology id. For JAXB.
   *
   * @return the sub type terminology id
   */
  public String getSubTypeTerminology() {
    return subType == null ? null : subType.getTerminology();
  }

  /**
   * Sets the sub type terminology.
   *
   * @param terminology the sub type terminology
   */
  public void setSubTypeTerminology(String terminology) {
    if (subType == null) {
      subType = new AtomJpa();
    }
    subType.setTerminology(terminology);
  }

  /**
   * Returns the sub type terminology. For JAXB.
   *
   * @return the sub type terminology
   */
  public String getSubTypeVersion() {
    return subType == null ? null : subType.getVersion();
  }

  /**
   * Sets the sub type version.
   *
   * @param version the sub type version
   */
  public void setSubTypeVersion(String version) {
    if (subType == null) {
      subType = new AtomJpa();
    }
    subType.setVersion(version);
  }

  /**
   * Returns the sub type terminology id. For JAXB.
   *
   * @return the sub type terminology id
   */
  public String getSubTypeTerminologyId() {
    return subType == null ? null : subType.getTerminologyId();
  }

  /**
   * Sets the sub type terminology id.
   *
   * @param terminologyId the sub type terminology id
   */
  public void setSubTypeTerminologyId(String terminologyId) {
    if (subType == null) {
      subType = new AtomJpa();
    }
    subType.setTerminologyId(terminologyId);
  }

  /**
   * Returns the sub type term. For JAXB.
   *
   * @return the sub type term
   */
  public String getSubTypeName() {
    return subType == null ? null : subType.getName();
  }

  /**
   * Sets the sub type term.
   *
   * @param term the sub type term
   */
  public void setSubTypeName(String term) {
    if (subType == null) {
      subType = new AtomJpa();
    }
    subType.setName(term);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((subType == null) ? 0 : subType.hashCode());
    result = prime * result + ((superType == null) ? 0 : superType.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    AtomTransitiveRelationshipJpa other = (AtomTransitiveRelationshipJpa) obj;
    if (subType == null) {
      if (other.subType != null)
        return false;
    } else if (!subType.equals(other.subType))
      return false;
    if (superType == null) {
      if (other.superType != null)
        return false;
    } else if (!superType.equals(other.superType))
      return false;
    return true;
  }

}
