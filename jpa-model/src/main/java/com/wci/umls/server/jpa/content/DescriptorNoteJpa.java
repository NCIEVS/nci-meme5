package com.wci.umls.server.jpa.content;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import org.hibernate.envers.Audited;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;

import com.wci.umls.server.helpers.Note;
import com.wci.umls.server.model.content.Descriptor;

/**
 * JPA and JAXB enabled implementation of a {@link Note} connected to a
 * {@link Descriptor}.
 */
@Entity
@Table(name = "descriptor_notes")
//@Audited
@Indexed
@XmlRootElement(name = "descriptorNote")
public class DescriptorNoteJpa extends AbstractNote {

  /** The descriptor. */
  @ManyToOne(targetEntity = DescriptorJpa.class, optional = false)
  private Descriptor descriptor = null;

  /**
   * Instantiates a new descriptor note jpa.
   */
  public DescriptorNoteJpa() {

  }

  /**
   * Instantiates a new descriptor note jpa.
   *
   * @param note the note
   */
  public DescriptorNoteJpa(DescriptorNoteJpa note) {
    super(note);
    descriptor = note.getDescriptor();
  }

  /**
   * Gets the descriptor.
   *
   * @return the descriptor
   */
  @XmlTransient
  public Descriptor getDescriptor() {
    return this.descriptor;
  }

  /**
   * Sets the descriptor.
   *
   * @param descriptor the new descriptor
   */
  public void setDescriptor(Descriptor descriptor) {
    this.descriptor = descriptor;
  }

  /**
   * Returns the descriptor id.
   *
   * @return the descriptor id
   */
  @XmlElement
  @GenericField(searchable = Searchable.YES)
  @IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "descriptor")))
  public Long getDescriptorId() {
    return (descriptor != null) ? descriptor.getId() : 0;
  }

  /**
   * Returns the descriptor name.
   *
   * @return the descriptor name
   */
  @XmlElement
  @FullTextField
  @IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "descriptor")))
  public String getDescriptorName() {
    return (descriptor != null) ? descriptor.getName() : "";
  }

  /**
   * Returns the descriptor name.
   *
   * @return the descriptor name
   */
  @XmlElement
  @KeywordField(searchable = Searchable.YES)
  @IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "descriptor")))
  public String getDescriptorTerminologyId() {
    return (descriptor != null) ? descriptor.getTerminologyId() : "";
  }

  /**
   * Sets the descriptor id.
   *
   * @param descriptorId the descriptor id
   */
  @SuppressWarnings("unused")
  private void setDescriptorId(Long descriptorId) {
    if (descriptor == null) {
      descriptor = new DescriptorJpa();
    }
    descriptor.setId(descriptorId);
  }

  /**
   * Sets the descriptor terminology id.
   *
   * @param terminologyId the descriptor terminology id
   */
  @SuppressWarnings("unused")
  private void setDescriptorTerminologyId(String terminologyId) {
    if (descriptor == null) {
      descriptor = new DescriptorJpa();
    }
    descriptor.setTerminologyId(terminologyId);
  }

  /**
   * Sets the descriptor name.
   *
   * @param name the descriptor name
   */
  @SuppressWarnings("unused")
  private void setDescriptorName(String name) {
    if (descriptor == null) {
      descriptor = new DescriptorJpa();
    }
    descriptor.setName(name);
  }

  /* see superclass */
  @Override
  public String toString() {
    return "DescriptorNoteJpa [descriptorId=" + getDescriptorId() + "] "
        + super.toString();
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result
        + ((getDescriptorId() == null) ? 0 : getDescriptorId().hashCode());
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
    DescriptorNoteJpa other = (DescriptorNoteJpa) obj;
    if (getDescriptorId() == null) {
      if (other.getDescriptorId() != null)
        return false;
    } else if (!getDescriptorId().equals(other.getDescriptorId()))
      return false;
    return true;
  }

}
