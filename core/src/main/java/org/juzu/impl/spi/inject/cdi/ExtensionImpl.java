/*
 * Copyright (C) 2011 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.juzu.impl.spi.inject.cdi;

import org.juzu.impl.inject.Export;
import org.juzu.impl.request.Scope;
import org.juzu.impl.spi.inject.InjectManager;
import org.juzu.impl.utils.Tools;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

/**
 * Juzu CDI extension.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public class ExtensionImpl implements Extension
{

   /** . */
   private final CDIManager manager;

   /** The singletons to shut down. */
   private List<Bean<?>> singletons;

   public ExtensionImpl()
   {
      this.manager = CDIManager.boot.get();
      this.singletons = new ArrayList<Bean<?>>();
   }

   <T> void processAnnotatedType(@Observes ProcessAnnotatedType<T> pat)
   {

      AnnotatedType<T> annotatedType = pat.getAnnotatedType();
      Class<T> type = annotatedType.getJavaClass();
      boolean veto; 
      if (type.getName().startsWith("org.juzu."))
      {
         veto = !manager.declaredBeans.contains(type);
      }
      else
      {
         veto = false;
      }
      
      //
      if (!veto)
      {
         for (AbstractBean boundBean : manager.boundBeans)
         {
            if (boundBean.getBeanClass().isAssignableFrom(type))
            {
               veto = true;
            }
         }
      }
      
      //
      if (veto)
      {
         pat.veto();
      }
   }

   void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager beanManager)
   {
      Container container = Container.boot.get();

      //
      for (Scope scope : container.scopes)
      {
         event.addContext(new ContextImpl(container.scopeController, scope, scope.getAnnotationType()));
      }

      // Add the manager
      event.addBean(new InstanceBean(InjectManager.class, Tools.set(AbstractBean.DEFAULT_QUALIFIER, AbstractBean.ANY_QUALIFIER), manager));

      // Add singletons
      for (AbstractBean bean : manager.boundBeans)
      {
         event.addBean(bean);
      }
   }

   void processBean(@Observes ProcessBean event, BeanManager beanManager)
   {
      Bean bean = event.getBean();
      manager.beans.add(bean);
      
      //
      if (bean.getScope() == Singleton.class)
      {
         singletons.add(bean);
      }
   }

   public void beforeShutdown(@Observes BeforeShutdown event, BeanManager beanManager) 
   {
      // Take care of destroying singletons
      for (Bean singleton : singletons)
      {
         CreationalContext cc = beanManager.createCreationalContext(singleton);
         Object o = beanManager.getReference(singleton, singleton.getBeanClass(), cc);
         singleton.destroy(o, cc);
      }
   }
}