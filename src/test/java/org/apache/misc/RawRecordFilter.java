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

package org.apache.misc;

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.hbql.client.HBqlException;
import org.apache.hadoop.hbase.hbql.client.HConnection;
import org.apache.hadoop.hbase.hbql.client.HConnectionManager;
import org.apache.hadoop.hbase.hbql.client.HMapping;
import org.apache.hadoop.hbase.hbql.filter.RecordFilter;
import org.apache.hadoop.hbase.hbql.mapping.TableMapping;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;

public class RawRecordFilter {

    public static void main(String[] args) throws HBqlException, IOException {

        final byte[] family = Bytes.toBytes("family1");
        final byte[] author = Bytes.toBytes("author");
        final byte[] title = Bytes.toBytes("title");

        HConnection connection = HConnectionManager.newConnection();

        connection.execute("CREATE TEMP MAPPING testobjects alias testobjects2"
                           + "("
                           + "keyval key, "
                           + "family1 ("
                           + "  author string alias author, "
                           + "  title string  alias title"
                           + "))");

        HMapping mapping = connection.getMapping("testobjects");

        final RecordFilter filter = ((TableMapping)mapping)
                .newRecordFilter("title LIKE '.*3.*' OR family1:author LIKE '.*4.*'");

        Scan scan = new Scan();
        scan.addColumn(family, author);
        scan.addColumn(family, title);
        scan.setFilter(filter);

        HTable table = new HTable(HBaseConfiguration.create(), "testobjects");
        ResultScanner scanner = table.getScanner(scan);

        for (Result result : scanner) {
            System.out.println(Bytes.toString(result.getRow()) + " - "
                               + Bytes.toString(result.getValue(family, author)) + " - "
                               + Bytes.toString(result.getValue(family, title)));
        }
    }
}