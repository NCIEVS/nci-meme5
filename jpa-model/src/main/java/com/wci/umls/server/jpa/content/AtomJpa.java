/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.content;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtract;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;

import com.wci.umls.server.helpers.ConfigUtility;
import com.wci.umls.server.helpers.Note;
import com.wci.umls.server.jpa.helpers.MapKeyValueToCsvBridge;
import com.wci.umls.server.jpa.helpers.ObjectToStringBridge;
import com.wci.umls.server.model.content.Atom;
import com.wci.umls.server.model.content.AtomRelationship;
import com.wci.umls.server.model.content.AtomSubsetMember;
import com.wci.umls.server.model.content.AtomTreePosition;
import com.wci.umls.server.model.content.Attribute;
import com.wci.umls.server.model.content.ComponentHistory;
import com.wci.umls.server.model.content.Definition;
import com.wci.umls.server.model.workflow.WorkflowStatus;

/**
 * JPA and JAXB enabled implementation of {@link Atom}.
 */
@Entity
// @UniqueConstraint here is being used to create an index, not to enforce
// uniqueness
@Table(name = "atoms", uniqueConstraints = {
    @UniqueConstraint(columnNames = {
        "terminologyId", "terminology", "version", "id"
    }), @UniqueConstraint(columnNames = {
        "conceptId", "terminology", "version", "id"
    }), @UniqueConstraint(columnNames = {
        "codeId", "terminology", "version", "id"
    }), @UniqueConstraint(columnNames = {
        "descriptorId", "terminology", "version", "id"
    }), @UniqueConstraint(columnNames = {
        "lexicalClassId", "terminology", "version", "id"
    }), @UniqueConstraint(columnNames = {
        "stringClassId", "terminology", "version", "id"
    }), @UniqueConstraint(columnNames = {
        "lowerNameHash", "conceptId", "terminology", "version", "id"
    }), @UniqueConstraint(columnNames = {
        "terminology", "version", "id"
    })
})
//@Audited
@Indexed
@XmlRootElement(name = "atom")
public class AtomJpa extends AbstractComponent implements Atom {

  /** The definitions. */
  @OneToMany(targetEntity = DefinitionJpa.class)
  @CollectionTable(name = "atoms_definitions", joinColumns = @JoinColumn(name = "atoms_id"))
  @IndexedEmbedded(targetType = DefinitionJpa.class)
  @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
  private List<Definition> definitions = null;

  /** The members. */
  @OneToMany(mappedBy = "member", targetEntity = AtomSubsetMemberJpa.class)
  private List<AtomSubsetMember> members = null;

  /** The relationships. */
  @OneToMany(mappedBy = "from", targetEntity = AtomRelationshipJpa.class)
  private List<AtomRelationship> relationships = null;

  /** The inverse relationships. */
  @OneToMany(mappedBy = "to", targetEntity = AtomRelationshipJpa.class)
  private List<AtomRelationship> inverseRelationships = null;

  /** The tree positions. */
  @OneToMany(mappedBy = "node", targetEntity = AtomTreePositionJpa.class)
  private List<AtomTreePosition> treePositions = null;

  /** The component histories. */
  @IndexedEmbedded(targetType = ComponentHistoryJpa.class)
  @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
  @CollectionTable(name = "atoms_component_histories", joinColumns = @JoinColumn(name = "atoms_id"))
  @OneToMany(targetEntity = ComponentHistoryJpa.class)
  private List<ComponentHistory> componentHistories = null;

  /** The concept terminology id map. */
  @ElementCollection
  @MapKeyColumn(length = 100)
  @Column(nullable = true, length = 100)
  @FullTextField(name = "conceptTerminologyIds",
      valueBridge = @ValueBridgeRef(type = MapKeyValueToCsvBridge.class),
      extraction = @ContainerExtraction(extract = ContainerExtract.NO))
  Map<String, String> conceptTerminologyIds;

