/*
 * Copyright (C) 2005-2011 ManyDesigns srl.  All rights reserved.
 * http://www.manydesigns.com/
 *
 * Unless you have purchased a commercial license agreement from ManyDesigns srl,
 * the following license terms apply:
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
 *
 * There are special exceptions to the terms and conditions of the GPL
 * as it is applied to this software. View the full text of the
 * exception in file OPEN-SOURCE-LICENSE.txt in the directory of this
 * software distribution.
 *
 * This program is distributed WITHOUT ANY WARRANTY; and without the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see http://www.gnu.org/licenses/gpl.txt
 * or write to:
 * Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307  USA
 *
 */

package com.manydesigns.portofino.util;

import com.manydesigns.elements.annotations.ShortName;
import com.manydesigns.elements.reflection.ClassAccessor;
import com.manydesigns.elements.reflection.PropertyAccessor;
import com.manydesigns.elements.text.OgnlTextFormat;

/*
* @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
* @author Angelo Lupo          - angelo.lupo@manydesigns.com
* @author Giampiero Granatella - giampiero.granatella@manydesigns.com
* @author Alessio Stalla       - alessio.stalla@manydesigns.com
*/
public class ShortNameUtils {
    public static final String copyright =
            "Copyright (c) 2005-2011, ManyDesigns srl";

    public static final String PK_ELEMENT_SEPARATOR = " ";

    public static String getName(ClassAccessor classAccessor, Object object) {
        ShortName annotation = classAccessor.getAnnotation(ShortName.class);
        String formatString;
        if (annotation == null) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            // sintetizziamo una stringa a partire dalla chiave primaria
            for (PropertyAccessor propertyAccessor : classAccessor.getKeyProperties()) {
                if (first) {
                    first = false;
                } else {
                    sb.append(PK_ELEMENT_SEPARATOR);
                }
                sb.append(String.format("%%{%s}", propertyAccessor.getName()));
            }
            formatString = sb.toString();
        } else {
            formatString = annotation.value();
        }
        OgnlTextFormat ognlTextFormat = OgnlTextFormat.create(formatString);
        return ognlTextFormat.format(object);
    }
}