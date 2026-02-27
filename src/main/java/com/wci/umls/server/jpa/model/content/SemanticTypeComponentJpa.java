/*
 *    Copyright 2015 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.model.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

import com.wci.umls.server.jpa.model.helpers.ObjectToStringBridge;

import com.wci.umls.server.model.content.SemanticTypeComponent;
import com.wci.umls.server.model.workflow.WorkflowStatus;

/**
 * JPA and JAXB enabled implementation of {@link SemanticTypeComponent}.
 */
@Entity
@Table(name = "semantic_type_components", uniqueConstraints = @UniqueConstraint(columnNames = {
    "terminologyId", "terminology", "version", "id"
}))
//@Audited
@Indexed
@XmlRootElement(name = "semanticTypeComponent")
public class SemanticTypeComponentJpa extends AbstractComponent
    implements SemanticTypeComponent {

  /** The semantic type. */
  @Column(nullable = false, length = 4000)
  private String semanticType;

  /** The workflow status. */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private WorkflowStatus workflowStatus;

  /**
   * Instantiates an empty {@link SemanticTypeComponentJpa}.
   */
  public SemanticTypeComponentJpa() {
    // do nothing
  }

  /**
   * Instantiates a {@link SemanticTypeComponentJpa} from the specified
   * parameters.
   *
   * @param sty the sty
   */
  public SemanticTypeComponentJpa(SemanticTypeComponent sty) {
    super(sty);
    semanticType = sty.getSemanticType();
    workflowStatus = sty.getWorkflowStatus();
  }

  /* see superclass */
  @Override
  @KeywordField(searchable = Searchable.YES)
  public String getSemanticType() {
    return semanticType;
  }

  /* see superclass */
  @Override
  public void setSemanticType(String semanticType) {
    this.semanticType = semanticType;
  }

  /* see superclass */
  @Override
  @GenericField(searchable = Searchable.YES,
      valueBridge = @ValueBridgeRef(type = ObjectToStringBridge.class))
  public WorkflowStatus getWorkflowStatus() {
    return workflowStatus;
  }

  /* see superclass */
  @Override
  public void setWorkflowStatus(WorkflowStatus workflowStatus) {
    this.workflowStatus = workflowStatus;

  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result =
        prime * result + ((semanticType == null) ? 0 : semanticType.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    SemanticTypeComponentJpa other = (SemanticTypeComponentJpa) obj;
    if (semanticType == null) {
      if (other.semanticType != null)
        return false;
    } else if (!semanticType.equals(other.semanticType))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "SemanticTypeComponentJpa [semanticType=" + semanticType
        + ", workflowStatus=" + workflowStatus + "]";
  }

}