  /** The alternate terminology ids. */
  @ElementCollection(fetch = FetchType.EAGER)
  @Fetch(FetchMode.JOIN)
  @MapKeyColumn(length = 100)
  @Column(nullable = true, length = 100)
  @FullTextField(name = "alternateTerminologyIds",
      valueBridge = @ValueBridgeRef(type = MapKeyValueToCsvBridge.class),
      extraction = @ContainerExtraction(extract = ContainerExtract.NO))
  private Map<String, String> alternateTerminologyIds;

  /** The code id. */
  @Column(nullable = false)
  @KeywordField(searchable = Searchable.YES)
  private String codeId;

  /** The descriptor id. */
  @Column(nullable = false)
  @KeywordField(searchable = Searchable.YES)
  private String descriptorId;

  /** The concept id. */
  @Column(nullable = false)
  @KeywordField(searchable = Searchable.YES)
  private String conceptId;

  /** The language. */
  @Column(nullable = false)
  @KeywordField(searchable = Searchable.YES)
  private String language;

  /** The lexical class id. */
  @Column(nullable = false)
  @KeywordField(searchable = Searchable.YES)
  private String lexicalClassId;

  /** The string class id. */
  @Column(nullable = false)
  @KeywordField(searchable = Searchable.YES)
  private String stringClassId;

  /** The lower name hash. */
  @Column(nullable = true)
  @KeywordField(searchable = Searchable.YES)
  private String lowerNameHash;

  /** The name. */
  @Column(nullable = false, columnDefinition = "LONGTEXT")
  @FullTextField(analyzer = "noStopWord")
  @KeywordField(name = "nameSort", sortable = Sortable.YES)
  @FullTextField(name = "edgeNGramName", analyzer = "autocompleteEdgeAnalyzer")
  @FullTextField(name = "nGramName", analyzer = "autocompleteNGramAnalyzer")
  private String name;

  /** The term type. */
  @Column(nullable = false)
  @KeywordField(searchable = Searchable.YES)
  private String termType;

  /** The workflow status. */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @GenericField(searchable = Searchable.YES,
      valueBridge = @ValueBridgeRef(type = ObjectToStringBridge.class))
  private WorkflowStatus workflowStatus;

  /** The last published rank. */
  @Column(nullable = true)
  private String lastPublishedRank = "0";

  /** The notes. */
  // NOTE: this could cause a performance problem with the join
  @OneToMany(mappedBy = "atom", targetEntity = AtomNoteJpa.class)
  @IndexedEmbedded(targetType = AtomNoteJpa.class)
  @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
  private List<Note> notes = new ArrayList<>();

  /** The attributes. */
  @OneToMany(targetEntity = AttributeJpa.class)
  @JoinColumn(name = "attributes_id")
  @JoinTable(name = "atoms_attributes", inverseJoinColumns = @JoinColumn(name = "attributes_id"),
      joinColumns = @JoinColumn(name = "atoms_id"))
  private List<Attribute> attributes = null;

  /** The rxcui. */
  @Column(nullable = true)
  @KeywordField(searchable = Searchable.YES)
  private String rxcui;

  /**
   * Instantiates an empty {@link AtomJpa}.
   */
  public AtomJpa() {
    // do nothing
  }

  /**
   * Instantiates a {@link AtomJpa} from the specified parameters.
   *
   * @param atom the atom
   */
  public AtomJpa(Atom atom) {
    this(atom, false);
  }

