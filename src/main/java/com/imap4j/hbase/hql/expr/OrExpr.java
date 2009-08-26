package com.imap4j.hbase.hql.expr;

import com.imap4j.hbase.hql.ClassSchema;

/**
 * Created by IntelliJ IDEA.
 * User: pambrose
 * Date: Aug 25, 2009
 * Time: 6:58:31 PM
 */
public class OrExpr implements Evaluatable {

    public AndExpr expr1;
    public OrExpr expr2;

    @Override
    public boolean evaluate(final ClassSchema classSchema, final Object recordObj) {
        if (expr2 == null)
            return expr1.evaluate(nil, nil);
        else
            return expr1.evaluate(nil, nil) || expr2.evaluate(nil, nil);
    }
}
