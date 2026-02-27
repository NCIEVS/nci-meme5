/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.model;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

import com.wci.umls.server.model.algo.AlgorithmConfig;
import com.wci.umls.server.model.algo.ProcessConfig;
import com.wci.umls.server.jpa.model.helpers.ObjectToStringBridge;

/**
 * JPA and JAXB enabled implementation of {@link AlgorithmConfig}.
 */
@Entity
@Table(name = "algorithm_configs")
//@Audited
@Indexed
@XmlRootElement(name = "algorithmConfig")
public class AlgorithmConfigJpa extends AbstractAlgorithmInfo<ProcessConfig>
    implements AlgorithmConfig {

  /** The project. */
  @ManyToOne(targetEntity = ProcessConfigJpa.class, optional = false)
  private ProcessConfig process;

  /** The enabled. */
  @Column(nullable = false)
  private boolean enabled = true;

  /** the properties */
  @ElementCollection
  @MapKeyColumn(length = 100)
  @Column(nullable = true, length = 4000)
  private Map<String, String> properties = new HashMap<>();

  /**
   * Instantiates an empty {@link AlgorithmConfigJpa}.
   */
  public AlgorithmConfigJpa() {
    // n/a
  }

  /**
   * Instantiates a {@link AlgorithmConfigJpa} from the specified parameters.
   *
   * @param config the config
   */
  public AlgorithmConfigJpa(AlgorithmConfig config) {
    super(config);
    enabled = config.isEnabled();
    process = config.getProcess();
    properties = new HashMap<>(config.getProperties());
  }

  /* see superclass */
  @XmlTransient
  @Override
  public ProcessConfig getProcess() {
    return process;
  }

  /* see superclass */
  @Override
  public void setProcess(ProcessConfig process) {
    this.process = process;
  }

  /**
   * Returns the process id. for JAXB
   *
   * @return the process id
   */
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
      process = new ProcessConfigJpa();
    }
    process.setId(processId);
  }

  /* see superclass */
  @GenericField(name = "enabled", searchable = Searchable.YES, valueBridge = @ValueBridgeRef(type = ObjectToStringBridge.class))
  @Override
  public boolean isEnabled() {
    return enabled;
  }

  /* see superclass */
  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
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
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
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
    AlgorithmConfigJpa other = (AlgorithmConfigJpa) obj;
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
    return "AlgorithmConfigJpa [processId=" + getProcessId() + ", properties="
        + properties + "] " + super.toString();
  }

}
