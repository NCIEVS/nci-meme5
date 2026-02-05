/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
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
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;

import com.wci.umls.server.jpa.helpers.ObjectToStringBridge;
import com.wci.umls.server.helpers.Note;
import com.wci.umls.server.model.content.Code;

/**
 * JPA and JAXB enabled implementation of a {@link Note} connected to a
 * {@link Code}.
 */
@Entity
@Table(name = "code_notes")
//@Audited
@Indexed
@XmlRootElement(name = "codeNote")
public class CodeNoteJpa extends AbstractNote {

  /** The code. */
  @ManyToOne(targetEntity = CodeJpa.class, optional = false)
  private Code code = null;

  /**
   * Instantiates a new code note jpa.
   */
  public CodeNoteJpa() {

  }

  /**
   * Instantiates a new code note jpa.
   *
   * @param note the note
   */
  public CodeNoteJpa(CodeNoteJpa note) {
    super(note);
    code = note.getCode();
  }

  /**
   * Gets the code.
   *
   * @return the code
   */
  @XmlTransient
  public Code getCode() {
    return this.code;
  }

  /**
   * Sets the code.
   *
   * @param code the new code
   */
  public void setCode(Code code) {
    this.code = code;
  }

  /**
   * Returns the code id.
   *
   * @return the code id
   */
  @XmlElement
  @GenericField(searchable = Searchable.YES,
      valueBridge = @ValueBridgeRef(type = ObjectToStringBridge.class))
  @IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "code")))
  public Long getCodeId() {
    return (code != null) ? code.getId() : 0;
  }

  /**
   * Returns the code name.
   *
   * @return the code name
   */
  @XmlElement
  @FullTextField
  @IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "code")))
  public String getCodeName() {
    return (code != null) ? code.getName() : "";
  }

  /**
   * Returns the code name. For indexing and JAXB
   *
   * @return the code name
   */
  @XmlElement
  @KeywordField(searchable = Searchable.YES)
  @IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "code")))
  public String getCodeTerminologyId() {
    return (code != null) ? code.getTerminologyId() : "";
  }

  /**
   * Sets the code id.
   *
   * @param codeId the code id
   */
  @SuppressWarnings("unused")
  private void setCodeId(Long codeId) {
    if (code == null) {
      code = new CodeJpa();
    }
    code.setId(codeId);
  }

  /**
   * Sets the code terminology id.
   *
   * @param terminologyId the code terminology id
   */
  @SuppressWarnings("unused")
  private void setCodeTerminologyId(String terminologyId) {
    if (code == null) {
      code = new CodeJpa();
    }
    code.setTerminologyId(terminologyId);
  }

  /**
   * Sets the code name.
   *
   * @param name the code name
   */
  @SuppressWarnings("unused")
  private void setCodeName(String name) {
    if (code == null) {
      code = new CodeJpa();
    }
    code.setName(name);
  }

  /* see superclass */
  @Override
  public String toString() {
    return "CodeNoteJpa [codeId=" + getCodeId() + "] " + super.toString();
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result =
        prime * result + ((getCodeId() == null) ? 0 : getCodeId().hashCode());
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
    CodeNoteJpa other = (CodeNoteJpa) obj;
    if (getCodeId() == null) {
      if (other.getCodeId() != null)
        return false;
    } else if (!getCodeId().equals(other.getCodeId()))
      return false;
    return true;
  }
}
