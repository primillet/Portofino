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

package com.manydesigns.portofino.model.database;

import org.jetbrains.annotations.Nullable;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/*
* @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
* @author Angelo Lupo          - angelo.lupo@manydesigns.com
* @author Giampiero Granatella - giampiero.granatella@manydesigns.com
* @author Alessio Stalla       - alessio.stalla@manydesigns.com
*/
public class Type {
    public static final String copyright =
            "Copyright (C) 2005-2020 ManyDesigns srl";

    //**************************************************************************
    // Logger
    //**************************************************************************

    public final static Logger logger = LoggerFactory.getLogger(Type.class);

    //**************************************************************************
    // Fields
    //**************************************************************************

    protected final String typeName;
    protected final int jdbcType;
    protected final boolean autoincrement;
    protected final Integer maximumPrecision;
    protected final String literalPrefix;
    protected final String literalSuffix;
    protected final boolean nullable;
    protected final boolean caseSensitive;
    protected final boolean searchable;
    protected final int minimumScale;
    protected final int maximumScale;
    protected final Boolean precisionRequired;
    protected final Boolean scaleRequired;


    //**************************************************************************
    // Constructors
    //**************************************************************************

    public Type(String typeName, int jdbcType, Integer maximumPrecision,
                String literalPrefix, String literalSuffix,
                boolean nullable, boolean caseSensitive, boolean searchable,
                boolean autoincrement, int minimumScale, int maximumScale) {
        this.typeName = typeName;
        this.jdbcType = jdbcType;
        this.maximumPrecision = maximumPrecision;
        this.literalPrefix = literalPrefix;
        this.literalSuffix = literalSuffix;
        this.nullable = nullable;
        this.caseSensitive = caseSensitive;
        this.searchable = searchable;
        this.autoincrement = autoincrement;
        this.minimumScale = minimumScale;
        this.maximumScale = maximumScale;

        this.precisionRequired = null;
        this.scaleRequired = null;

    }

    public Type(String typeName, int jdbcType, Integer maximumPrecision,
                String literalPrefix, String literalSuffix,
                boolean nullable, boolean caseSensitive, boolean searchable,
                boolean autoincrement, int minimumScale, int maximumScale,
                boolean precisionRequired, boolean scaleRequired) {
        this.typeName = typeName;
        this.jdbcType = jdbcType;
        this.maximumPrecision = maximumPrecision;
        this.literalPrefix = literalPrefix;
        this.literalSuffix = literalSuffix;
        this.nullable = nullable;
        this.caseSensitive = caseSensitive;
        this.searchable = searchable;
        this.autoincrement = autoincrement;
        this.minimumScale = minimumScale;
        this.maximumScale = maximumScale;

        this.precisionRequired = precisionRequired;
        this.scaleRequired = scaleRequired;

    }

    //**************************************************************************
    // Getters/setter
    //**************************************************************************

    public String getTypeName() {
        return typeName;
    }

    public int getJdbcType() {
        return jdbcType;
    }

    public Class getDefaultJavaType() {
        return getDefaultJavaType(jdbcType, typeName, maximumPrecision, maximumScale);
    }

    public static @Nullable Class getDefaultJavaType(int jdbcType, String databaseType, Integer precision, Integer scale) {
        switch (jdbcType) {
            case Types.BIGINT:
                return Long.class;
            case Types.BIT:
            case Types.BOOLEAN:
                return Boolean.class;
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.NCHAR:
            case Types.NVARCHAR:
            case Types.CLOB:
            case Types.LONGVARCHAR:
                return String.class;
            case Types.DATE:
                return java.sql.Date.class;
            case Types.TIME:
                return Time.class;
            case Types.TIMESTAMP:
                return Timestamp.class;
            case Types.DECIMAL:
            case Types.NUMERIC:
                if(scale != null && scale > 0) {
                    return BigDecimal.class;
                } else {
                    return getDefaultIntegerType(precision);
                }
            case Types.DOUBLE:
            case Types.REAL:
                return Double.class;
            case Types.FLOAT:
                return Float.class;
            case Types.INTEGER:
                return getDefaultIntegerType(precision);
            case Types.SMALLINT:
                return Short.class;
            case Types.TINYINT:
                return Byte.class;
            case Types.BINARY:
            case Types.BLOB:
            case Types.LONGVARBINARY:
            case Types.VARBINARY:
                return byte[].class;
            case Types.ARRAY:
                return java.sql.Array.class;
            case Types.DATALINK:
                return java.net.URL.class;
            case Types.DISTINCT:
            case Types.JAVA_OBJECT:
                return Object.class;
            case Types.NULL:
            case Types.REF:
                return java.sql.Ref.class;
            case Types.OTHER:
                if("JSONB".equalsIgnoreCase(databaseType)) { //TODO make configurable by modules
                    return String.class;
                } else {
                    return java.sql.Ref.class;
                }
            case Types.STRUCT:
                return java.sql.Struct.class;
            default:
                logger.warn("Unsupported jdbc type: {}", jdbcType);
                return null;
        }
    }

