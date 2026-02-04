/*
 *    Copyright 2015 West Coast Informatics, Inc
 */
package com.wci.umls.server.jpa.content;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.xml.bind.annotation.XmlSeeAlso;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;

import com.wci.umls.server.helpers.Branch;
import com.wci.umls.server.jpa.helpers.ObjectToStringBridge;
import com.wci.umls.server.model.content.Component;
import com.wci.umls.server.model.content.ComponentHasAttributes;
import com.wci.umls.server.model.meta.IdType;

/**
 * Abstract implementation of {@link ComponentHasAttributes} for use with JPA.
 */
@MappedSuperclass
@XmlSeeAlso({
    ConceptJpa.class
})
public abstract class AbstractComponent extends AbstractHasLastModified
    implements Component {

  /** The id. */
  @Id
  @GenericGenerator(name = "ExistingOrGeneratedId",
      type = com.wci.umls.server.jpa.helpers.UseExistingOrGenerateIdGenerator.class,
      parameters = {
          @Parameter(name = "sequence_name", value = "table_generator"),
          @Parameter(name = "initial_value", value = "1"),
          @Parameter(name = "increment_size", value = "1"),
          @Parameter(name = "optimizer", value = "pooled-lo")
      })
  @GeneratedValue(generator = "ExistingOrGeneratedId")
  private Long id;

  /** The suppressible flag. */
  @Column(nullable = false)
  private boolean suppressible = false;

  /** The obsolete flag. */
  @Column(nullable = false)
  private boolean obsolete = false;

  /** The published flag. */
  @Column(nullable = false)
  private boolean published = false;

  /** The publishable flag. */
  @Column(nullable = false)
  private boolean publishable = true;

  /** The terminology. */
  @Column(nullable = false)
  private String terminology;

  /** The terminology id. */
  @Column(nullable = false)
  private String terminologyId;

  /** The version. */
  @Column(nullable = false)
  private String version;

  /** The branch set to include empty branch. */
  @Column(nullable = true)
  private String branch = Branch.ROOT;

  /**
   * Instantiates an empty {@link AbstractComponent}.
   */
  public AbstractComponent() {
    // do nothing
  }

  /**
   * Instantiates a {@link AbstractComponent} from the specified parameters.
   *
   * @param component the component
   */
  public AbstractComponent(Component component) {
    super(component);
    id = component.getId();
    timestamp = component.getTimestamp();
    lastModified = component.getLastModified();
    lastModifiedBy = component.getLastModifiedBy();
    terminology = component.getTerminology();
    terminologyId = component.getTerminologyId();
    version = component.getVersion();
    publishable = component.isPublishable();
    published = component.isPublished();
    obsolete = component.isObsolete();
    suppressible = component.isSuppressible();
    branch = component.getBranch();
  }

  /* see superclass */
  @Override
  @GenericField(searchable = Searchable.YES,
      valueBridge = @ValueBridgeRef(type = ObjectToStringBridge.class))
  public Long getId() {
    return this.id;
  }

  /* see superclass */
  @Override
  public void setId(Long id) {
    this.id = id;
  }

  /* see superclass */
  @Override
  @GenericField(name = "suppressible", searchable = Searchable.YES,
      valueBridge = @ValueBridgeRef(type = ObjectToStringBridge.class))
  public boolean isSuppressible() {
    return suppressible;
  }

  /* see superclass */
  @Override
  public void setSuppressible(boolean suppressible) {
    this.suppressible = suppressible;
  }

  /* see superclass */
  @GenericField(name = "obsolete", searchable = Searchable.YES,
      valueBridge = @ValueBridgeRef(type = ObjectToStringBridge.class))
  @Override
  public boolean isObsolete() {
    return obsolete;
  }

  /**
   * Indicates whether or not active is the case.
   *
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  @GenericField(name = "active", searchable = Searchable.YES,
      valueBridge = @ValueBridgeRef(type = ObjectToStringBridge.class))
  @IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "obsolete")))
  private boolean isActive() {
    return !obsolete;
  }

  /* see superclass */
  @Override
  public void setObsolete(boolean obsolete) {
    this.obsolete = obsolete;
  }

  /* see superclass */
  @Override
  @GenericField(name = "published", searchable = Searchable.YES,
      valueBridge = @ValueBridgeRef(type = ObjectToStringBridge.class))
  public boolean isPublished() {
    return published;
  }

  /* see superclass */
  @Override
  public void setPublished(boolean published) {
    this.published = published;
  }

  /* see superclass */
  @Override
  @GenericField(name = "publishable", searchable = Searchable.YES,
      valueBridge = @ValueBridgeRef(type = ObjectToStringBridge.class))
  public boolean isPublishable() {
    return publishable;
  }

  /* see superclass */
  @Override
  public void setPublishable(boolean publishable) {
    this.publishable = publishable;
  }

  /* see superclass */
  @Override
  @KeywordField(searchable = Searchable.YES)
  public String getBranch() {
    return branch;
  }

  /* see superclass */
  @Override
  public void setBranch(String branch) {
    this.branch = branch;
  }

  /* see superclass */
  @Override
  @KeywordField(searchable = Searchable.YES)
  public String getVersion() {
    return version;
  }

  /* see superclass */
  @Override
  public void setVersion(String version) {
    this.version = version;
  }

  /* see superclass */
  @KeywordField(searchable = Searchable.YES)
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
  @KeywordField(searchable = Searchable.YES)
  public String getTerminologyId() {
    return terminologyId;
  }

  /* see superclass */
  @Override
  public void setTerminologyId(String terminologyId) {
    this.terminologyId = terminologyId;
  }

  @Override
  public void setType(IdType type) {
    // n/a
  }

  /* see superclass */
  @Override
  public IdType getType() throws Exception {
    return IdType.getIdType(getClass());
  }

  /* see superclass */
  @Override
  public String getName() {
    return null;
  }

  /* see superclass */
  @Override
  public void setName(String name) {
    // n/a
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((branch == null) ? 0 : branch.hashCode());
    result = prime * result + (obsolete ? 1231 : 1237);
    result = prime * result + (suppressible ? 1231 : 1237);
    result =
        prime * result + ((terminology == null) ? 0 : terminology.hashCode());
    result = prime * result
        + ((terminologyId == null) ? 0 : terminologyId.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    AbstractComponent other = (AbstractComponent) obj;
    if (branch == null) {
      if (other.branch != null)
        return false;
    } else if (!branch.equals(other.branch))
      return false;
    if (obsolete != other.obsolete)
      return false;
    if (suppressible != other.suppressible)
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
  public String toString() {
    return "id=" + id + ", terminologyId=" + terminologyId + ", lastModified="
        + getLastModified() + ", lastModifiedBy=" + getLastModifiedBy()
        + ", suppressible=" + suppressible + ", obsolete=" + obsolete
        + ", published=" + published + ", publishable=" + publishable
        + ", terminology=" + terminology + ", version=" + version + ", branch="
        + branch;
  }

}