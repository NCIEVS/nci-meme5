/**
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.content;

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
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtract;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;

import com.wci.umls.server.jpa.helpers.MapKeyValueToCsvBridge;
import com.wci.umls.server.model.content.Attribute;
import com.wci.umls.server.model.content.Concept;
import com.wci.umls.server.model.content.ConceptSubset;
import com.wci.umls.server.model.content.ConceptSubsetMember;
import com.wci.umls.server.model.content.Subset;

/**
 * JPA and JAXB enabled implementation of an {@link Concept} {@link Subset}.
 */
@Entity
@Table(name = "concept_subsets", uniqueConstraints = @UniqueConstraint(columnNames = {
    "terminologyId", "terminology", "version", "id"
}))
//@Audited
@XmlRootElement(name = "conceptSubset")
public class ConceptSubsetJpa extends AbstractSubset implements ConceptSubset {

  /** The disjoint subset. */
  @Column(nullable = false)
  private boolean disjointSubset = false;

  /** The label subset flag. */
  @Column(nullable = false)
  private boolean labelSubset = false;

  /** The members. */
  @OneToMany(mappedBy = "subset", targetEntity = ConceptSubsetMemberJpa.class)
  private List<ConceptSubsetMember> members = null;

  /** The alternate terminology ids. */
  @ElementCollection(fetch = FetchType.EAGER)
  @Fetch(FetchMode.JOIN)
  @MapKeyColumn(length = 100)
  @Column(nullable = true, length = 100)
  @FullTextField(name = "alternateTerminologyIds",
      valueBridge = @ValueBridgeRef(type = MapKeyValueToCsvBridge.class),
      extraction = @ContainerExtraction(extract = ContainerExtract.NO))
  private Map<String, String> alternateTerminologyIds;

  /** The attributes. */
  @OneToMany(targetEntity = AttributeJpa.class)
  @JoinColumn(name = "attributes_id")
  @JoinTable(name = "concept_subsets_attributes",
      inverseJoinColumns = @JoinColumn(name = "attributes_id"),
      joinColumns = @JoinColumn(name = "concept_subsets_id"))
  private List<Attribute> attributes = null;
  
  /**
   * Instantiates an empty {@link ConceptSubsetJpa}.
   */
  public ConceptSubsetJpa() {
    // do nothing
  }

  /**
   * Instantiates a {@link ConceptSubsetJpa} from the specified parameters.
   *
   * @param subset the subset
   * @param collectionCopy the deep copy
   */
  public ConceptSubsetJpa(ConceptSubset subset, boolean collectionCopy) {
    super(subset, collectionCopy);
    disjointSubset = subset.isDisjointSubset();
    labelSubset = subset.isLabelSubset();
    alternateTerminologyIds =
        new HashMap<>(subset.getAlternateTerminologyIds());

    if (collectionCopy) {
      members = new ArrayList<>(subset.getMembers());
      for (final Attribute attribute : subset.getAttributes()) {
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
  @XmlElement(type = ConceptSubsetMemberJpa.class)
  @Override
  public List<ConceptSubsetMember> getMembers() {
    if (members == null) {
      members = new ArrayList<>();
    }
    return members;
  }

  /* see superclass */
  @Override
  public void setMembers(List<ConceptSubsetMember> members) {
    this.members = members;
  }

  /* see superclass */
  @Override
  public void clearMembers() {
    members = new ArrayList<>();
  }

  /* see superclass */
  @Override
  public boolean isDisjointSubset() {
    return disjointSubset;
  }

  /* see superclass */
  @Override
  public void setDisjointSubset(boolean disjointSubset) {
    this.disjointSubset = disjointSubset;
  }

  /* see superclass */
  @Override
  public boolean isLabelSubset() {
    return labelSubset;
  }

  /* see superclass */
  @Override
  public void setLabelSubset(boolean labelSubset) {
    this.labelSubset = labelSubset;
  }

  /* see superclass */
  @Override
  public Map<String, String> getAlternateTerminologyIds() {
    if (alternateTerminologyIds == null) {
      alternateTerminologyIds = new HashMap<>(2);
    }
    return alternateTerminologyIds;
  }

  /* see superclass */
  @Override
  public void setAlternateTerminologyIds(
    Map<String, String> alternateTerminologyIds) {
    this.alternateTerminologyIds = alternateTerminologyIds;
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + (disjointSubset ? 1231 : 1237);
    result = prime * result + (labelSubset ? 1231 : 1237);
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
    ConceptSubsetJpa other = (ConceptSubsetJpa) obj;
    if (disjointSubset != other.disjointSubset)
      return false;
    if (labelSubset != other.labelSubset)
      return false;
    return true;
  }

  /* see superclass */
  @Override
  public String toString() {
    return getClass().getSimpleName() + " [name=" + getName() + ", description="
        + getDescription() + ", disjointSubset=" + disjointSubset + "]";
  }

}
