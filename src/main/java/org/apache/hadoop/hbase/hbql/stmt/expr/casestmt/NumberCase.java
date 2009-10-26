package org.apache.hadoop.hbase.hbql.stmt.expr.casestmt;

import org.apache.hadoop.hbase.hbql.client.HBqlException;
import org.apache.hadoop.hbase.hbql.client.ResultMissingColumnException;
import org.apache.hadoop.hbase.hbql.stmt.expr.node.NumberValue;

import java.util.List;

public class NumberCase extends GenericCase implements NumberValue {

    public NumberCase(final List<GenericCaseWhen> whenExprList, final GenericCaseElse elseExpr) {
        super(Type.NUMBERCASE, whenExprList, elseExpr);
    }

    public Number getValue(final Object object) throws HBqlException, ResultMissingColumnException {
        return (Number)super.getValue(object);
    }
}