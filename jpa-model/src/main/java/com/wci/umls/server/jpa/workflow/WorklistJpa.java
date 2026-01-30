/*
 * Copyright 2022 West Coast Informatics - All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains the property of West Coast Informatics
 * The intellectual and technical concepts contained herein are proprietary to
 * West Coast Informatics and may be covered by U.S. and Foreign Patents, patents in process,
 * and are protected by trade secret or copyright law.  Dissemination of this information
 * or reproduction of this material is strictly forbidden.
 */
package com.wci.umls.server.jpa.workflow;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

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

import com.wci.umls.server.helpers.Note;
import com.wci.umls.server.jpa.helpers.CollectionToCsvBridge;
import com.wci.umls.server.jpa.helpers.MaxStateHistoryBridge;
import com.wci.umls.server.jpa.helpers.MinValueBridge;
import com.wci.umls.server.jpa.helpers.ObjectToStringBridge;
import com.wci.umls.server.model.workflow.TrackingRecord;
import com.wci.umls.server.model.workflow.WorkflowStatus;
import com.wci.umls.server.model.workflow.Worklist;

/**
 * JAXB and JPA enabled implementation of {@link Worklist}.
 */
@Entity
@Table(name = "worklists", uniqueConstraints = @UniqueConstraint(columnNames = {
    "name", "workflowBinName", "project_id"
}))
// @Audited
@Indexed
@XmlRootElement(name = "worklist")
public class WorklistJpa extends AbstractChecklist implements Worklist {

  /** The tracking records. */
  @OneToMany(targetEntity = TrackingRecordJpa.class)
  @JoinColumn(name = "trackingRecords_id")
  @JoinTable(name = "worklists_tracking_records",
      inverseJoinColumns = @JoinColumn(name = "trackingRecords_id"),
      joinColumns = @JoinColumn(name = "worklists_id"))
  private List<TrackingRecord> trackingRecords = new ArrayList<>();;

  /** The authors. */
  @ElementCollection
  @CollectionTable(name = "worklist_authors")
  @FullTextField(valueBridge = @ValueBridgeRef(type = CollectionToCsvBridge.class),
      extraction = @ContainerExtraction(extract = ContainerExtract.NO))
  @KeywordField(name = "authorsSort", sortable = Sortable.YES,
      valueBridge = @ValueBridgeRef(type = MinValueBridge.class),
      extraction = @ContainerExtraction(extract = ContainerExtract.NO))
  private List<String> authors = new ArrayList<>();

  /** The reviewers. */
  @ElementCollection
  @CollectionTable(name = "worklist_reviewers")
  @FullTextField(valueBridge = @ValueBridgeRef(type = CollectionToCsvBridge.class),
      extraction = @ContainerExtraction(extract = ContainerExtract.NO))
  @KeywordField(name = "reviewersSort", sortable = Sortable.YES,
      valueBridge = @ValueBridgeRef(type = MinValueBridge.class),
      extraction = @ContainerExtraction(extract = ContainerExtract.NO))
  private List<String> reviewers = new ArrayList<>();

  /** The team (e.g. worklist group). */
  @Column(nullable = true)
  @KeywordField(searchable = Searchable.YES)
  private String team;

  /** The workflow bin. */
  @Column(nullable = true)
  @KeywordField(searchable = Searchable.YES)
  private String workflowBinName;

  /** The epoch. */
  @Column(nullable = false)
  @KeywordField(searchable = Searchable.YES)
  private String epoch;

  /** The workflow status. */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @GenericField(searchable = Searchable.YES,
      valueBridge = @ValueBridgeRef(type = ObjectToStringBridge.class))
  private WorkflowStatus workflowStatus;

  /** The number, also the last part of the name. */
  @Column(nullable = false)
  private int number;

  /** The author time. */
  @Column(nullable = true)
  private Long authorTime;

  /** The reviewer time. */
  @Column(nullable = true)
  private Long reviewerTime;

  /** The workflow state history. */
  @ElementCollection
  @FullTextField(name = "workflowState",
      valueBridge = @ValueBridgeRef(type = MaxStateHistoryBridge.class),
      extraction = @ContainerExtraction(extract = ContainerExtract.NO))
  @KeywordField(name = "workflowStateSort", sortable = Sortable.YES,
      valueBridge = @ValueBridgeRef(type = MaxStateHistoryBridge.class),
      extraction = @ContainerExtraction(extract = ContainerExtract.NO))
  private Map<String, Date> workflowStateHistory = new HashMap<>();

  /** The notes. */
  @OneToMany(mappedBy = "worklist", targetEntity = WorklistNoteJpa.class)
  @IndexedEmbedded(targetType = WorklistNoteJpa.class)
  @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
  private List<Note> notes = new ArrayList<>();

  /** The author available. */
  @Transient
  private boolean authorAvailable;

  /** The reviewer available. */
  @Transient
  private boolean reviewerAvailable;

  /**
   * Instantiates an empty {@link WorklistJpa}.
   */
  public WorklistJpa() {
    // do nothing
  }

