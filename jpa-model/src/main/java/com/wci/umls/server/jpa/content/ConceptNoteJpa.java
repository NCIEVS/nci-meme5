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
import com.wci.umls.server.model.content.Concept;

/**
 * JPA and JAXB enabled implementation of a {@link Note} connected to a
 * {@link Concept}.
 */
@Entity
@Table(name = "concept_notes")
//@Audited
@Indexed
@XmlRootElement(name = "conceptNote")
public class ConceptNoteJpa extends AbstractNote {

  /** The concept. */
  @ManyToOne(targetEntity = ConceptJpa.class, optional = false)
  private Concept concept = null;

  /**
   * Instantiates a new concept note jpa.
   */
  public ConceptNoteJpa() {
    // n/a
  }

  /**
   * Instantiates a new concept note jpa.
   *
   * @param note the note
   */
  public ConceptNoteJpa(ConceptNoteJpa note) {
    super(note);
    concept = note.getConcept();
  }

  /**
   * Gets the concept.
   *
   * @return the concept
   */
  @XmlTransient
  public Concept getConcept() {
    return this.concept;
  }

  /**
   * Sets the concept.
   *
   * @param concept the new concept
   */
  public void setConcept(Concept concept) {
    this.concept = concept;
  }

  /**
   * Returns the concept id.
   *
   * @return the concept id
   */
  @XmlElement
  @GenericField(searchable = Searchable.YES)
  @IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "concept")))
  public Long getConceptId() {
    return (concept != null) ? concept.getId() : 0;
  }

  /**
   * Returns the concept name.
   *
   * @return the concept name
   */
  @XmlElement
  @FullTextField
  @IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "concept")))
  public String getConceptName() {
    return (concept != null) ? concept.getName() : "";
  }

  /**
   * Returns the concept name.
   *
   * @return the concept name
   */
  @XmlElement
  @KeywordField(searchable = Searchable.YES)
  @IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "concept")))
  public String getConceptTerminologyId() {
    return (concept != null) ? concept.getTerminologyId() : "";
  }

  /**
   * Sets the concept id.
   *
   * @param conceptId the concept id
   */
  @SuppressWarnings("unused")
  private void setConceptId(Long conceptId) {
    if (concept == null) {
      concept = new ConceptJpa();
    }
    concept.setId(conceptId);
  }

  /**
   * Sets the concept terminology id.
   *
   * @param terminologyId the concept terminology id
   */
  @SuppressWarnings("unused")
  private void setConceptTerminologyId(String terminologyId) {
    if (concept == null) {
      concept = new ConceptJpa();
    }
    concept.setTerminologyId(terminologyId);
  }

  /**
   * Sets the concept name.
   *
   * @param name the concept name
   */
  @SuppressWarnings("unused")
  private void setConceptName(String name) {
    if (concept == null) {
      concept = new ConceptJpa();
    }
    concept.setName(name);
  }

  /* see superclass */
  @Override
  public String toString() {
    return "ConceptNoteJpa [conceptId=" + getConceptId() + "] "
        + super.toString();
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result
        + ((getConceptId() == null) ? 0 : getConceptId().hashCode());
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
    ConceptNoteJpa other = (ConceptNoteJpa) obj;
    if (getConceptId() == null) {
      if (other.getConceptId() != null)
        return false;
    } else if (!getConceptId().equals(other.getConceptId()))
      return false;
    return true;
  }

}
