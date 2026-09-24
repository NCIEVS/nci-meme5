/*
 *    Copyright 2016 West Coast Informatics, LLC
 */
package com.wci.umls.server.jpa.model.helpers;

import org.hibernate.HibernateException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.boot.internal.EnversIntegrator;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.event.spi.EnversListenerDuplicationStrategy;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import com.wci.umls.server.helpers.PropertyUtility;

/**
 * Provides integration for Envers into Hibernate, which mainly means
 * registering the proper event listeners.
 *
 * For this project, we will only be logging delete events.
 */
public class TermServerEnversIntegrator  extends EnversIntegrator {

  /* see superclass */
  @Override
  public void integrate(Metadata metadata, BootstrapContext bootstrapContext,
    SessionFactoryImplementor sessionFactory) {

    // Avoid custom behavior is autoregister is true
    try {
      if (!"true".equals(PropertyUtility.getProperties()
          .getProperty("hibernate.listeners.envers.autoRegister"))) {

        super.integrate(metadata, bootstrapContext, sessionFactory);

        ServiceRegistryImplementor serviceRegistry =
            sessionFactory.getServiceRegistry();

        EnversService enversService =
            serviceRegistry.getService(EnversService.class);
        if (enversService == null) {
          throw new HibernateException("Unable to retrieve EnversService");
        }
        if (!enversService.isInitialized()) {
          throw new HibernateException(
              "Expecting EnversService to have been initialized prior to call to EnversIntegrator#integrate");
        }
        EventListenerRegistry listenerRegistry =
            serviceRegistry.getService(EventListenerRegistry.class);
        if (listenerRegistry == null) {
          throw new HibernateException(
              "Unable to retrieve EventListenerRegistry");
        }

        listenerRegistry.addDuplicationStrategy(EnversListenerDuplicationStrategy.INSTANCE);

        // if (enversConfiguration.getEntCfg().hasAuditedEntities()) {
        listenerRegistry.appendListeners(EventType.POST_INSERT,
            new EmptyEnversPostInsertEventListenerImpl(enversService));
        listenerRegistry.appendListeners(EventType.POST_DELETE,
            new CustomEnversPostDeleteEventListenerImpl(enversService));
        // }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }
}
