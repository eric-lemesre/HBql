package org.apache.hadoop.hbase.hbql.query.schema;

import org.apache.hadoop.hbase.hbql.client.HPersistException;
import org.apache.hadoop.hbase.hbql.query.util.Lists;
import org.apache.hadoop.hbase.util.Bytes;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: pambrose
 * Date: Aug 23, 2009
 * Time: 5:01:22 PM
 */
public enum FieldType {

    BooleanType(Boolean.TYPE, Bytes.SIZEOF_BOOLEAN, "BOOLEAN", "BOOL"),
    ByteType(Byte.TYPE, Bytes.SIZEOF_BYTE, "BYTE"),
    CharType(Short.TYPE, Bytes.SIZEOF_CHAR, "CHAR"),
    ShortType(Short.TYPE, Bytes.SIZEOF_SHORT, "SHORT"),
    IntegerType(Integer.TYPE, Bytes.SIZEOF_INT, "INTEGER", "INT"),
    LongType(Long.TYPE, Bytes.SIZEOF_LONG, "LONG"),
    FloatType(Float.TYPE, Bytes.SIZEOF_FLOAT, "FLOAT"),
    DoubleType(Double.TYPE, Bytes.SIZEOF_DOUBLE, "DOUBLE"),
    KeyType(String.class, -1, "KEY"),
    StringType(String.class, -1, "STRING", "STRING", "VARCHAR"),
    DateType(Date.class, -1, "DATE", "DATETIME"),
    ObjectType(Object.class, -1, "OBJECT", "OBJ");

    private final Class clazz;
    private final int size;
    private final List<String> synonymList;


    FieldType(final Class clazz, final int size, final String... synonyms) {
        this.clazz = clazz;
        this.size = size;
        this.synonymList = Lists.newArrayList();
        this.synonymList.addAll(Arrays.asList(synonyms));
    }


    public Class getClazz() {
        return clazz;
    }

    public int getSize() {
        return size;
    }

    public static FieldType getFieldType(final Object obj) {
        final Class fieldClass = obj.getClass();
        return getFieldType(fieldClass);
    }

    public static FieldType getFieldType(final Field field) {
        final Class fieldClass = field.getType();
        return getFieldType(fieldClass);
    }

    public String getFirstSynonym() {
        return this.getSynonymList().get(0);
    }

    private List<String> getSynonymList() {
        return this.synonymList;
    }

    public static FieldType getFieldType(final Class fieldClass) {

        final Class<?> clazz = fieldClass.isArray() ? fieldClass.getComponentType() : fieldClass;

        if (!clazz.isPrimitive()) {
            if (clazz.equals(String.class))
                return StringType;
            else if (clazz.equals(Date.class))
                return DateType;
            else
                return ObjectType;
        }
        else {
            for (final FieldType type : values())
                if (clazz == type.getClazz())
                    return type;
        }

        throw new RuntimeException("Unknown type: " + clazz);
    }

    public static FieldType getFieldType(final String desc) throws HPersistException {

        for (final FieldType type : values()) {
            if (type.matchesSynonym(desc))
                return type;
        }

        throw new HPersistException("Unknown type description: " + desc);
    }

    private boolean matchesSynonym(final String str) {
        for (final String syn : this.getSynonymList())
            if (str.equalsIgnoreCase(syn))
                return true;
        return false;
    }
}