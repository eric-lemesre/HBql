/*
 * Copyright (c) 2011.  The Apache Software Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.expreval.expr.nullcomp;

import org.apache.expreval.client.ResultMissingColumnException;
import org.apache.expreval.expr.TypeSupport;
import org.apache.expreval.expr.node.BooleanValue;
import org.apache.expreval.expr.node.ByteValue;
import org.apache.expreval.expr.node.DateValue;
import org.apache.expreval.expr.node.GenericValue;
import org.apache.expreval.expr.node.NumberValue;
import org.apache.expreval.expr.node.ObjectValue;
import org.apache.expreval.expr.node.StringValue;
import org.apache.hadoop.hbase.hbql.client.HBqlException;
import org.apache.hadoop.hbase.hbql.impl.HConnectionImpl;
import org.apache.hadoop.hbase.hbql.impl.InvalidTypeException;

public class DelegateNullCompare extends GenericNullCompare {

    public DelegateNullCompare(final boolean not, final GenericValue expr) {
        super(null, not, expr);
    }

    public Class<? extends GenericValue> validateTypes(final GenericValue parentExpr,
                                                       final boolean allowCollections) throws HBqlException {

        final GenericValue genericValue = this.getExprArg(0);
        final Class<? extends GenericValue> validatedValue = genericValue.validateTypes(this, false);
        final Class<? extends GenericValue> type = this.getGenericValueClass(validatedValue);

        final GenericNullCompare typedExpr;
        if (TypeSupport.isParentClass(StringValue.class, type))
            typedExpr = new StringNullCompare(this.isNot(), this.getExprArg(0));
        else if (TypeSupport.isParentClass(NumberValue.class, type))
            typedExpr = new NumberNullCompare(this.isNot(), this.getExprArg(0));
        else if (TypeSupport.isParentClass(DateValue.class, type))
            typedExpr = new DateNullCompare(this.isNot(), this.getExprArg(0));
        else if (TypeSupport.isParentClass(BooleanValue.class, type))
            typedExpr = new BooleanNullCompare(this.isNot(), this.getExprArg(0));
        else if (TypeSupport.isParentClass(ByteValue.class, type))
            typedExpr = new ByteNullCompare(this.isNot(), this.getExprArg(0));
        else if (TypeSupport.isParentClass(ObjectValue.class, type))
            typedExpr = new ObjectNullCompare(this.isNot(), this.getExprArg(0));
        else {
            typedExpr = null;
            throw new InvalidTypeException(this.getInvalidTypeMsg(type));
        }

        this.setTypedExpr(typedExpr);

        return this.getTypedExpr().validateTypes(parentExpr, false);
    }

    public GenericValue getOptimizedValue() throws HBqlException {
        this.optimizeAllArgs();
        return !this.isAConstant() ? this : this.getTypedExpr().getOptimizedValue();
    }

    public Boolean getValue(final HConnectionImpl conn, final Object object) throws HBqlException,
                                                                                    ResultMissingColumnException {
        return this.getTypedExpr().getValue(conn, object);
    }
}