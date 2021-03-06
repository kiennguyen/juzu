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

package juzu.plugin.asset;

import juzu.asset.AssetLocation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A stylesheet asset.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface Stylesheet {

  /**
   * Returns the stylesheet id, the value is optional and has meaning when this stylesheet must be referenced (for
   * instance as a dependency of another stylesheet).
   *
   * @return the script id
   */
  String id() default "";

  /**
   * Return the stylesheet dependencies, i.e the stylesheet that should be executed before the stylesheet determined by
   * this annotation.
   *
   * @return the dependencies
   */
  String[] depends() default {};

  /**
   * The stylesheet location.
   *
   * @return the location
   */
  AssetLocation location() default AssetLocation.SERVER;

  /**
   * The stylesheet source.
   *
   * @return the source
   */
  String src();

}
