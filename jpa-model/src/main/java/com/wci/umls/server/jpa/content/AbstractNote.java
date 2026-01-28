package com.wci.umls.server.jpa.content;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.xml.bind.annotation.XmlSeeAlso;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.envers.Audited;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

import com.wci.umls.server.helpers.Note;
import com.wci.umls.server.jpa.workflow.ChecklistNoteJpa;
import com.wci.umls.server.jpa.workflow.WorklistNoteJpa;

/**
 * Abstract implementation of a {@link Note}.
 */
//@Audited
@MappedSuperclass
@XmlSeeAlso({
    CodeNoteJpa.class, ConceptNoteJpa.class, DescriptorNoteJpa.class,
    WorklistNoteJpa.class, ChecklistNoteJpa.class
})
public abstract class AbstractNote implements Note {

  /** The id. */
  @Id
  @GenericGenerator(name = "ExistingOrGeneratedId", type = com.wci.umls.server.jpa.helpers.UseExistingOrGenerateIdGenerator.class)
  @GeneratedValue(generator = "ExistingOrGeneratedId")
  private Long id;

  /** The timestamp. */
  @Column(nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date timestamp = null;

  /** The last modified. */
  @Column(nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  @GenericField(searchable = Searchable.YES, sortable = Sortable.YES)
  private Date lastModified = null;

  /** The last modified by. */
  @Column(nullable = false)
  @KeywordField(searchable = Searchable.YES)
  private String lastModifiedBy;

  /** The note. */
  @Column(nullable = false, length = 4000)
  @FullTextField
  private String note;

  /**
   * Instantiates a new abstract note.
   */
  public AbstractNote() {

  }

  /**
   * Instantiates a new abstract note.
   *
   * @param note the note
   */
  public AbstractNote(Note note) {
    this.id = note.getId();
    this.lastModified = note.getLastModified();
    this.lastModifiedBy = note.getLastModifiedBy();
    this.timestamp = note.getTimestamp();
    this.note = note.getNote();
  }

  /* see superclass */
  @Override
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
  public Date getTimestamp() {
    return timestamp;
  }

  /* see superclass */
  @Override
  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  /* see superclass */
  @Override
  public Date getLastModified() {
    return lastModified;
  }

  /* see superclass */
  @Override
  public void setLastModified(Date lastModified) {
    this.lastModified = lastModified;
  }

  /* see superclass */
  @Override
  public String getLastModifiedBy() {
    return lastModifiedBy;
  }

  /* see superclass */
  @Override
  public void setLastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }

  /* see superclass */
  @Override
  public void setNote(String note) {
    this.note = note;
  }

  /* see superclass */
  @Override
  public String getNote() {
    return this.note;
  }

  @Override
  public String toString() {
    return "AbstractNote [id=" + id + ", timestamp=" + timestamp
        + ", lastModified=" + lastModified + ", lastModifiedBy="
        + lastModifiedBy + ", note=" + note + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((note == null) ? 0 : note.hashCode());
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
    AbstractNote other = (AbstractNote) obj;
    if (note == null) {
      if (other.note != null)
        return false;
    } else if (!note.equals(other.note))
      return false;
    return true;
  }

}