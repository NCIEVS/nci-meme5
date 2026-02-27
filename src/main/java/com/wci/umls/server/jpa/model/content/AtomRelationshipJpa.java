/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.model.content;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtract;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;

import com.wci.umls.server.jpa.model.helpers.MapKeyValueToCsvBridge;
import com.wci.umls.server.jpa.model.helpers.ObjectToStringBridge;
import com.wci.umls.server.model.content.Atom;
import com.wci.umls.server.model.content.AtomRelationship;
import com.wci.umls.server.model.content.Attribute;
import com.wci.umls.server.model.content.Relationship;

/**
 * JPA and JAXB enabled implementation of {@link AtomRelationship}.
 */
@Entity
@Table(name = "atom_relationships", uniqueConstraints = @UniqueConstraint(columnNames = {
    "terminologyId", "terminology", "version", "id"
}))
//@Audited
@Indexed
@XmlRootElement(name = "atomRelationship")
public class AtomRelationshipJpa extends AbstractRelationship<Atom, Atom>
    implements AtomRelationship {

  /** The from atom. */
  @ManyToOne(targetEntity = AtomJpa.class, optional = false)
  @JoinColumn(nullable = false)
  private Atom from;

  /** the to atom. */
  @ManyToOne(targetEntity = AtomJpa.class, optional = false)
  @JoinColumn(nullable = false)
  private Atom to;

  /** The alternate terminology ids. */
  @ElementCollection(fetch = FetchType.EAGER)
  @Fetch(FetchMode.JOIN)
  @MapKeyColumn(length = 100)
  @Column(nullable = true, length = 100)
  private Map<String, String> alternateTerminologyIds;

  /** The attributes. */
  @OneToMany(targetEntity = AttributeJpa.class)
  @JoinColumn(name = "attributes_id")
  @JoinTable(name = "atom_relationships_attributes",
      inverseJoinColumns = @JoinColumn(name = "attributes_id"),
      joinColumns = @JoinColumn(name = "atom_relationships_id"))
  private List<Attribute> attributes = null;

  /**
   * Instantiates an empty {@link AtomRelationshipJpa}.
   */
  public AtomRelationshipJpa() {
    // do nothing
  }

  /**
   * Instantiates a {@link AtomRelationshipJpa} from the specified parameters.
   *
   * @param relationship the concept relationship
   * @param collectionCopy the deep copy
   */
  public AtomRelationshipJpa(AtomRelationship relationship, boolean collectionCopy) {
    super(relationship, collectionCopy);
    to = relationship.getTo();
    from = relationship.getFrom();
    if (collectionCopy) {
      alternateTerminologyIds = new HashMap<>(relationship.getAlternateTerminologyIds());
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

  /* see superclass */
  @Override
  @XmlTransient
  public Atom getFrom() {
    return from;
  }

  /* see superclass */
  @Override
  public void setFrom(Atom component) {
    this.from = component;
  }

  /**
   * Returns the from id. For JAXB.
   *
   * @return the from id
   */
  @GenericField(searchable = Searchable.YES,
      valueBridge = @ValueBridgeRef(type = ObjectToStringBridge.class))
  @IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "from")))
  public Long getFromId() {
    return from == null ? null : from.getId();
  }

  /**
   * Sets the from id.
   *
   * @param id the from id
   */
  public void setFromId(Long id) {
    if (from == null) {
      from = new AtomJpa();
    }
    from.setId(id);
  }

  /**
   * Returns the from terminology id.
   *
   * @return the from terminology id
   */
  @KeywordField(searchable = Searchable.YES)
  @IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "from")))
  public String getFromTerminologyId() {
    return from == null ? null : from.getTerminologyId();
  }

  /**
   * Sets the from terminology id.
   *
   * @param terminologyId the from terminology id
   */
  public void setFromTerminologyId(String terminologyId) {
    if (from == null) {
      from = new AtomJpa();
    }
    from.setTerminologyId(terminologyId);
  }

  /**
   * Returns the from name. For JAXB.
   *
   * @return the from name
   */
  @FullTextField(analyzer = "noStopWord")
  @KeywordField(name = "fromNameSort", sortable = Sortable.YES)
  @IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "from")))
  public String getFromName() {
    return from == null ? null : from.getName();
  }

  /**
   * Sets the from name.
   *
   * @param name the from name
   */
  public void setFromName(String name) {
    if (from == null) {
      from = new AtomJpa();
    }
    from.setName(name);
  }

  /* see superclass */
  @Override
  @XmlTransient
  public Atom getTo() {
    return to;
  }

  /**
   * Returns the to id.
   *
   * @return the to id
   */
  @GenericField(searchable = Searchable.YES,
      valueBridge = @ValueBridgeRef(type = ObjectToStringBridge.class))
  @IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "to")))
  public Long getToId() {
    return to == null ? null : to.getId();
  }

  /**
   * Sets the to id.
   *
   * @param id the to id
   */
  public void setToId(Long id) {
    if (to == null) {
      to = new AtomJpa();
    }
    to.setId(id);
  }

  /**
   * Returns the to terminology id.
   *
   * @return the to terminology id
   */
  @KeywordField(searchable = Searchable.YES)
  @IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "to")))
  public String getToTerminologyId() {
    return to == null ? null : to.getTerminologyId();
  }

  /**
   * Sets the to terminology id.
   *
   * @param terminologyId the to terminology id
   */
  public void setToTerminologyId(String terminologyId) {
    if (to == null) {
      to = new AtomJpa();
    }
    to.setTerminologyId(terminologyId);
  }

  /**
   * Returns the to name.
   *
   * @return the to name
   */
  @FullTextField
  @KeywordField(name = "toNameSort", sortable = Sortable.YES)
  @IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "to")))
  public String getToName() {
    return to == null ? null : to.getName();
  }

  /**
   * Sets the to name.
   *
   * @param name the to name
   */
  public void setToName(String name) {
    if (to == null) {
      to = new AtomJpa();
    }
    to.setName(name);
  }

  /* see superclass */
  @Override
  public void setTo(Atom component) {
    this.to = component;
  }

  /* see superclass */
  @Override
  @FullTextField(name = "alternateTerminologyIds",
      valueBridge = @ValueBridgeRef(type = MapKeyValueToCsvBridge.class),
      extraction = @ContainerExtraction(extract = ContainerExtract.NO))
  public Map<String, String> getAlternateTerminologyIds() {
    if (alternateTerminologyIds == null) {
      alternateTerminologyIds = new HashMap<>(2);
    }
    return alternateTerminologyIds;
  }

  /* see superclass */
  @Override
  public void setAlternateTerminologyIds(Map<String, String> alternateTerminologyIds) {
    this.alternateTerminologyIds = alternateTerminologyIds;
  }

  /* see superclass */
  @Override
  public Relationship<Atom, Atom> createInverseRelationship(Relationship<Atom, Atom> relationship,
    String inverseRelType, String inverseAdditionalRelType) throws Exception {

    final AtomRelationship inverseRelationship =
        new AtomRelationshipJpa((AtomRelationship) relationship, false);

    return populateInverseRelationship(relationship, inverseRelationship, inverseRelType,
        inverseAdditionalRelType);
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((from == null) ? 0 : from.hashCode());
    result = prime * result + ((to == null) ? 0 : to.hashCode());
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
    AtomRelationshipJpa other = (AtomRelationshipJpa) obj;
    if (from == null) {
      if (other.from != null)
        return false;
    } else if (!from.equals(other.from))
      return false;
    if (to == null) {
      if (other.to != null)
        return false;
    } else if (!to.equals(other.to))
      return false;
    return true;
  }

  @Override
  @XmlTransient
  public boolean isShortFormLongForm() {
    return getRelationshipType().equals("SY") && (getAdditionalRelationshipType().startsWith("mth_")
        || getAdditionalRelationshipType().contains("expanded_form"));
  }

  // Use superclass toString()

}