  /**
   * Instantiates a {@link AtomJpa} from the specified parameters.
   *
   * @param atom the atom
   * @param collectionCopy the deep copy
   */
  public AtomJpa(Atom atom, boolean collectionCopy) {
    super(atom);
    codeId = atom.getCodeId();
    conceptTerminologyIds = new HashMap<>(atom.getConceptTerminologyIds());
    alternateTerminologyIds = new HashMap<>(atom.getAlternateTerminologyIds());
    descriptorId = atom.getDescriptorId();
    conceptId = atom.getConceptId();
    language = atom.getLanguage();
    lexicalClassId = atom.getLexicalClassId();
    stringClassId = atom.getStringClassId();
    setName(atom.getName());
    termType = atom.getTermType();
    workflowStatus = atom.getWorkflowStatus();
    lastPublishedRank = atom.getLastPublishedRank();
    rxcui = atom.getRxcui();

    if (collectionCopy) {
      definitions = new ArrayList<>(atom.getDefinitions());
      relationships = new ArrayList<>(atom.getRelationships());
      treePositions = new ArrayList<>(atom.getTreePositions());
      members = new ArrayList<>(atom.getMembers());
      componentHistories = new ArrayList<>(atom.getComponentHistory());
      for (final Attribute attribute : atom.getAttributes()) {
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

      // If there are more than one with this name, sort to return the most recent
	List<Attribute> matchingAttributes = new ArrayList<>();
    for (final Attribute attribute : getAttributes()) {
      if (attribute.getName().equals(name)) {
        matchingAttributes.add(attribute);
      }
    }
    matchingAttributes.sort(Comparator.comparing(Attribute::getLastModified).reversed());
    if (matchingAttributes.size() != 0) {
      return matchingAttributes.get(0);
    }
    return null;
  }

  /* see superclass */
  @Override
  @XmlElement(type = DefinitionJpa.class)
  public List<Definition> getDefinitions() {
    if (definitions == null) {
      definitions = new ArrayList<>(1);
    }
    return definitions;
  }

  /* see superclass */
  @Override
  public void setDefinitions(List<Definition> definitions) {
    this.definitions = definitions;
  }

  /* see superclass */
  @XmlElement(type = AtomRelationshipJpa.class)
  @Override
  public List<AtomRelationship> getRelationships() {
    if (relationships == null) {
      relationships = new ArrayList<>(1);
    }
    return relationships;
  }

  /* see superclass */
  @XmlTransient
  @Override
  public List<AtomRelationship> getInverseRelationships() {
    if (inverseRelationships == null) {
      inverseRelationships = new ArrayList<>(1);
    }
    return inverseRelationships;
  }

  /* see superclass */
  @Override
  public void setRelationships(List<AtomRelationship> relationships) {
    this.relationships = relationships;
  }

  /* see superclass */
  @XmlTransient
  @Override
  public List<AtomTreePosition> getTreePositions() {
    if (treePositions == null) {
      treePositions = new ArrayList<>(1);
    }
    return treePositions;
  }

  /* see superclass */
  @Override
  public void setTreePositions(List<AtomTreePosition> treePositions) {
    this.treePositions = treePositions;
  }

  /* see superclass */
  @Override
  public Map<String, String> getConceptTerminologyIds() {
    if (conceptTerminologyIds == null) {
      conceptTerminologyIds = new HashMap<>(2);
    }
    return conceptTerminologyIds;
  }

  /* see superclass */
  @Override
  public void setConceptTerminologyIds(Map<String, String> conceptTerminologyIds) {
    this.conceptTerminologyIds = conceptTerminologyIds;
  }

  /* see superclass */
  @Override
  public void putConceptTerminologyId(String terminology, String terminologyId) {
    if (conceptTerminologyIds == null) {
      conceptTerminologyIds = new HashMap<>(2);
    }
    conceptTerminologyIds.put(terminology, terminologyId);
  }

  /* see superclass */
  @Override
  public void removeConceptTerminologyId(String terminology) {
    if (conceptTerminologyIds == null) {
      conceptTerminologyIds = new HashMap<>(2);
    }
    conceptTerminologyIds.remove(terminology);
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
  public String getLanguage() {
    return language;
  }

  /* see superclass */
  @Override
  public void setLanguage(String language) {
    this.language = language;
  }

  /* see superclass */
  @Override
  public String getLexicalClassId() {
    return lexicalClassId;
  }

  /* see superclass */
  @Override
  public void setLexicalClassId(String lexicalClassId) {
    this.lexicalClassId = lexicalClassId;
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
  public String getRxcui() {
	if (rxcui == null) {
	  return "";
	}
    return rxcui;
  }

  /* see superclass */
  @Override
  public void setRxcui(String rxcui) {
    this.rxcui = rxcui;
  }

  /**
   * Returns the lower name hash.
   *
   * @return the lower name hash
   */
  @Override
  public String getLowerNameHash() {
    return lowerNameHash;
  }

  /* see superclass */
  @Override
  public String getName() {
    return name;
  }

  /**
   * Returns the name norm.
   *
   * @return the name norm
   */
  @XmlTransient
  @KeywordField(searchable = Searchable.YES)
  @IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "name")))
  public String getNameNorm() {
    return ConfigUtility.normalize(name);
  }

  /* see superclass */
  @Override
  public void setName(String name) {
    this.name = name;
    this.lowerNameHash = ConfigUtility.getMd5(name.toLowerCase());
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
  @XmlElement(type = AtomSubsetMemberJpa.class)
  public List<AtomSubsetMember> getMembers() {
    if (members == null) {
      members = new ArrayList<AtomSubsetMember>();
    }
    return members;
  }

  /* see superclass */
  @Override
  public void setMembers(List<AtomSubsetMember> members) {
    this.members = members;
  }

  /* see superclass */
  @Override
  @XmlElement(type = ComponentHistoryJpa.class)
  public List<ComponentHistory> getComponentHistory() {
    if (componentHistories == null) {
      componentHistories = new ArrayList<>();
    }
    return componentHistories;
  }

  /* see superclass */
  @Override
  public void setComponentHistory(List<ComponentHistory> componentHistory) {
    this.componentHistories = componentHistory;
  }

  /* see superclass */
  @Override
  public String getLastPublishedRank() {
    return lastPublishedRank;
  }

  /* see superclass */
  @Override
  public void setLastPublishedRank(String lastPublishedRank) {
    this.lastPublishedRank = lastPublishedRank;
  }

  /* see superclass */
  @Override
  public void setNotes(List<Note> notes) {
    this.notes = notes;

  }

  /* see superclass */
  @Override
  @XmlElement(type = AtomNoteJpa.class)
  public List<Note> getNotes() {
    if (this.notes == null) {
      this.notes = new ArrayList<>(1);
    }
    return this.notes;
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((codeId == null) ? 0 : codeId.hashCode());
    result = prime * result + ((conceptId == null) ? 0 : conceptId.hashCode());
    result = prime * result + ((descriptorId == null) ? 0 : descriptorId.hashCode());
    result = prime * result + ((language == null) ? 0 : language.hashCode());
    result = prime * result + ((lexicalClassId == null) ? 0 : lexicalClassId.hashCode());
    result = prime * result + ((stringClassId == null) ? 0 : stringClassId.hashCode());
    result = prime * result + ((termType == null) ? 0 : termType.hashCode());
    result = prime * result + ((lastPublishedRank == null) ? 0 : lastPublishedRank.hashCode());
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
    AtomJpa other = (AtomJpa) obj;
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
    if (language == null) {
      if (other.language != null)
        return false;
    } else if (!language.equals(other.language))
      return false;
    if (lexicalClassId == null) {
      if (other.lexicalClassId != null)
        return false;
    } else if (!lexicalClassId.equals(other.lexicalClassId))
      return false;
    if (stringClassId == null) {
      if (other.stringClassId != null)
        return false;
    } else if (!stringClassId.equals(other.stringClassId))
      return false;
    if (termType == null) {
      if (other.termType != null)
        return false;
    } else if (!termType.equals(other.termType))
      return false;
    if (lastPublishedRank == null) {
      if (other.lastPublishedRank != null)
        return false;
    } else if (!lastPublishedRank.equals(other.lastPublishedRank))
      return false;
    return true;
  }

  /* see superclass */
  @Override
  public String toString() {
    return "AtomJpa [name=" + name + ", codeId=" + codeId + ", descriptorId=" + descriptorId
        + ", conceptId=" + conceptId + ", language=" + language + ", lexicalClassId="
        + lexicalClassId + ", stringClassId=" + stringClassId + ", termType=" + termType + ", rxcui=" + rxcui
        + ", workflowStatus=" + workflowStatus + ", lastPublishedRank=" + lastPublishedRank + "] - "
        + super.toString();
  }

}