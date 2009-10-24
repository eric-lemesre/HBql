package org.apache.hadoop.hbase.hbql.query.stmt.select;

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.hbql.client.HBqlException;
import org.apache.hadoop.hbase.hbql.client.HConnection;
import org.apache.hadoop.hbase.hbql.query.schema.ColumnAttrib;
import org.apache.hadoop.hbase.hbql.query.stmt.args.SelectStmt;

import java.util.List;

public interface SelectElement {

    void validate(final SelectStmt selectStmt,
                  HConnection connection,
                  List<ColumnAttrib> selectAttribList) throws HBqlException;

    void assignValues(Object newobj,
                      List<ColumnAttrib> selectAttribList,
                      int maxVerions,
                      Result result) throws HBqlException;

    int setParameter(String name, Object val) throws HBqlException;

    String getAsName();

    boolean hasAsName();

    String asString();
}