  /**
   * Instantiates a {@link WorklistJpa} from the specified parameters.
   *
   * @param worklist the worklist
   * @param collectionCopy the deep copy
   */
  public WorklistJpa(Worklist worklist, boolean collectionCopy) {
    super(worklist, collectionCopy);
    authors = new ArrayList<>(worklist.getAuthors());
    reviewers = new ArrayList<>(worklist.getReviewers());
    team = worklist.getTeam();
    epoch = worklist.getEpoch();
    workflowStatus = worklist.getWorkflowStatus();
    workflowBinName = worklist.getWorkflowBinName();
    number = worklist.getNumber();
    authorTime = worklist.getAuthorTime();
    reviewerTime = worklist.getReviewerTime();
    workflowStateHistory = new HashMap<>(worklist.getWorkflowStateHistory());
    if (collectionCopy) {
      trackingRecords = new ArrayList<>(worklist.getTrackingRecords());
      notes = new ArrayList<>(worklist.getNotes());
    }
  }

  /* see superclass */
  @Override
  public int getNumber() {
    return number;
  }

  /* see superclass */
  @Override
  public void setNumber(int number) {
    this.number = number;
  }

  /* see superclass */
  @Override
  public Long getAuthorTime() {
    return authorTime;
  }

  /* see superclass */
  @Override
  public void setAuthorTime(Long authorTime) {
    this.authorTime = authorTime;
  }

  /* see superclass */
  @Override
  public Long getReviewerTime() {
    return reviewerTime;
  }

  /* see superclass */
  @Override
  public void setReviewerTime(Long reviewerTime) {
    this.reviewerTime = reviewerTime;
  }

  /* see superclass */
  @Override
  public List<String> getAuthors() {
    if (authors == null) {
      authors = new ArrayList<>();
    }
    return authors;
  }

  /* see superclass */
  @Override
  public void setAuthors(List<String> authors) {
    this.authors = authors;
  }

  /* see superclass */
  @Override
  public Map<String, Date> getWorkflowState() {
    return getWorkflowStateHistory();
  }

  /* see superclass */
  @Override
  public Map<String, Date> getWorkflowStateHistory() {
    if (workflowStateHistory == null) {
      workflowStateHistory = new HashMap<>();
    }
    return workflowStateHistory;
  }

  /* see superclass */
  @Override
  public void setWorkflowStateHistory(Map<String, Date> workflowStateHistory) {
    this.workflowStateHistory = workflowStateHistory;
  }

  /* see superclass */
  @Override
  public List<String> getReviewers() {
    if (reviewers == null) {
      reviewers = new ArrayList<>();
    }
    return reviewers;
  }

  /* see superclass */
  @Override
  public void setReviewers(List<String> reviewers) {
    this.reviewers = reviewers;
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
  public String getTeam() {
    return team;
  }

  /* see superclass */
  @Override
  public void setTeam(String team) {
    this.team = team;
  }

  /* see superclass */
  @Override
  public String getWorkflowBinName() {
    return workflowBinName;
  }

  /* see superclass */
  @Override
  public void setWorkflowBinName(String workflowBin) {
    this.workflowBinName = workflowBin;
  }

  /* see superclass */
  @Override
  public String getEpoch() {
    return epoch;
  }

  /* see superclass */
  @Override
  public void setEpoch(String epoch) {
    this.epoch = epoch;
  }

  /* see superclass */
  @XmlElement(type = WorklistNoteJpa.class)
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

  /* see superclass */
  @Override
  public boolean isAuthorAvailable() {
    return authorAvailable;
  }

  /* see superclass */
  @Override
  public void setAuthorAvailable(boolean authorAvailable) {
    this.authorAvailable = authorAvailable;
  }

  /* see superclass */
  @Override
  public boolean isReviewerAvailable() {
    return reviewerAvailable;
  }

  /* see superclass */
  @Override
  public void setReviewerAvailable(boolean reviewerAvailable) {
    this.reviewerAvailable = reviewerAvailable;
  }

  /* see superclass */
  @XmlTransient
  @Override
  public List<TrackingRecord> getTrackingRecords() {
    if (trackingRecords == null) {
      return new ArrayList<>();
    }
    return trackingRecords;
  }

  /* see superclass */
  @Override
  public void setTrackingRecords(List<TrackingRecord> records) {
    this.trackingRecords = records;
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((authors == null) ? 0 : authors.hashCode());
    result = prime * result + ((reviewers == null) ? 0 : reviewers.hashCode());
    result = prime * result + ((workflowBinName == null) ? 0 : workflowBinName.hashCode());
    result = prime * result + ((team == null) ? 0 : team.hashCode());
    result = prime * result + ((epoch == null) ? 0 : epoch.hashCode());
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
    WorklistJpa other = (WorklistJpa) obj;
    if (authors == null) {
      if (other.authors != null)
        return false;
    } else if (!authors.equals(other.authors))
      return false;
    if (reviewers == null) {
      if (other.reviewers != null)
        return false;
    } else if (!reviewers.equals(other.reviewers))
      return false;
    if (workflowBinName == null) {
      if (other.workflowBinName != null)
        return false;
    } else if (!workflowBinName.equals(other.workflowBinName))
      return false;
    if (team == null) {
      if (other.team != null)
        return false;
    } else if (!team.equals(other.team))
      return false;
    if (epoch == null) {
      if (other.epoch != null)
        return false;
    } else if (!epoch.equals(other.epoch))
      return false;
    return true;
  }

  /* see superclass */
  @Override
  public String toString() {
    return "WorklistJpa [id=" + getId() + ", authors=" + authors + ", reviewers=" + reviewers
        + ", team=" + team + ", workflowBin=" + workflowBinName + ", epoch=" + epoch
        + ", workflowStatus=" + workflowStatus + ", number=" + number + ", " + ", authorTime="
        + authorTime + ", reviewerTime=" + reviewerTime + ", workflowStateHistory="
        + workflowStateHistory + "] " + super.toString();
  }

}