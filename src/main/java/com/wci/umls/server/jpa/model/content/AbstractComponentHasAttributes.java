/**
 * Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.model.content;

import jakarta.persistence.MappedSuperclass;

import com.wci.umls.server.model.content.Attribute;
import com.wci.umls.server.model.content.ComponentHasAttributes;

/**
 * Abstract implementation of {@link ComponentHasAttributes} for use with JPA.
 */
//@Audited 
@MappedSuperclass
public abstract class AbstractComponentHasAttributes extends AbstractComponent
    implements ComponentHasAttributes {

  /** The attributes. */
  //@OneToMany(targetEntity = AttributeJpa.class)
  // @IndexedEmbedded(targetElement = AttributeJpa.class)
  //private List<Attribute> attributes = null;

  /**
   * Instantiates an empty {@link AbstractComponentHasAttributes}.
   */
  public AbstractComponentHasAttributes() {
    // do nothing
  }

  /**
   * Instantiates a {@link AbstractComponentHasAttributes} from the specified
   * parameters.
   *
   * @param component the component
   * @param collectionCopy the deep copy
   */
  public AbstractComponentHasAttributes(ComponentHasAttributes component,
      boolean collectionCopy) {
    super(component);

    
  }



  /* see superclass */
  @Override
  public Attribute getAttributeByName(String name) {
    for (final Attribute attribute : getAttributes()) {
      // If there are more than one, this just returns the first.
      if (attribute.getName().equals(name)) {
        return attribute;
      }
    }
    return null;
  }

}
