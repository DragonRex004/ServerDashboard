package de.dragonrex.serverdashboard.database.processor;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import de.dragonrex.serverdashboard.database.Database;
import de.dragonrex.serverdashboard.database.DatabaseResult;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MongoProcessor implements IProcessor {
    protected final Database database;
    protected final String databaseName;
    protected MongoClient client;
    protected MongoDatabase mongoDatabase;

    public MongoProcessor(Database database, String databaseName) {
        this.database = database;
        this.databaseName = databaseName;
    }

    @Override
    public void connect() {
        try {
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(database.getJdbcUrl()))
                    .build();
            this.client = MongoClients.create(settings);
            this.mongoDatabase = this.client.getDatabase(this.databaseName);

            this.mongoDatabase.listCollectionNames().first();
        } catch (Exception e) {
            throw new RuntimeException("Failed to establish MongoDB connection to database: " + this.databaseName, e);
        }
    }

    @Override
    public void disconnect() {
        if (this.client != null) {
            try {
                this.client.close();
            } catch (Exception e) {
                throw new RuntimeException("Failed to disconnect from MongoDB", e);
            }
        }
    }

    @Override
    public DatabaseResult query(String query, Object... args) {
        try {
            MongoCollection<Document> collection = this.mongoDatabase.getCollection(query);
            List<Bson> filters = new ArrayList<>();

            for (Object arg : args) {
                String[] parts = arg.toString().split(":", 2);
                if (parts.length == 2) {
                    filters.add(Filters.eq(parts[0], parts[1]));
                }
            }

            FindIterable<Document> results = filters.isEmpty() ? 
                collection.find() : collection.find(Filters.and(filters));

            List<DatabaseResult.Row> rows = new ArrayList<>();
            for (Document doc : results) {
                DatabaseResult.Row row = new DatabaseResult.Row();
                for (String key : doc.keySet()) {
                    row.put(key, doc.get(key));
                }
                rows.add(row);
            }

            return new DatabaseResult(rows);
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute MongoDB query on collection: " + query, e);
        }
    }

    @Override
    public DatabaseResult update(String query, Object... args) {
        try {
            MongoCollection<Document> collection = this.mongoDatabase.getCollection(query);

            if (args.length < 2) {
                throw new IllegalArgumentException("MongoDB update requires at least operation and document parameters");
            }

            String operation = (String) args[0];
            Document document = Document.parse((String) args[1]);
            List<DatabaseResult.Row> rows = new ArrayList<>();
            DatabaseResult.Row row = new DatabaseResult.Row();

            switch (operation.toUpperCase()) {
                case "INSERT" -> {
                    InsertOneResult insertResult = collection.insertOne(document);
                    row.put("insertedId", Objects.requireNonNull(insertResult.getInsertedId()).toString());
                    row.put("acknowledged", insertResult.wasAcknowledged());
                }
                case "UPDATE" -> {
                    List<Bson> filters = createFiltersFromArgs(args, 2);
                    UpdateResult updateResult = collection.updateOne(Filters.and(filters), document);
                    row.put("matchedCount", updateResult.getMatchedCount());
                    row.put("modifiedCount", updateResult.getModifiedCount());
                    row.put("acknowledged", updateResult.wasAcknowledged());
                }
                case "DELETE" -> {
                    List<Bson> filters = createFiltersFromArgs(args, 2);
                    DeleteResult deleteResult = collection.deleteOne(Filters.and(filters));
                    row.put("deletedCount", deleteResult.getDeletedCount());
                    row.put("acknowledged", deleteResult.wasAcknowledged());
                }
                default -> throw new IllegalArgumentException("Unsupported MongoDB operation: " + operation);
            }

            rows.add(row);
            return new DatabaseResult(rows);
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute MongoDB update on collection: " + query, e);
        }
    }

    @Override
    public Connection getConnection() {
        return null;
    }

    private List<Bson> createFiltersFromArgs(Object[] args, int startIndex) {
        List<Bson> filters = new ArrayList<>();
        for (int i = startIndex; i < args.length; i++) {
            String[] parts = args[i].toString().split(":", 2);
            if (parts.length == 2) {
                filters.add(Filters.eq(parts[0], parts[1]));
            }
        }
        return filters;
    }

    protected String getDatabaseType() {
        return "MongoDB";
    }
}
