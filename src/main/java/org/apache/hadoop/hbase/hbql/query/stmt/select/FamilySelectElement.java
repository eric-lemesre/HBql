package org.apache.hadoop.hbase.hbql.query.stmt.select;

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.hbql.client.HBqlException;
import org.apache.hadoop.hbase.hbql.client.HConnection;
import org.apache.hadoop.hbase.hbql.client.HRecord;
import org.apache.hadoop.hbase.hbql.query.schema.ColumnAttrib;
import org.apache.hadoop.hbase.hbql.query.schema.FamilyAttrib;
import org.apache.hadoop.hbase.hbql.query.schema.HBaseSchema;
import org.apache.hadoop.hbase.hbql.query.util.HUtil;
import org.apache.hadoop.hbase.hbql.query.util.Lists;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class FamilySelectElement implements SelectElement {

    private final boolean useAllFamilies;
    private final List<String> familyNameList = Lists.newArrayList();
    private final List<byte[]> familyNameBytesList = Lists.newArrayList();
    private final String familyName;

    private HBaseSchema schema;

    public FamilySelectElement(final String familyName) {

        this.familyName = familyName;

        if (familyName != null && familyName.equals("*")) {
            this.useAllFamilies = true;
        }
        else {
            this.useAllFamilies = false;
            this.addAFamily(familyName.replaceAll(" ", "").replace(":*", ""));
        }
    }

    public void addAFamily(final String familyName) {
        this.familyNameList.add(familyName);
    }

    public static List<SelectElement> newAllFamilies() {
        final List<SelectElement> retval = Lists.newArrayList();
        retval.add(new FamilySelectElement("*"));
        return retval;
    }

    public static FamilySelectElement newFamilyElement(final String family) {
        return new FamilySelectElement(family);
    }

    public List<String> getFamilyNameList() {
        return this.familyNameList;
    }

    public List<byte[]> getFamilyNameBytesList() {
        return this.familyNameBytesList;
    }

    protected HBaseSchema getSchema() {
        return this.schema;
    }

    public String getAsName() {
        return null;
    }

    public String asString() {
        return this.familyName;
    }

    public int setParameter(final String name, final Object val) {
        // Do nothing
        return 0;
    }

    public void validate(final HConnection connection,
                         final HBaseSchema schema,
                         final List<ColumnAttrib> selectAttribList) throws HBqlException {

        this.schema = schema;

        if (this.useAllFamilies) {
            // connction will be null from tests
            final Collection<String> familyList = this.getSchema().getAllSchemaFamilyNames(connection);
            for (final String familyName : familyList) {
                this.addAFamily(familyName);
                selectAttribList.add(new FamilyAttrib(familyName));
            }
        }
        else {
            // Only has one family
            final String familyName = this.getFamilyNameList().get(0);
            if (!schema.containsFamilyNameInFamilyNameMap(familyName))
                throw new HBqlException("Invalid family name: " + familyName);

            selectAttribList.add(new FamilyAttrib(familyName));
        }

        for (final String familyName : this.getFamilyNameList())
            this.getFamilyNameBytesList().add(HUtil.ser.getStringAsBytes(familyName));
    }

    public void assignValues(final Object newobj,
                             final List<ColumnAttrib> selectAttribList,
                             final int maxVersions,
                             final Result result) throws HBqlException {

        // Evaluate each of the families (select * will yield all families)
        for (int i = 0; i < this.getFamilyNameBytesList().size(); i++) {

            final String familyName = this.getFamilyNameList().get(i);
            final byte[] familyNameBytes = this.getFamilyNameBytesList().get(i);

            final NavigableMap<byte[], byte[]> columnMap = result.getFamilyMap(familyNameBytes);

            for (final byte[] columnBytes : columnMap.keySet()) {

                final byte[] currentValueBytes = columnMap.get(columnBytes);
                final String columnName = HUtil.ser.getStringFromBytes(columnBytes);

                if (columnName.endsWith("]")) {

                    final int lbrace = columnName.indexOf("[");
                    final String mapColumn = columnName.substring(0, lbrace);
                    final String mapKey = columnName.substring(lbrace + 1, columnName.length() - 1);
                    final ColumnAttrib attrib = this.getSchema().getAttribFromFamilyQualifiedName(familyName,
                                                                                                  mapColumn);

                    if (attrib != null) {
                        final Object val = attrib.getValueFromBytes(newobj, currentValueBytes);
                        attrib.setKeysAsColumnsValue(newobj, 0, mapKey, val);
                    }
                    else {
                        // Set unknown attrib value to byte[] value
                        // Find value in results and assign the byte[] value to HRecord, but bail on Annotated object
                        if (!(newobj instanceof HRecord))
                            return;

                        final HRecord hrecord = (HRecord)newobj;
                        hrecord.setKeysAsColumnsValue(familyName + ":" + columnName, 0, mapKey, currentValueBytes, false);
                    }
                }
                else {
                    final ColumnAttrib attrib = this.getSchema().getAttribFromFamilyQualifiedName(familyName,
                                                                                                  columnName);
                    // If attrib is found, then assign the deserialized value to the known atribb
                    if (attrib != null)
                        attrib.setCurrentValue(newobj, 0, currentValueBytes);
                    else {
                        // Set unknown attrib value to byte[] value
                        // Assign value for an HRecord, but not for Annotated object
                        if (!(newobj instanceof HRecord))
                            return;

                        final HRecord hrecord = (HRecord)newobj;
                        hrecord.setCurrentValue(familyName + ":" + columnName, currentValueBytes, false);
                    }
                }
            }

            // Bail if no versions were requested
            if (maxVersions <= 1)
                continue;

            final NavigableMap<byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>> familyMap = result.getMap();
            final NavigableMap<byte[], NavigableMap<Long, byte[]>> versionColumnMap = familyMap.get(familyNameBytes);

            if (versionColumnMap == null)
                continue;

            for (final byte[] columnBytes : versionColumnMap.keySet()) {

                final NavigableMap<Long, byte[]> timeStampMap = versionColumnMap.get(columnBytes);
                final String columnName = HUtil.ser.getStringFromBytes(columnBytes);

                if (columnName.endsWith("]")) {

                    final int lbrace = columnName.indexOf("[");
                    final String mapColumn = columnName.substring(0, lbrace);
                    final String mapKey = columnName.substring(lbrace + 1, columnName.length() - 1);

                    final ColumnAttrib attrib = this.getSchema().getAttribFromFamilyQualifiedName(familyName,
                                                                                                  mapColumn);

                    if (attrib != null) {
                        for (final Long timestamp : timeStampMap.keySet()) {
                            final Object val = attrib.getValueFromBytes(newobj, timeStampMap.get(timestamp));
                            attrib.setKeysAsColumnsVersionValue(newobj, timestamp, mapKey, val);
                        }
                    }
                    else {
                        // Set unknown attrib value to byte[] value
                        // Find value in results and assign the byte[] value to HRecord, but bail on Annotated object
                        if (!(newobj instanceof HRecord))
                            return;

                        final HRecord hrecord = (HRecord)newobj;
                        for (final Long timestamp : timeStampMap.keySet())
                            hrecord.setKeysAsColumnsVersionValue(familyName + ":" + columnName,
                                                                 mapKey,
                                                                 timestamp,
                                                                 timeStampMap.get(timestamp),
                                                                 false);
                    }
                }
                else {
                    final ColumnAttrib attrib = this.getSchema().getAttribFromFamilyQualifiedName(familyName,
                                                                                                  columnName);

                    // Ignore data if no version map exists for the column

                    if (attrib == null) {

                        if (!(newobj instanceof HRecord))
                            return;

                        final Map<Long, Object> mapval = new TreeMap();
                        for (final Long timestamp : timeStampMap.keySet()) {
                            final byte[] b = timeStampMap.get(timestamp);
                            mapval.put(timestamp, b);
                        }

                        final HRecord hrecord = (HRecord)newobj;
                        hrecord.setVersionedValueMap(familyName + ":" + columnName, mapval, false);
                    }
                    else {
                        // Ignore if not in select list
                        if (!selectAttribList.contains(attrib))
                            continue;

                        Map<Long, Object> mapval = attrib.getVersionValueMapValue(newobj);

                        if (mapval == null) {
                            mapval = new TreeMap();
                            attrib.setVersionValueMapValue(newobj, mapval);
                        }
                        for (final Long timestamp : timeStampMap.keySet()) {
                            final byte[] b = timeStampMap.get(timestamp);
                            final Object val = attrib.getValueFromBytes(newobj, b);
                            mapval.put(timestamp, val);
                        }
                    }
                }
            }
        }
    }
}
