package org.apache.hadoop.hbase.hbql.query.expr;

import org.apache.hadoop.hbase.hbql.query.schema.FieldType;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: pambrose
 * Date: Sep 1, 2009
 * Time: 8:11:19 PM
 */
public class ExprVariable implements Serializable {

    private final String attribName;
    private final FieldType fieldType;

    public ExprVariable(final String attribName, FieldType fieldType) {
        this.attribName = attribName;
        this.fieldType = fieldType;
    }

    public String getName() {
        return this.attribName;
    }

    public FieldType getFieldType() {
        return this.fieldType;
    }

    @Override
    public boolean equals(final Object o) {
        return (o instanceof ExprVariable) && this.getName().equals(((ExprVariable)o).getName());
    }
}
