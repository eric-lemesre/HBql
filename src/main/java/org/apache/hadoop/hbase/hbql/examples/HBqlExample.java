package org.apache.hadoop.hbase.hbql.examples;

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.HBqlFilter;
import org.apache.hadoop.hbase.hbql.client.HPersistException;
import org.apache.hadoop.hbase.hbql.client.SchemaManager;
import org.apache.hadoop.hbase.hbql.query.schema.DefinedSchema;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: pambrose
 * Date: Aug 19, 2009
 * Time: 4:39:06 PM
 */
public class HBqlExample {

    public static void main(String[] args) throws IOException, HPersistException {

        final byte[] family = Bytes.toBytes("family1");
        final byte[] author = Bytes.toBytes("author");
        final byte[] title = Bytes.toBytes("title");

        SchemaManager.parse("define table testobjects alias testobjects2"
                            + "("
                            + "keyval key, "
                            + "family1:author string alias author, "
                            + "family1:title string  alias title"
                            + ")");

        DefinedSchema schema = SchemaManager.getDefinedSchema("testobjects");
        final HBqlFilter filter = schema.newHBqlFilter("title LIKE '.*3.*' OR family1:author LIKE '.*4.*'");

        Scan scan = new Scan();
        scan.addColumn(family, author);
        scan.addColumn(family, title);
        scan.setFilter(filter);

        HTable table = new HTable(new HBaseConfiguration(), "testobjects");
        ResultScanner scanner = table.getScanner(scan);

        for (Result result : scanner) {
            System.out.println(Bytes.toString(result.getRow()) + " - "
                               + Bytes.toString(result.getValue(family, author)) + " - "
                               + Bytes.toString(result.getValue(family, title)));
        }

    }

}