    public static Class<? extends Number> getDefaultIntegerType(Integer precision) {
        if(precision == null) {
            return BigInteger.class;
        }
        if(precision < Math.log10(Integer.MAX_VALUE)) {
            return Integer.class;
        } else if(precision < Math.log10(Long.MAX_VALUE)) {
            return Long.class;
        } else {
            if(precision == 131089) {
                return BigDecimal.class; //Postgres bug - #925
            } else {
                return BigInteger.class;
            }
        }
    }

    public Class[] getAvailableJavaTypes(Integer length) {
        if(isNumeric()) {
            return new Class[] {
                    Integer.class, Long.class, Byte.class, Short.class,
                    Float.class, Double.class, BigInteger.class, BigDecimal.class,
                    Boolean.class };
        } else {
            if("JSONB".equalsIgnoreCase(typeName)) { //TODO make configurable by modules
                return new Class[] { String.class, Map.class, List.class };
            }
            Class defaultJavaType = getDefaultJavaType();
            if(defaultJavaType == String.class) {
                if(length != null && length < 256) {
                    return new Class[] { String.class, Boolean.class };
                } else {
                    return new Class[] { String.class };
                }
            } else if(defaultJavaType == Timestamp.class) {
                return new Class[] { Timestamp.class, DateTime.class, java.sql.Date.class, LocalDateTime.class, ZonedDateTime.class, Instant.class };
            } else if(defaultJavaType == java.sql.Date.class) {
                return new Class[] { java.sql.Date.class, DateTime.class, LocalDate.class, Timestamp.class }; //TODO Joda LocalDate as well?
            } else {
                if(defaultJavaType != null) {
                    return new Class[] { defaultJavaType };
                } else {
                    return new Class[] { Object.class };
                }
            }
        }
    }

    public boolean isAutoincrement() {
        return autoincrement;
    }

    public Integer getMaximumPrecision() {
        return maximumPrecision;
    }

    public String getLiteralPrefix() {
        return literalPrefix;
    }

    public String getLiteralSuffix() {
        return literalSuffix;
    }

    public boolean isNullable() {
        return nullable;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public boolean isSearchable() {
        return searchable;
    }

    public Integer getMinimumScale() {
        return minimumScale;
    }

    public Integer getMaximumScale() {
        return maximumScale;
    }

    @Override
    public String toString() {
        return "Type{" +
                "typeName='" + typeName + '\'' +
                ", jdbcType=" + jdbcType +
                ", autoincrement=" + autoincrement +
                ", maximumPrecision=" + maximumPrecision +
                ", literalPrefix='" + literalPrefix + '\'' +
                ", literalSuffix='" + literalSuffix + '\'' +
                ", nullable=" + nullable +
                ", caseSensitive=" + caseSensitive +
                ", searchable=" + searchable +
                ", minimumScale=" + minimumScale +
                ", maximumScale=" + maximumScale +
                '}';
    }

    public boolean isPrecisionRequired() {
        if (precisionRequired!= null){
            return precisionRequired;
        }
        switch (jdbcType) {
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.DECIMAL:
            case Types.NUMERIC:
                return true;
            default:
                return false;
        }

    }

    public boolean isScaleRequired() {
        if (scaleRequired!= null){
            return scaleRequired;
        }
        switch (jdbcType) {
            case Types.DECIMAL:
            case Types.NUMERIC:
                return true;
            default:
                return false;
        }
    }

    public boolean isNumeric() {
        switch (jdbcType) {
            case Types.NUMERIC:
            case Types.DECIMAL:
            case Types.INTEGER:
            case Types.FLOAT:
            case Types.DOUBLE:
            case Types.BIGINT:
            case Types.SMALLINT:
            case Types.TINYINT:
            case Types.REAL:
                return true;
            default: return false;
        }
    }

}
