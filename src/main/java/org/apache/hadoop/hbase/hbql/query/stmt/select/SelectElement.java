package org.apache.hadoop.hbase.hbql.query.stmt.select;

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.hbql.client.HBqlException;
import org.apache.hadoop.hbase.hbql.query.schema.ColumnAttrib;
import org.apache.hadoop.hbase.hbql.query.schema.HBaseSchema;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: pambrose
 * Date: Sep 28, 2009
 * Time: 5:31:29 PM
 */
public interface SelectElement {

    void validate(HBaseSchema schema, List<ColumnAttrib> selectAttribList) throws HBqlException;

    void assignCurrentValue(Object newobj, Result result) throws HBqlException;

    void assignVersionValue(Object newobj, Result result) throws HBqlException;
}