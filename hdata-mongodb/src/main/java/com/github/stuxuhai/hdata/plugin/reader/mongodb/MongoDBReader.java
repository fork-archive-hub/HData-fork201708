package com.github.stuxuhai.hdata.plugin.reader.mongodb;

import com.github.stuxuhai.hdata.api.*;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.apache.commons.lang3.StringUtils;
import org.bson.BsonTimestamp;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MongoDBReader extends Reader {

    private static int MIN_BATCH_SIZE = 5000;

    private FindIterable<Document> iterable;

    private String[] columns;

    @Override
    public void prepare(JobContext context, PluginConfig readerConfig) {
        this.iterable = (FindIterable<Document>) readerConfig.get(MongoDBReaderProperties.ITERATOR);
        this.columns = readerConfig.getString(MongoDBReaderProperties.COLUMNS).split(",");
    }

    @Override
    public void execute(RecordCollector recordCollector) {
        if (this.iterable != null) {
            MongoCursor<Document> cursor = this.iterable.iterator();
            while (cursor.hasNext()) {
                Document document = cursor.next();
                Record r = new DefaultRecord(this.columns.length);
                for (String column : this.columns) {
                    Object obj = document.get(column);
                    String str = DataConvert.getInstance().convert(obj);
                    r.add(str);
                }
                recordCollector.send(r);
            }
        }
    }

    private MongoCollection getMongoCollection(String address, int port, String username, String password, String database, String collection) {
        ServerAddress serverAddress = new ServerAddress(address, port);
        MongoClient mongoClient;
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
            MongoCredential mongoCredential = MongoCredential.createCredential(username, database, password.toCharArray());
            mongoClient = new MongoClient(serverAddress, Arrays.asList(mongoCredential));
        } else {
            mongoClient = new MongoClient(serverAddress);
        }
        MongoDatabase db = mongoClient.getDatabase(database);
        return db.getCollection(collection);
    }

    private SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private Object convertCursorValue(String value, String cursorType) {
        if (cursorType != null) {
            switch (cursorType) {
                case "short":
                case "int":
                    return Integer.valueOf(value);
                case "decimal":
                case "double":
                case "float":
                    return Float.valueOf(value);
                case "bigint":
                    return Long.valueOf(value);
                case "date":
                    try {
                        return timeFormat.parse(value);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                case "timestamp":
                    try {
                        Date time = timeFormat.parse(value);
                        return new BsonTimestamp((int) (time.getTime() / 1000), 1);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
            }
        }
        return value;
    }

    @Override
    public Splitter newSplitter() {
        return new Splitter() {
            @Override
            public List<PluginConfig> split(JobConfig jobConfig) {
                List<PluginConfig> ret = new ArrayList();

                PluginConfig readerConfig = jobConfig.getReaderConfig();
                String address = readerConfig.getString(MongoDBReaderProperties.ADDRESS);
                int port = readerConfig.getInt(MongoDBReaderProperties.PORT, 27017);
                String database = readerConfig.getString(MongoDBReaderProperties.DATABASE);
                String collection = readerConfig.getString(MongoDBReaderProperties.COLLECTION);
                String query = readerConfig.getString(MongoDBReaderProperties.QUERY);
                String username = readerConfig.getString(MongoDBReaderProperties.USERNAME);
                String password = readerConfig.getString(MongoDBReaderProperties.PASSWORD);
                String cursorColumn = readerConfig.getString(MongoDBReaderProperties.CURSOR_COLUMN);
                String cursorType = readerConfig.getString(MongoDBReaderProperties.CURSOR_TYPE);
                String cursorValue = readerConfig.getString(MongoDBReaderProperties.CURSOR_VALUE);

                int parallelism = readerConfig.getParallelism();

                MongoCollection c = getMongoCollection(address, port, username, password, database, collection);
                List<Bson> querys = new ArrayList<>();
                querys.add(Filters.expr(Document.parse("{ $eq: [1, 1] }")));
                if (StringUtils.isNotBlank(cursorColumn)) {
                    Document max = (Document) c.find().sort(new BasicDBObject(cursorColumn, -1)).iterator().next();
                    if (max != null) {
                        Object maxValue = max.get(cursorColumn);
                        jobConfig.setString("CursorValue", DataConvert.getInstance().convert(maxValue));
                        querys.add(Filters.lte(cursorColumn, maxValue));
                    }
                    if (StringUtils.isNotBlank(cursorValue)) {
                        querys.add(Filters.gt(cursorColumn, convertCursorValue(cursorValue, cursorType)));
                    }
                }
                if (StringUtils.isNotBlank(query)) {
                    querys.add(Filters.expr(Document.parse(query)));
                }
                Long count = c.countDocuments(Filters.and(querys));
                int batch = MIN_BATCH_SIZE;
                int pCount = count.intValue() / parallelism;
                if (batch < pCount) {
                    batch = pCount;
                }
                for (int i = 0; i < parallelism; i++) {
                    int skip = i * batch;
                    if (skip > count) {
                        break;
                    }
                    PluginConfig otherReaderConfig = (PluginConfig) readerConfig.clone();
                    FindIterable<Document> iterable = c.find(Filters.and(querys)).skip(skip).limit(batch);
                    otherReaderConfig.put(MongoDBReaderProperties.ITERATOR, iterable);
                    ret.add(otherReaderConfig);
                }
                jobConfig.getWriterConfig().setInt("parallelism", ret.size());
                return ret;
            }
        };
    }

}
