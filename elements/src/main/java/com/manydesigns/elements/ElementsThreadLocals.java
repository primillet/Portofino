/*
 * Copyright (C) 2005-2013 ManyDesigns srl.  All rights reserved.
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

package com.manydesigns.elements;

import com.manydesigns.elements.blobs.BlobManager;
import com.manydesigns.elements.i18n.SimpleTextProvider;
import com.manydesigns.elements.i18n.TextProvider;
import com.manydesigns.elements.ognl.CustomTypeConverter;
import com.manydesigns.elements.servlet.WebFramework;
import ognl.Ognl;
import ognl.OgnlContext;
import ognl.TypeConverter;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*
* @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
* @author Angelo Lupo          - angelo.lupo@manydesigns.com
* @author Giampiero Granatella - giampiero.granatella@manydesigns.com
* @author Alessio Stalla       - alessio.stalla@manydesigns.com
*/
public final class ElementsThreadLocals {
    public static final String copyright =
            "Copyright (c) 2005-2013, ManyDesigns srl";

    //**************************************************************************
    // Static
    //**************************************************************************
    private static ThreadLocal<ElementsContext> threadLocalElementsContext =
            new ThreadLocal<ElementsContext>() {
                @Override
                protected ElementsContext initialValue() {
                    return new ElementsContext();
                }
            };

    //**************************************************************************
    // Static cleanup
    //**************************************************************************

    public static void destroy() {
        threadLocalElementsContext = null;
    }

    //**************************************************************************
    // Constructors
    //**************************************************************************

    private ElementsThreadLocals() {}

    //**************************************************************************
    // Static getters/setters
    //**************************************************************************

    public static ElementsContext getElementsContext() {
        return threadLocalElementsContext.get();
    }

    public static TextProvider getTextProvider() {
        return getElementsContext().getTextProvider();
    }

    public static void setTextProvider(TextProvider textProvider) {
        getElementsContext().setTextProvider(textProvider);
    }

    public static HttpServletRequest getHttpServletRequest() {
        return getElementsContext().getHttpServletRequest();
    }

    public static void setHttpServletRequest(HttpServletRequest httpServletRequest) {
        getElementsContext().setHttpServletRequest(httpServletRequest);
    }

    public static HttpServletResponse getHttpServletResponse() {
        return getElementsContext().getHttpServletResponse();
    }

    public static void setHttpServletResponse(HttpServletResponse httpServletResponse) {
        getElementsContext().setHttpServletResponse(httpServletResponse);
    }

    public static ServletContext getServletContext() {
        return getElementsContext().getServletContext();
    }

    public static void setServletContext(ServletContext servletContext) {
        getElementsContext().setServletContext(servletContext);
    }

    public static OgnlContext getOgnlContext() {
        return getElementsContext().getOgnlContext();
    }

    public static void setOgnlContext(OgnlContext context) {
        getElementsContext().setOgnlContext(context);
    }

    public static BlobManager getBlobManager() {
        return getElementsContext().getBlobManager();
    }

    public static void setBlobManager(BlobManager blobManager) {
        getElementsContext().setBlobManager(blobManager);
    }

    public static WebFramework getWebFramework() {
        return getElementsContext().getWebFramework();
    }

    public static void setWebFramework(WebFramework webFramework) {
        getElementsContext().setWebFramework(webFramework);
    }


    //**************************************************************************
    // Utility methods
    //**************************************************************************

    public static void setupDefaultElementsContext() {
        OgnlContext ognlContext = (OgnlContext) Ognl.createDefaultContext(null);
        TypeConverter conv = ognlContext.getTypeConverter();
        ognlContext.setTypeConverter(new CustomTypeConverter(conv));
        TextProvider textProvider = SimpleTextProvider.create();

        ElementsContext elementsContext = getElementsContext();

        elementsContext.setOgnlContext(ognlContext);
        elementsContext.setTextProvider(textProvider);
        elementsContext.setHttpServletRequest(null);
        elementsContext.setHttpServletResponse(null);
        elementsContext.setServletContext(null);
        elementsContext.setWebFramework(WebFramework.getDefaultWebFramework());

        BlobManager blobManager = BlobManager.createDefaultBlobManager();
        elementsContext.setBlobManager(blobManager);
    }

    public static void removeElementsContext() {
        threadLocalElementsContext.remove();
    }

    //**************************************************************************
    // i18n
    //**************************************************************************

    public static String getText(String key, Object... args) {
        return getTextProvider().getText(key, args);
    }
}
