package org.larpconnect.data.hibernate;

import com.google.common.util.concurrent.Service;
import org.hibernate.SessionFactory;

/** Service interface for managing the Hibernate SessionFactory lifecycle. */
public interface HibernateService extends Service {
  /** Returns the active Hibernate SessionFactory. */
  SessionFactory getSessionFactory();
}
