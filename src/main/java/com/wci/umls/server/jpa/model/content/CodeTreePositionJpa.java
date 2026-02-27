/**
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.model.content;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;

import com.wci.umls.server.jpa.model.helpers.ObjectToStringBridge;
import com.wci.umls.server.model.content.Attribute;
import com.wci.umls.server.model.content.Code;
import com.wci.umls.server.model.content.CodeTreePosition;

/**
 * JPA and JAXB enabled implementation of {@link CodeTreePosition}.
 */
@Entity
@Table(name = "code_tree_positions", uniqueConstraints = @UniqueConstraint(columnNames = {
    "terminologyId", "terminology", "version", "id"
}))
//@Audited
@Indexed
@XmlRootElement(name = "codeTreePosition")
public class CodeTreePositionJpa extends AbstractTreePosition<Code> implements CodeTreePosition {

  /** The code. */
  @ManyToOne(targetEntity = CodeJpa.class, fetch = FetchType.EAGER, optional = false)
  @JoinColumn(nullable = false)
  private Code node;

  /** The attributes. */
  @OneToMany(targetEntity = AttributeJpa.class)
  @JoinColumn(name = "attributes_id")
  @JoinTable(name = "code_tree_positions_attributes",
      inverseJoinColumns = @JoinColumn(name = "attributes_id"),
      joinColumns = @JoinColumn(name = "code_tree_positions_id"))
  private List<Attribute> attributes = null;

  /**
   * Instantiates an empty {@link CodeTreePositionJpa}.
   */
  public CodeTreePositionJpa() {
    // do nothing
  }

  /**
   * Instantiates a {@link CodeTreePositionJpa} from the specified parameters.
   *
   * @param treepos the treepos
   * @param collectionCopy the deep copy
   */
  public CodeTreePositionJpa(CodeTreePosition treepos, boolean collectionCopy) {
    super(treepos, collectionCopy);
    node = treepos.getNode();
    if (collectionCopy) {
      for (final Attribute attribute : treepos.getAttributes()) {
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
  @XmlTransient
  @Override
  public Code getNode() {
    return node;
  }

  /**
   * Returns the node id. For JAXB.
   *
   * @return the node id
   */
  @XmlElement
  @KeywordField(searchable = Searchable.YES,
      valueBridge = @ValueBridgeRef(type = ObjectToStringBridge.class))
  @IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "node")))
  public Long getNodeId() {
    return node == null ? null : node.getId();
  }

  /**
   * Sets the node id. For JAXB.
   *
   * @param id the node id
   */
  public void setNodeId(Long id) {
    if (node == null) {
      node = new CodeJpa();
    }
    node.setId(id);
  }

  /**
   * Returns the node name. For JAXB.
   *
   * @return the node name
   */
  @FullTextField(name = "nodeName", analyzer = "noStopWord")
  @KeywordField(name = "nodeNameSort", sortable = Sortable.YES)
  @IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "node")))
  public String getNodeName() {
    return node == null ? null : node.getName();
  }

  /**
   * Sets the node name. For JAXB.
   *
   * @param name the node name
   */
  public void setNodeName(String name) {
    if (node == null) {
      node = new CodeJpa();
    }
    node.setName(name);
  }

  /**
   * Returns the node terminology id. For JAXB.
   *
   * @return the node terminology id
   */
  @KeywordField(searchable = Searchable.YES, sortable = Sortable.YES)
  @IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "node")))
  public String getNodeTerminologyId() {
    return node == null ? null : node.getTerminologyId();
  }

  /**
   * Sets the node terminology id. For JAXB.
   *
   * @param terminologyId the node terminology id
   */
  public void setNodeTerminologyId(String terminologyId) {
    if (node == null) {
      node = new CodeJpa();
    }
    node.setTerminologyId(terminologyId);
  }

  /**
   * Returns the node terminology. For JAXB.
   *
   * @return the node terminology
   */
  @KeywordField(searchable = Searchable.YES)
  @IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "node")))
  public String getNodeTerminology() {
    return node == null ? null : node.getTerminology();
  }

  /**
   * Sets the node terminology. For JAXB.
   *
   * @param terminology the node terminology
   */
  public void setNodeTerminology(String terminology) {
    if (node == null) {
      node = new CodeJpa();
    }
    node.setTerminology(terminology);
  }

  /**
   * Returns the node version. For JAXB.
   *
   * @return the node version
   */
  @KeywordField(searchable = Searchable.YES)
  @IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "node")))
  public String getNodeVersion() {
    return node == null ? null : node.getVersion();
  }

  /**
   * Sets the node version. For JAXB.
   *
   * @param version the node version
   */
  public void setNodeVersion(String version) {
    if (node == null) {
      node = new CodeJpa();
    }
    node.setVersion(version);
  }

  /* see superclass */
  @Override
  public void setNode(Code code) {
    this.node = code;
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((node == null || node.getTerminologyId() == null) ? 0
        : node.getTerminologyId().hashCode());
    return result;
  }

  /**
   * CUSTOM for concept id.
   *
   * @param obj the obj
   * @return true, if successful
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    CodeTreePositionJpa other = (CodeTreePositionJpa) obj;
    if (node == null) {
      if (other.node != null)
        return false;
    } else if (node.getTerminologyId() == null) {
      if (other.node != null && other.node.getTerminologyId() != null)
        return false;
    } else if (!node.getTerminologyId().equals(other.node.getTerminologyId()))
      return false;
    return true;
  }

}
