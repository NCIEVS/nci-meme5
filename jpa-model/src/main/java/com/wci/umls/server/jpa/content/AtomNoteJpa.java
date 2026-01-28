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
import com.wci.umls.server.model.content.Atom;

/**
 * JPA and JAXB enabled implementation of a {@link Note} connected to a
 * {@link Atom}.
 */
@Entity
@Table(name = "atom_notes")
//@Audited
@Indexed
@XmlRootElement(name = "atomNote")
public class AtomNoteJpa extends AbstractNote {

  /** The atom. */
  @ManyToOne(targetEntity = AtomJpa.class, optional = false)
  private Atom atom = null;

  /**
   * Instantiates a new atom note jpa.
   */
  public AtomNoteJpa() {
    // n/a
  }

  /**
   * Instantiates a new atom note jpa.
   *
   * @param note the note
   */
  public AtomNoteJpa(AtomNoteJpa note) {
    super(note);
    atom = note.getAtom();
  }

  /**
   * Gets the atom.
   *
   * @return the atom
   */
  @XmlTransient
  public Atom getAtom() {
    return this.atom;
  }

  /**
   * Sets the atom.
   *
   * @param atom the new atom
   */
  public void setAtom(Atom atom) {
    this.atom = atom;
  }

  /**
   * Returns the atom id.
   *
   * @return the atom id
   */
  @XmlElement
  @GenericField(searchable = Searchable.YES)
  @IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "atom")))
  public Long getAtomId() {
    return (atom != null) ? atom.getId() : 0;
  }

  /**
   * Returns the atom name.
   *
   * @return the atom name
   */
  @XmlElement
  @FullTextField
  @IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "atom")))
  public String getAtomName() {
    return (atom != null) ? atom.getName() : "";
  }

  /**
   * Returns the atom name.
   *
   * @return the atom name
   */
  @XmlElement
  @KeywordField(searchable = Searchable.YES)
  @IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "atom")))
  public String getAtomTerminologyId() {
    return (atom != null) ? atom.getTerminologyId() : "";
  }

  /**
   * Sets the atom id.
   *
   * @param atomId the atom id
   */
  @SuppressWarnings("unused")
  private void setAtomId(Long atomId) {
    if (atom == null) {
      atom = new AtomJpa();
    }
    atom.setId(atomId);
  }

  /**
   * Sets the atom terminology id.
   *
   * @param terminologyId the atom terminology id
   */
  @SuppressWarnings("unused")
  private void setAtomTerminologyId(String terminologyId) {
    if (atom == null) {
      atom = new AtomJpa();
    }
    atom.setTerminologyId(terminologyId);
  }

  /**
   * Sets the atom name.
   *
   * @param name the atom name
   */
  @SuppressWarnings("unused")
  private void setAtomName(String name) {
    if (atom == null) {
      atom = new AtomJpa();
    }
    atom.setName(name);
  }

  /* see superclass */
  @Override
  public String toString() {
    return "AtomNoteJpa [atomId=" + getAtomId() + "] "
        + super.toString();
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result
        + ((getAtomId() == null) ? 0 : getAtomId().hashCode());
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
    AtomNoteJpa other = (AtomNoteJpa) obj;
    if (getAtomId() == null) {
      if (other.getAtomId() != null)
        return false;
    } else if (!getAtomId().equals(other.getAtomId()))
      return false;
    return true;
  }

}
