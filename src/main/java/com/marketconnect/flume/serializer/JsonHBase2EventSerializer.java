package com.marketconnect.flume.serializer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.FlumeException;
import org.apache.flume.conf.ComponentConfiguration;
import org.apache.flume.sink.hbase2.HBase2EventSerializer;
import org.apache.hadoop.hbase.client.Increment;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Row;
import org.apache.hadoop.hbase.util.Bytes;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.ReadContext;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonHBase2EventSerializer implements HBase2EventSerializer {
    private static final Logger logger =
            LoggerFactory.getLogger(JsonHBase2EventSerializer.class);

  // Config vars
  /** event header where we can find the JSON Array with all the object to put in the row. */
  public static final String COL_NAME_CONFIG = "colName";
  public static final String COLUMN_NAME_DEFAULT = "col";

  /** JSON Path in the previous JSON object where we can find the hbase column name. */
  public static final String COL_ID_CONFIG = "colId";
  public static final String COLUMN_ID_DEFAULT = "id";

  /** What charset to use when serializing into HBase's byte arrays */
  public static final String CHARSET_CONFIG = "charset";
  public static final String CHARSET_DEFAULT = "UTF-8";

  protected byte[] cf;
  private byte[] payload;
  private String colName;
  private String colId;
  private Map<String, String> headers;
  private Charset charset;

  @Override
  public void configure(Context context) {
    charset = Charset.forName(context.getString(CHARSET_CONFIG,
        CHARSET_DEFAULT));

    colName = context.getString(COL_NAME_CONFIG, COLUMN_NAME_DEFAULT);
    colId = context.getString(COL_ID_CONFIG, COLUMN_ID_DEFAULT);

  }

  @Override
  public void configure(ComponentConfiguration conf) {
  }

  @Override
  public void initialize(Event event, byte[] columnFamily) {
    this.headers = event.getHeaders();
    this.payload = event.getBody();
    this.cf = Arrays.copyOf(columnFamily, columnFamily.length);
  }

  @Override
  public List<Row> getActions() throws FlumeException {
    List<Row> actions = Lists.newArrayList();
    byte[] rowKey = this.payload;

    if (rowKey.length == 0) {
        return Lists.newArrayList();
    }

    if (headers.isEmpty()) {
        return Lists.newArrayList();
    }

    Put put = new Put(rowKey);

    String json = headers.get(this.colName);

    if (json != null) {
        ReadContext ctx = JsonPath.parse(json);
        Object result = ctx.read("$.[*]");
        if (result instanceof List) {
            Iterator it = ((List) result).iterator();
            while (it.hasNext()) {
                Map object = (Map) it.next();
                String columnIdStr = (String) object.get(this.colId);
                try {
                    put.addColumn(cf, columnIdStr.getBytes(charset), Bytes.toBytes(JSONObject.toJSONString(object)));
                } catch (NullPointerException e) {
                    logger.error("NullPointerException for colId " + columnIdStr + " in " + object.toString());
                } catch (Exception e) {
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    String exceptionAsString = sw.toString();
                    throw new FlumeException(e.toString() + " row key " + Bytes.toString(rowKey) + " columnIdStr " + columnIdStr + " " + object.toString() + " " + exceptionAsString);
                }
            }
        }
    }
    if (put.size() > 0)
        actions.add(put);
    return actions;
  }

  @Override
  public List<Increment> getIncrements() {
    List<Increment> incs = new LinkedList<>();
    return incs;
  }

  @Override
  public void close() {  }
}
