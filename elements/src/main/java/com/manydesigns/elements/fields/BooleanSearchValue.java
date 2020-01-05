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

package com.manydesigns.elements.fields;

/*
* @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
* @author Angelo Lupo          - angelo.lupo@manydesigns.com
* @author Giampiero Granatella - giampiero.granatella@manydesigns.com
* @author Alessio Stalla       - alessio.stalla@manydesigns.com
*/
public enum BooleanSearchValue {
    TRUE("true", "elements.Yes"),
    FALSE("false", "elements.No"),
    ANY("", "elements.Any"),
    NULL("-", "elements.Undefined");

    public static final String copyright =
            "Copyright (C) 2005-2020 ManyDesigns srl";

    private final String stringValue;
    private final String labelI18N;

    BooleanSearchValue(String stringValue, String labelI18N) {
        this.stringValue = stringValue;
        this.labelI18N = labelI18N;
    }

    public String getStringValue() {
        return stringValue;
    }

    public String getLabelI18N() {
        return labelI18N;
    }
}
