package com.wci.umls.server.jpa.workflow;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.envers.Audited;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

import com.wci.umls.server.helpers.Note;
import com.wci.umls.server.model.workflow.Checklist;
import com.wci.umls.server.model.workflow.TrackingRecord;

/**
 * JPA-enabled implementation of a {@link Checklist}.
 */
@Entity
@Table(name = "checklists", uniqueConstraints = @UniqueConstraint(columnNames = {
    "name", "project_id"
}))
//@Audited
@Indexed
@XmlRootElement(name = "checklist")
public class ChecklistJpa extends AbstractChecklist {

  /** The notes. */
  @OneToMany(mappedBy = "checklist", targetEntity = ChecklistNoteJpa.class)
  @IndexedEmbedded(targetElement = ChecklistNoteJpa.class)
  private List<Note> notes = new ArrayList<>();
  

  /**
   * Instantiates an empty {@link ChecklistJpa}.
   */
  public ChecklistJpa() {
    // do nothing
  }

  /**
   * Instantiates a {@link ChecklistJpa} from the specified parameters.
   *
   * @param checklist the checklist
   * @param collectionCopy the deep copy
   */
  public ChecklistJpa(Checklist checklist, boolean collectionCopy) {
    super(checklist, collectionCopy);
    if (collectionCopy) {
      notes = new ArrayList<>(checklist.getNotes());
    }
  }

  /* see superclass */
  @XmlElement(type = ChecklistNoteJpa.class)
  @Override
  public List<Note> getNotes() {
    if (notes == null) {
      notes = new ArrayList<Note>();
    }
    return notes;
  }

  /* see superclass */
  @Override
  public void setNotes(List<Note> notes) {
    this.notes = notes;
  }

}
