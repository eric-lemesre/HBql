/*
 * Copyright (c) 2009.  The Apache Software Foundation
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

package org.apache.hadoop.hbase.hbql.client;

import org.apache.expreval.util.Maps;
import org.apache.expreval.util.Sets;
import org.apache.hadoop.hbase.hbql.impl.HConnectionImpl;
import org.apache.hadoop.hbase.hbql.schema.ColumnDescription;
import org.apache.hadoop.hbase.hbql.schema.HBaseSchema;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class SchemaManager {

    private final HConnectionImpl connection;
    private final Map<String, HBaseSchema> schemaMap = Maps.newConcurrentHashMap();

    public SchemaManager(final HConnectionImpl connection) {
        this.connection = connection;
    }

    public void validatePersistentMetadata() throws HBqlException {

        final String sql = "CREATE TEMP SCHEMA system_schemas (schema_name key, f1:schema_obj object alias schema_obj)";
        this.getConnection().execute(sql);

        if (!this.getConnection().tableExists("system_schemas"))
            this.getConnection().execute("CREATE TABLE USING system_schemas");
    }

    private HConnectionImpl getConnection() {
        return connection;
    }

    private Map<String, HBaseSchema> getSchemaMap() {
        return this.schemaMap;
    }

    public Set<HSchema> getSchemas() throws HBqlException {

        final Set<HSchema> names = Sets.newHashSet();
        names.addAll(getSchemaMap().values());

        final String sql = "SELECT schema_obj FROM system_schemas)";
        final List<HRecord> recs = this.getConnection().executeQueryAndFetch(sql);

        for (final HRecord rec : recs)
            names.add((HBaseSchema)rec.getCurrentValue("schema_obj"));

        return names;
    }

    public boolean schemaExists(final String schemaName) throws HBqlException {

        if (this.getSchemaMap().get(schemaName) != null)
            return true;
        else {
            final String sql = "SELECT schema_name FROM system_schemas WITH CLIENT FILTER WHERE schema_name =  ?)";
            final HPreparedStatement stmt = this.getConnection().prepareStatement(sql);
            stmt.setParameter(1, schemaName);
            final List<HRecord> recs = stmt.executeQueryAndFetch();
            return recs.size() > 0;
        }
    }

    public boolean dropSchema(final String schemaName) throws HBqlException {

        if (this.getSchemaMap().containsKey(schemaName)) {
            this.getSchemaMap().remove(schemaName);
            return true;
        }
        else {
            final String sql = "DELETE FROM system_schemas WITH CLIENT FILTER WHERE schema_name =  ?)";
            final HPreparedStatement stmt = this.getConnection().prepareStatement(sql);
            stmt.setParameter(1, schemaName);
            final int cnt = stmt.executeUpdate().getCount();
            return cnt > 0;
        }
    }

    public synchronized HBaseSchema createSchema(final boolean tempSchema,
                                                 final String schemaName,
                                                 final String tableName,
                                                 final List<ColumnDescription> colList) throws HBqlException {

        if (!schemaName.equals("system_schemas") && this.schemaExists(schemaName))
            throw new HBqlException("Schema already defined: " + schemaName);

        final HBaseSchema schema = new HBaseSchema(tempSchema, schemaName, tableName, colList, true);

        if (schema.isTempSchema())
            this.getSchemaMap().put(schemaName, schema);
        else
            this.insertSchema(schema);

        return schema;
    }

    private void insertSchema(final HBaseSchema schema) throws HBqlException {

        final String sql = "INSERT INTO system_schemas (schema_name, schema_obj) VALUES (?, ?)";
        final HPreparedStatement stmt = this.getConnection().prepareStatement(sql);

        stmt.setParameter(1, schema.getSchemaName());
        stmt.setParameter(2, schema);
        stmt.execute();
    }

    public HBaseSchema getSchema(final String schemaName) throws HBqlException {

        if (this.getSchemaMap().containsKey(schemaName)) {
            return this.getSchemaMap().get(schemaName);
        }
        else {
            final String sql = "SELECT schema_obj FROM system_schemas WITH CLIENT FILTER WHERE schema_name =  ?)";
            final HPreparedStatement stmt = this.getConnection().prepareStatement(sql);
            stmt.setParameter(1, schemaName);
            List<HRecord> recs = stmt.executeQueryAndFetch();

            if (recs.size() == 0)
                throw new HBqlException("Schema not found: " + schemaName);

            return (HBaseSchema)recs.get(0).getCurrentValue("schema_obj");
        }
    }
}
