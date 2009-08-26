package com.imap4j.hbase.hql.expr;

import com.imap4j.hbase.hql.ClassSchema;

/**
 * Created by IntelliJ IDEA.
 * User: pambrose
 * Date: Aug 25, 2009
 * Time: 10:30:32 PM
 */
public class StringCompareExpr extends CompareExpr {

    public StringExpr expr;


    public StringCompareExpr(final StringExpr expr) {
        this.expr = expr;
    }

    public StringCompareExpr(final String attribName, final Operator op, final StringExpr expr) {
        super(attribName, op);
        this.expr = expr;
    }

    @Override
    public boolean evaluate(final ClassSchema classSchema, final Object recordObj) {
        switch (this.op) {
            case EQ: {
                return
            }
            case GT: {

            }
            case GTEQ: {

            }
            case LT: {

            }
            case LTEQ: {

            }
            case LTGT: {

            }
        }
    }

}
