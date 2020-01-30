/*
 * Copyright (C) 2005-2020 ManyDesigns srl.  All rights reserved.
 * http://www.manydesigns.com/
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
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

package com.manydesigns.elements.reflection.decorators;

import com.manydesigns.elements.reflection.AbstractAnnotatedAccessor;
import com.manydesigns.elements.reflection.PropertyAccessor;

import java.lang.annotation.Annotation;

/**
 * @author Angelo Lupo          - angelo.lupo@manydesigns.com
 * @author Giampiero Granatella - giampiero.granatella@manydesigns.com
 * @author Emanuele Poggi       - emanuele.poggi@manydesigns.com
 * @author Alessio Stalla       - alessio.stalla@manydesigns.com
 */
public class PropertyAccessorDecorator extends AbstractAnnotatedAccessor implements PropertyAccessor {
    public static final String copyright =
            "Copyright (C) 2005-2020 ManyDesigns srl";

    private final PropertyAccessor delegate;

    public PropertyAccessorDecorator(PropertyAccessor delegate, PropertyAccessor decorator) {
        this.delegate = delegate;
        for(Annotation annotation : delegate.getAnnotations()) {
            this.annotations.put(annotation.annotationType(), annotation);
        }
        for(Annotation annotation : decorator.getAnnotations()) {
            this.annotations.put(annotation.annotationType(), annotation);
        }
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public Class getType() {
        return delegate.getType();
    }

    @Override
    public int getModifiers() {
        return delegate.getModifiers();
    }

    @Override
    public Object get(Object obj) {
        return delegate.get(obj);
    }

    @Override
    public void set(Object obj, Object value) {
        delegate.set(obj, value);
    }
}
