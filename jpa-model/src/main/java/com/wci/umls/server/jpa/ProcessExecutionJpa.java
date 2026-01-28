/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.hibernate.envers.Audited;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

import com.wci.umls.server.AlgorithmExecution;
import com.wci.umls.server.ProcessConfig;
import com.wci.umls.server.ProcessExecution;

/**
 * JPA and JAXB enabled implementation of {@link ProcessExecution}.
 */
@Entity
@Table(name = "process_executions")
//@Audited
@Indexed
@XmlRootElement(name = "processExecution")
public class ProcessExecutionJpa extends AbstractProcessInfo<AlgorithmExecution>
    implements ProcessExecution {

  /** The stop date. */
  @Column(nullable = true)
  @Temporal(TemporalType.TIMESTAMP)
  @GenericField(searchable = Searchable.YES, sortable = Sortable.YES)
  private Date startDate;

  /** The stop date. */
  @Column(nullable = true)
  @Temporal(TemporalType.TIMESTAMP)
  @GenericField(searchable = Searchable.YES, sortable = Sortable.YES)
  private Date stopDate;

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

  /** The process config id. */
  @Column(nullable = false)
  @GenericField(searchable = Searchable.YES)
  private Long processConfigId;

  /** The work id. */
  @Column(nullable = true)
  @KeywordField(searchable = Searchable.YES)
  private String workId;

  /** The type. */
  @Column(nullable = false)
  @KeywordField(searchable = Searchable.YES)
  private String type;

  /** The input path. */
  @Column(nullable = true)
  private String inputPath;
  
  /** The log path. */
  @Column(nullable = true)
  private String logPath;


  /** Has the algorithm had a warning fired during its execution. */
  @Column(nullable = false)
  private boolean warning = false;

  /** The steps . */
  @OneToMany(mappedBy = "process", targetEntity = AlgorithmExecutionJpa.class)
  @OrderColumn
  private List<AlgorithmExecution> steps = new ArrayList<>();

  /** The execution info. */
  @ElementCollection()
  @Column(nullable = true)
  private Map<String, String> executionInfo;

  /**
   * Instantiates an empty {@link ProcessExecutionJpa}.
   */
  public ProcessExecutionJpa() {
    // n/a
  }

  /**
   * Instantiates a {@link ProcessExecutionJpa} from the specified parameters.
   *
   * @param exec the exec
   */
  public ProcessExecutionJpa(ProcessExecution exec) {
    super(exec);
    startDate = exec.getStartDate();
    stopDate = exec.getStopDate();
    finishDate = exec.getStartDate();
    finishDate = exec.getFinishDate();
    failDate = exec.getFailDate();
    processConfigId = exec.getProcessConfigId();
    workId = exec.getWorkId();
    steps = new ArrayList<>(exec.getSteps());
    type = exec.getType();
    inputPath = exec.getInputPath();
    logPath = exec.getLogPath();
    executionInfo = new HashMap<>(exec.getExecutionInfo());
    warning = exec.isWarning();
  }

  /**
   * Instantiates a {@link ProcessExecutionJpa} from the specified parameters.
   *
   * @param config the config
   */
  public ProcessExecutionJpa(ProcessConfig config) {
    super(config);
    // Clear out the id copied from the config
    this.setId(null);
    processConfigId = config.getId();
    type = config.getType();
    inputPath = config.getInputPath();
    logPath = config.getLogPath();
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
  public Date getStopDate() {
    return stopDate;
  }

  /* see superclass */
  @Override
  public void setStopDate(Date stopDate) {
    this.stopDate = stopDate;
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
  @XmlElement(type = AlgorithmExecutionJpa.class)
  public List<AlgorithmExecution> getSteps() {
    if (steps == null) {
      steps = new ArrayList<>();
    }
    return steps;
  }

  /* see superclass */
  @Override
  public void setSteps(List<AlgorithmExecution> steps) {
    this.steps = steps;
  }

  /* see superclass */
  @Override
  public Map<String, String> getExecutionInfo() {
    if (executionInfo == null) {
      executionInfo = new HashMap<>();
    }
    return executionInfo;
  }

  /* see superclass */
  @Override
  public void setExecutionInfo(Map<String, String> executionInfo) {
    this.executionInfo = executionInfo;
  }

  /* see superclass */
  @Override
  public Long getProcessConfigId() {
    return processConfigId;
  }

  /* see superclass */
  @Override
  public void setProcessConfigId(Long processConfigId) {
    this.processConfigId = processConfigId;
  }

  /* see superclass */
  @Override
  public String getWorkId() {
    return workId;
  }

  /* see superclass */
  @Override
  public void setWorkId(String workId) {
    this.workId = workId;
  }

  /* see superclass */
  @Override
  public String getType() {
    return type;
  }

  /* see superclass */
  @Override
  public void setType(String type) {
    this.type = type;
  }

  /* see superclass */
  @Override
  public String getInputPath() {
    return inputPath;
  }

  /* see superclass */
  @Override
  public void setInputPath(String inputPath) {
    this.inputPath = inputPath;
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
        + ((processConfigId == null) ? 0 : processConfigId.hashCode());
    result = prime * result + ((workId == null) ? 0 : workId.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
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
    ProcessExecutionJpa other = (ProcessExecutionJpa) obj;
    if (processConfigId == null) {
      if (other.processConfigId != null)
        return false;
    } else if (!processConfigId.equals(other.processConfigId))
      return false;
    if (workId == null) {
      if (other.workId != null)
        return false;
    } else if (!workId.equals(other.workId))
      return false;
    if (type == null) {
      if (other.type != null)
        return false;
    } else if (!type.equals(other.type))
      return false;
    return true;
  }

  /* see superclass */
  @Override
  public String toString() {
    return "ProcessExecutionJpa [startDate=" + startDate + ", stopDate="
        + stopDate + ", finishDate=" + finishDate + ", failDate=" + failDate
        + ", steps=" + steps + ", workId=" + workId + ", processConfigId="
        + processConfigId + ", type=" + type + "] " + super.toString();

  }

  @Override
  public String getLogPath() {
    return logPath;
  }

  @Override
  public void setLogPath(String logPath) {
    this.logPath = logPath;
  }
}
