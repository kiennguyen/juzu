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

import org.juzu.impl.inject.ScopeController;
import org.juzu.impl.request.Scope;
import org.juzu.impl.spi.inject.InjectBuilder;
import org.juzu.impl.spi.inject.InjectManager;
import org.juzu.impl.spi.inject.cdi.weld.WeldContainer;
import org.juzu.impl.spi.fs.ReadFileSystem;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.inject.Provider;
import javax.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class CDIBootstrap extends InjectBuilder
{

   /** . */
   private Set<Scope> scopes;

   /** . */
   private ClassLoader classLoader;

   /** . */
   private List<ReadFileSystem<?>> fileSystems;

   /** . */
   private ArrayList<AbstractBean> boundBeans;

   /** . */
   private Set<Class<?>> declaredBeans;

   public CDIBootstrap()
   {
      this.scopes = new HashSet<Scope>();
      this.fileSystems = new ArrayList<ReadFileSystem<?>>();
      this.boundBeans = new ArrayList<AbstractBean>();
      this.declaredBeans = new HashSet<Class<?>>();
   }

   @Override
   public <T> InjectBuilder declareBean(Class<T> type, Class<? extends T> implementationType)
   {
      declaredBeans.add(implementationType != null ? implementationType : type);
      return this;
   }

   @Override
   public <T> InjectBuilder declareProvider(Class<T> type, Class<? extends Provider<T>> provider)
   {
      declaredBeans.add(provider);
      return this;
   }

   @Override
   public <P> InjectBuilder addFileSystem(ReadFileSystem<P> fs)
   {
      fileSystems.add(fs);
      return this;
   }

   @Override
   public InjectBuilder addScope(Scope scope)
   {
      scopes.add(scope);
      return this;
   }

   @Override
   public InjectBuilder setClassLoader(ClassLoader classLoader)
   {
      this.classLoader = classLoader;
      return this;
   }

   @Override
   public <T> InjectBuilder bindBean(Class<T> type, Iterable<Annotation> qualifiers, T instance)
   {
      Map<Class<? extends Annotation>, Annotation> qualifierMap = new HashMap<Class<? extends Annotation>, Annotation>();

      //
      if (qualifiers != null)
      {
         for (Annotation qualifier : qualifiers)
         {
            qualifierMap.put(qualifier.annotationType(), qualifier);
         }
      }
      else
      {
         // Introspect qualifiers if the qualifiers were not set
         for (Annotation annotation : type.getAnnotations())
         {
            if (annotation.annotationType().getAnnotation(Qualifier.class) != null)
            {
               qualifierMap.put(annotation.annotationType(), annotation);
            }
         }
      }

      // Add those
      qualifierMap.put(Default.class, AbstractBean.DEFAULT_QUALIFIER);
      qualifierMap.put(Any.class, AbstractBean.ANY_QUALIFIER);

      //
      boundBeans.add(new InstanceBean(type, new HashSet<Annotation>(qualifierMap.values()), instance));
      return this;
   }

   @Override
   public <T> InjectBuilder bindProvider(Class<T> type, Provider<T> provider)
   {
      boundBeans.add(new ProviderBean(type, provider));
      return this;
   }

   @Override
   public InjectManager<?, ?> create() throws Exception
   {
      Container container = new WeldContainer(classLoader, ScopeController.INSTANCE, scopes);
      for (ReadFileSystem<?> fs : fileSystems)
      {
         container.addFileSystem(fs);
      }
      return new CDIManager(container, boundBeans, declaredBeans);
   }
}
