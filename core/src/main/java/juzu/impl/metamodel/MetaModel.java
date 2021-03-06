/*
 * Copyright (C) 2012 eXo Platform SAS.
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

package juzu.impl.metamodel;

import juzu.impl.compiler.AnnotationData;
import juzu.impl.compiler.BaseProcessor;
import juzu.impl.compiler.ProcessingException;
import juzu.impl.compiler.ProcessingContext;
import juzu.impl.common.JSON;
import juzu.impl.common.Logger;

import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public final class MetaModel extends MetaModelObject {

  /** . */
  public static final Logger log = BaseProcessor.getLogger(MetaModel.class);

  /** . */
  public ProcessingContext env;

  /** . */
  private final EventQueue queue = new EventQueue();

  /** . */
  private final EventQueue dispatch = new EventQueue();

  /** . */
  private static final ThreadLocal<MetaModel> current = new ThreadLocal<MetaModel>();

  /** The meta model plugins. */
  final LinkedHashMap<String, MetaModelPlugin> plugins = new LinkedHashMap<String, MetaModelPlugin>();

  /** . */
  private Set<Class> supportedAnnotations;

  /** . */
  private boolean queuing = false;

  public MetaModel() {
  }

  public Set<Class> getSupportedAnnotations() {
    return supportedAnnotations;
  }

  public void init(ProcessingContext env) {

    this.env = env;

    try {
      LinkedHashMap<String, MetaModelPlugin> plugins = new LinkedHashMap<String, MetaModelPlugin>();
      StringBuilder msg = new StringBuilder("Using plugins:");
      for (MetaModelPlugin plugin : env.loadServices(MetaModelPlugin.class)) {
        msg.append(" ").append(plugin.getName());
        plugins.put(plugin.getName(), plugin);
      }
      log.log(msg);

      // Initialize plugins
      for (MetaModelPlugin plugin : plugins.values()) {
        plugin.init(this);
      }

      // Collect processed annotations
      HashSet<Class> supportedAnnotations = new HashSet<Class>();
      for (MetaModelPlugin plugin : plugins.values()) {
        Set<Class<? extends Annotation>> processed = plugin.getAnnotationTypes();
        log.log("Plugin " + plugin.getName() + " wants to process " + processed);
        supportedAnnotations.addAll(processed);
      }

      //
      this.plugins.putAll(plugins);
      this.supportedAnnotations = supportedAnnotations;
    } finally {
      this.env = null;
    }

  }

  public JSON toJSON() {
    JSON json = new JSON();
    for (MetaModelPlugin plugin : plugins.values()) {
      JSON pluginJSON = plugin.toJSON(this);
      if (pluginJSON != null) {
        json.set(plugin.getName(), pluginJSON);
      }
    }
    return json;
  }

  //

  public void postActivate(ProcessingContext env) {
    this.env = env;
    current.set(this);

    //
    queuing = true;
    try {
      //
      garbage(this, this, new HashSet<MetaModelObject>());

      //
      for (MetaModelPlugin plugin : plugins.values()) {
        plugin.postActivate(this);
      }
    }
    finally {
      queuing = false;
    }
  }

  public void processAnnotation(Element element, String annotationType, AnnotationData annotationData) throws ProcessingException {
    queuing = true;
    try {
      MetaModel.log.log("Processing annotation " + element);

      //
      for (MetaModelPlugin plugin : plugins.values()) {
        plugin.processAnnotation(this, element, annotationType, annotationData);
      }
    }
    finally {
      queuing = false;
    }
  }

  public void postProcessAnnotations() throws ProcessingException {
    queuing = true;
    try {
      for (MetaModelPlugin plugin : plugins.values()) {
        plugin.postProcessAnnotations(this);
      }
    }
    finally {
      queuing = false;
    }

    //
    for (MetaModelPlugin plugin : plugins.values()) {
      plugin.processEvents(this, new EventQueue(dispatch));
    }

    // Clear dispatch queue
    dispatch.clear();


    //
    MetaModel.log.log("Post processing");
    for (MetaModelPlugin plugin : plugins.values()) {
      plugin.postProcessEvents(this);
    }
  }

  public void prePassivate() {
    try {
      for (MetaModelPlugin plugin : plugins.values()) {
        plugin.prePassivate(this);
      }
    }
    finally {
      this.env = null;
      current.set(null);
    }
  }

  //

  private void garbage(MetaModel model, MetaModelObject object, HashSet<MetaModelObject> visited) {
    if (!visited.contains(object)) {
      visited.add(this);

      //
      for (MetaModelObject child : object.getChildren()) {
        garbage(model, child, visited);
      }

      //
      if (!object.exist(model)) {
        object.remove();
      }
    }
  }

  @Override
  public void queue(MetaModelEvent event) {
    if (!queuing) {
      throw new IllegalStateException("Not queueing");
    }
    queue.queue(event);
    dispatch.queue(event);
  }

  public EventQueue getQueue() {
    return queue;
  }
}
