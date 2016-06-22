package com.wci.umls.server.jpa.actions;

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

import com.wci.umls.server.jpa.content.AbstractComponent;
import com.wci.umls.server.model.actions.ChangeEvent;

/**
 * JAXB enabled implementation of a {@link ChangeEvent}. NOTE: this object
 * cannot be effectively "reserialized" because it uses
 * {@link AbstractComponent}
 *
 * @param <T> the type
 */
@XmlRootElement(name = "change")
public class ChangeEventJpa<T extends AbstractComponent> implements
    ChangeEvent<T> {

  /** The id. */
  private Long id;

  /** The timestamp. */
  private Date timestamp;

  /** The last modified. */
  private Date lastModified;

  /** The last modified by. */
  private String lastModifiedBy;

  /** The name. */
  private String name;

  /** The type. */
  private String type;

  /** The old value. */
  private T oldValue;

  /** The new value. */
  private T newValue;

  /**
   * Instantiates an empty {@link ChangeEventJpa}.
   */
  public ChangeEventJpa() {
    // n/a
  }

  /**
   * Instantiates a {@link ChangeEventJpa} from the specified parameters.
   *
   * @param event the event
   */
  public ChangeEventJpa(ChangeEvent<T> event) {
    id = event.getId();
    timestamp = event.getTimestamp();
    lastModified = event.getLastModified();
    lastModifiedBy = event.getLastModifiedBy();
    name = event.getName();
    type = event.getType();
    oldValue = event.getOldValue();
    newValue = event.getNewValue();
  }

  /**
   * Instantiates a {@link ChangeEventJpa} from the specified parameters.
   *
   * @param name the name
   * @param type the type
   * @param oldValue the old value
   * @param newValue the new value
   */
  public ChangeEventJpa(String name, String type, T oldValue, T newValue) {
    if (newValue != null) {
      id = newValue.getId();
      timestamp = newValue.getTimestamp();
      lastModified = newValue.getLastModified();
      lastModifiedBy = newValue.getLastModifiedBy();
    } else if (oldValue != null) {
      id = oldValue.getId();
      timestamp = oldValue.getTimestamp();
      lastModified = oldValue.getLastModified();
      lastModifiedBy = oldValue.getLastModifiedBy();
    }
    this.name = name;
    this.type = type;
    this.oldValue = oldValue;
    this.newValue = newValue;
  }

  /* see superclass */
  @Override
  public Long getId() {
    return id;
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
  public String getName() {
    return name;
  }

  /* see superclass */
  @Override
  public void setName(String name) {
    this.name = name;
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
  public T getOldValue() {
    return oldValue;
  }

  /* see superclass */
  @Override
  public void setOldValue(T oldValue) {
    this.oldValue = oldValue;
  }

  /* see superclass */
  @Override
  public T getNewValue() {
    return newValue;
  }

  /* see superclass */
  @Override
  public void setNewValue(T newValue) {
    this.newValue = newValue;
  }

  /* see superclass */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((newValue == null) ? 0 : newValue.hashCode());
    result = prime * result + ((oldValue == null) ? 0 : oldValue.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    return result;
  }

  /* see superclass */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ChangeEventJpa<?> other = (ChangeEventJpa<?>) obj;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (newValue == null) {
      if (other.newValue != null)
        return false;
    } else if (!newValue.equals(other.newValue))
      return false;
    if (oldValue == null) {
      if (other.oldValue != null)
        return false;
    } else if (!oldValue.equals(other.oldValue))
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
    return "ChangeEventJpa [id=" + id + ", timestamp=" + timestamp
        + ", lastModified=" + lastModified + ", lastModifiedBy="
        + lastModifiedBy + ", name=" + name + ", type=" + type + ", oldValue="
        + oldValue + ", newValue=" + newValue + "]";
  }

}