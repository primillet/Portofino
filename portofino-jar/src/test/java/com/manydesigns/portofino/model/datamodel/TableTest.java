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
package com.manydesigns.portofino.model.datamodel;

import com.manydesigns.portofino.model.Model;
import junit.framework.TestCase;

/*
* @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
* @author Angelo Lupo          - angelo.lupo@manydesigns.com
* @author Giampiero Granatella - giampiero.granatella@manydesigns.com
* @author Alessio Stalla       - alessio.stalla@manydesigns.com
*/
public class TableTest extends TestCase {

    public void testActualEntityNames(){
        Model model = new Model();
        Database db = new Database();
        db.setDatabaseName("portofino");
        Schema schema = new Schema();
        schema.setDatabase(db);
        schema.setSchemaName("meta");
        Table table = new Table();
        table.setSchema(schema);
        table.setTableName(" ab!!!..acus$%/()");
        model.getDatabases().add(db);
        table.init(model);

        assertNotNull(table.getActualEntityName());
        assertEquals("_ab_____acus$____", table.getActualEntityName());
        System.out.println(table.getActualEntityName());

        table = new Table();
        table.setSchema(schema);
        table.setTableName("0DPrpt");
        table.init(model);
        assertEquals("_0dprpt", table.getActualEntityName());
        System.out.println(table.getActualEntityName());


        db = new Database();
        db.setDatabaseName("1portofino");
        schema = new Schema();
        schema.setDatabase(db);
        schema.setSchemaName("meta");
        table = new Table();
        table.setSchema(schema);
        table.setTableName("0DPrpt");
        table.init(model);
        assertEquals("_0dprpt", table.getActualEntityName());
        System.out.println(table.getActualEntityName());

        db = new Database();
        db.setDatabaseName("$1portofino");
        schema = new Schema();
        schema.setDatabase(db);
        schema.setSchemaName("meta");
        table = new Table();
        table.setSchema(schema);
        table.setTableName("0DPrpt");
        table.init(model);
        assertEquals("_0dprpt", table.getActualEntityName());
        System.out.println(table.getActualEntityName());

        db = new Database();
        db.setDatabaseName(".portofino");
        table.setTableName("0DPrpt");
        table.init(model);
        assertEquals("_0dprpt", table.getActualEntityName());
        System.out.println(table.getActualEntityName());

        table.setTableName("XYZéèçò°àùì");
        table.init(model);
        assertEquals("xyzéèçò_àùì", table.getActualEntityName());
        System.out.println(table.getActualEntityName());

        table.setTableName("ĖĔĕĘĘŜŞŝōŎľĿʛʋʊɪɩɨɷ");
        table.init(model);
        assertEquals("ĖĔĕĘĘŜŞŝōŎľĿʛʋʊɪɩɨɷ", table.getActualEntityName());
        System.out.println(table.getActualEntityName());
    }

    public void testActualColumnNames() {
        Model model = new Model();
        Database db = new Database();
        db.setDatabaseName("portofino");
        Schema schema = new Schema();
        schema.setDatabase(db);
        db.getSchemas().add(schema);
        schema.setSchemaName("meta");
        Table table = new Table();
        table.setSchema(schema);
        table.setTableName("ignore");
        schema.getTables().add(table);
        Column column = new Column();
        column.setTable(table);
        column.setColumnName(" ab!!!..acus$%/()");
        table.getColumns().add(column);
        model.getDatabases().add(db);
        model.init();

        assertNotNull(column.getActualPropertyName());
        assertEquals("_ab_____acus$____", column.getActualPropertyName());

        table = new Table();
        table.setSchema(schema);
        table.setTableName("ignore");
        column = new Column();
        column.setTable(table);
        column.setColumnName("0DPrpt");
        table.getColumns().add(column);
        schema.getTables().clear();
        schema.getTables().add(table);
        model.init();
        assertEquals("_0dprpt", column.getActualPropertyName());

        
        column.setColumnName("XYZéèçò°àùì");
        model.init();
        assertEquals("xyzéèçò_àùì", column.getActualPropertyName());

        column.setColumnName("ĖĔĕĘĘŜŞŝōŎľĿʛʋʊɪɩɨɷ");
        model.init();
        assertEquals("ĖĔĕĘĘŜŞŝōŎľĿʛʋʊɪɩɨɷ", column.getActualPropertyName());
    }
}