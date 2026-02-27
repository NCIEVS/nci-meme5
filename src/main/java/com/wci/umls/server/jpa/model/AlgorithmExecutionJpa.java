/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

import com.wci.umls.server.jpa.model.helpers.ObjectToStringBridge;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;

import com.wci.umls.server.model.algo.AlgorithmConfig;
import com.wci.umls.server.model.algo.AlgorithmExecution;
import com.wci.umls.server.model.algo.ProcessExecution;

/**
 * JPA and JAXB enabled implementation of {@link AlgorithmExecution}.
 */
@Entity
// TODO: fix this
@Table(name = "algorithm_execs")
//@Audited
@Indexed
@XmlRootElement(name = "algorithmExecution")
public class AlgorithmExecutionJpa extends
    AbstractAlgorithmInfo<ProcessExecution> implements AlgorithmExecution {

  /** The last modified. */
  @Column(nullable = true)
  @Temporal(TemporalType.TIMESTAMP)
  @GenericField(searchable = Searchable.YES, sortable = Sortable.YES)
  private Date startDate;

  /** The finish date. */
  @Column(nullable = true)
  @Temporal(TemporalType.TIMESTAMP)
  @GenericField(searchable = Searchable.YES, sortable = Sortable.YES)
  private Date finishDate;

  /** The fail date. */
  @Column(nullable = true)
  @Temporal(TemporalType.TIMESTAMP)
  @GenericField(searchable = Searchable.YES, sortable = Sortable.YES)
  private Date failDate;

  /** The algorithm config id. */
  @Column(nullable = false)
  @GenericField(searchable = Searchable.YES,
      valueBridge = @ValueBridgeRef(type = ObjectToStringBridge.class))
  private Long algorithmConfigId;

  /** The activity id. */
  @Column(nullable = true)
  @KeywordField(searchable = Searchable.YES)
  private String activityId;

  /** The project. */
  @ManyToOne(targetEntity = ProcessExecutionJpa.class, optional = false)
  private ProcessExecution process;

  /** Has the algorithm had a warning fired during its execution?. */
  @Column(nullable = false)
  private boolean warning = false;

  /** the properties */
  @ElementCollection
  @MapKeyColumn(length = 100)
  @Column(nullable = true, length = 4000)
  private Map<String, String> properties = new HashMap<>();

  /**
   * Instantiates an empty {@link AlgorithmExecutionJpa}.
   */
  public AlgorithmExecutionJpa() {
    // n/a
  }

  /**
   * Instantiates a {@link AlgorithmExecutionJpa} from the specified parameters.
   *
   * @param exec the exec
   */
  public AlgorithmExecutionJpa(AlgorithmExecution exec) {
    super(exec);
    process = exec.getProcess();
    startDate = exec.getStartDate();
    finishDate = exec.getFinishDate();
    failDate = exec.getFailDate();
    algorithmConfigId = exec.getAlgorithmConfigId();
    activityId = exec.getActivityId();
    warning = exec.isWarning();
    properties = new HashMap<>(exec.getProperties());
  }

  /**
   * Instantiates a {@link AlgorithmExecutionJpa} from the specified parameters.
   * 
   * @param config the config
   */
  public AlgorithmExecutionJpa(AlgorithmConfig config) {
    super(config);
    // Clear out the id copied from the config
    this.setId(null);
    algorithmConfigId = config.getId();
    properties = new HashMap<>(config.getProperties());
  }

  /* see superclass */
  @Override
  public Date getStartDate() {
    return startDate;
  }

  /* see superclass */
  @Override
  public void setStartDate(Date startDate) {
    this.startDate = startDate;
  }

  /* see superclass */
  @Override
  public Date getFinishDate() {
    return finishDate;
  }

  /* see superclass */
  @Override
  public void setFinishDate(Date finishDate) {
    this.finishDate = finishDate;
  }

  /* see superclass */
  @Override
  public Date getFailDate() {
    return failDate;
  }

  /* see superclass */
  @Override
  public void setFailDate(Date failDate) {
    this.failDate = failDate;
  }

  /* see superclass */

  @Override
  @XmlTransient
  public ProcessExecution getProcess() {
    return process;
  }

  /* see superclass */
  @Override
  public void setProcess(ProcessExecution process) {
    this.process = process;
  }

  /**
   * Returns the process id. for JAXB
   *
   * @return the process id
   */
  @GenericField(searchable = Searchable.YES,
      valueBridge = @ValueBridgeRef(type = ObjectToStringBridge.class))
  @IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "process")))
  public Long getProcessId() {
    return process == null ? null : process.getId();
  }

  /**
   * Sets the process id.
   *
   * @param processId the process id
   */
  public void setProcessId(Long processId) {
    if (process == null) {
      process = new ProcessExecutionJpa();
    }
    process.setId(processId);
  }

  /* see superclass */
  @Override
  public Long getAlgorithmConfigId() {
    return algorithmConfigId;
  }

  /* see superclass */
  @Override
  public void setAlgorithmConfigId(Long algorithmConfigId) {
    this.algorithmConfigId = algorithmConfigId;
  }

  /* see superclass */
  @Override
  public String getActivityId() {
    return activityId;
  }

  /**
   * Sets the activity id.
   *
   * @param activityId the activity id
   */
  /* see superclass */
  @Override
  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

  /* see superclass */
  @Override
  @XmlTransient
  public Map<String, String> getProperties() {
    if (properties == null) {
      properties = new HashMap<>();
    }
    return properties;
  }

  /* see superclass */
  @Override
  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  /* see superclass */
  @Override
  public Boolean isWarning() {
    return warning;
  }

  /* see superclass */
  @Override
  public void setWarning(Boolean warning) {
    this.warning = warning;
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result
        + ((algorithmConfigId == null) ? 0 : algorithmConfigId.hashCode());
    result =
        prime * result + ((activityId == null) ? 0 : activityId.hashCode());
    result = prime * result
        + ((getProcessId() == null) ? 0 : getProcessId().hashCode());
    result =
        prime * result + ((properties == null) ? 0 : properties.hashCode());
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
    AlgorithmExecutionJpa other = (AlgorithmExecutionJpa) obj;
    if (algorithmConfigId == null) {
      if (other.algorithmConfigId != null)
        return false;
    } else if (!algorithmConfigId.equals(other.algorithmConfigId))
      return false;
    if (activityId == null) {
      if (other.activityId != null)
        return false;
    } else if (!activityId.equals(other.activityId))
      return false;
    if (getProcessId() == null) {
      if (other.getProcessId() != null)
        return false;
    } else if (!getProcessId().equals(other.getProcessId()))
      return false;
    if (properties == null) {
      if (other.properties != null)
        return false;
    } else if (!properties.equals(other.properties))
      return false;
    return true;
  }

  /* see superclass */
  @Override
  public String toString() {
    return "AlgorithmExecutionJpa [startDate=" + startDate + ", finishDate="
        + finishDate + ", failDate=" + failDate + ", processId="
        + getProcessId() + ", activityId=" + activityId + ", algorithmConfigId="
        + algorithmConfigId + "] " + super.toString();
  }

}
