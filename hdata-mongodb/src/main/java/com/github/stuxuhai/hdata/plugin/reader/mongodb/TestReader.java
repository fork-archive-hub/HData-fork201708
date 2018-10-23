package com.github.stuxuhai.hdata.plugin.reader.mongodb;

import com.mongodb.client.*;
import org.bson.Document;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class TestReader {


    public static void main(String[] args) throws Throwable {

//        int a = 8;
//        int b = 3;
//        int d = (int) Math.ceil((double) a / (double) b);
//        System.out.println(d);


        MongoClient client = MongoClients.create("mongodb://europa:europa@192.168.1.188:27017");


//        MongoClient mongoClient = MongoClients.create(
//                MongoClientSettings.builder()
//                        .applyToClusterSettings(builder ->
//                                builder.hosts(Arrays.asList(new ServerAddress("host1", 27017))))
//                        .credential(credential)
//                        .build());
//        ServerAddress serverAddress = new ServerAddress("182.92.162.12", 27017);
//        MongoCredential.createPlainCredential()
//        MongoClient client = new MongoClient(serverAddress, Arrays.asList(MongoCredential.createCredential("zkhh", "wx", "zkhh123".toCharArray())), MongoClientOptions.builder().build());
        MongoDatabase database = client.getDatabase("test");
        MongoCollection collection = database.getCollection("timeType");
//        List<Bson> querys = new ArrayList<>();
//        querys.add(Filters.lte("id", "8"));
//        Long count = c.countDocuments(Filters.and(querys));
//        System.out.println(count);
//        c.find()
//        Document.parse("{\"name\": \"}");
//        System.out.println(c.countDocuments(Filters.expr(Document.parse("{ $eq: [\"$name\", \"Anya\"] }"))));
//        String cursorValue = "";
//
//        Document max = (Document) c.find().sort(new BasicDBObject("_id", -1)).iterator().next();
//        String maxId = max.getObjectId("_id").toHexString();
//        List<Bson> query = new ArrayList<>();
//        if (StringUtils.isNotBlank(cursorValue)) {
//            query.add(Filters.gt("_id", new ObjectId(cursorValue)));
//        }
//        query.add(Filters.lte("_id", new ObjectId(maxId)));
//        Long count = c.countDocuments(Filters.and(query));
//        System.out.println("count = " + count);
//        int MIN_BATCH_SIZE = 5000;
//        int parallelism = 2;
//        int batch = MIN_BATCH_SIZE;
//        int pCount = count.intValue() / parallelism;
//        if (batch < pCount) {
//            batch = pCount;
//        }
//        for (int i = 0; i < parallelism; i++) {
//            int skip = i * batch;
//            if (skip > count) {
//                break;
//            }
//            System.out.println("skip = " + skip + ", batch = " + batch);
//            PluginConfig otherReaderConfig = (PluginConfig) readerConfig.clone();
//            FindIterable<Document> iterable = c.find(Filters.and(query)).skip(skip).limit(batch);
//            otherReaderConfig.put(MongoDBReaderProperties.ITERATOR, iterable);
//            ret.add(otherReaderConfig);
//        }

//        collection.

//        Bson sort = new BasicDBObject("_id", -1);
//        FindIterable<Document> iterable = collection.find().limit(10);
        DateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        String str = "2018-10-17 15:56:00";
//        Date time = timeFormat.parse(str);
//        BsonTimestamp bsonTimestamp = new BsonTimestamp((int) (time.getTime() / 1000), 1);
        FindIterable<Document> iterable = collection.find();
//        FindIterable<Document> iterable = collection.find(Filters.gt("time", bsonTimestamp));
        MongoCursor<Document> cursor = iterable.iterator();
        while (cursor.hasNext()) {
            Document document = cursor.next();
            Object data = document.get("time");
            System.out.println("json = " + DataConvert.getInstance().convert(data));
        }
//        while (cursor.hasNext()) {
//            Document document = cursor.next();
//            ObjectId id = (ObjectId)document.get("_id");
//            System.out.println(id.toHexString());//5b76632e3cfa776c1ae3fd19
//            System.out.println(Long.valueOf(id, 16));
//        iterable.
//        while(iterable.){
//
//        }
//        long l = collection.countDocuments();
//        System.out.println(l);
    }
}
