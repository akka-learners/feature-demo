package com.learning.akka;

import java.util.HashMap;
import java.util.Map;

public  class DummyDB {
    public static final DummyDB instance = new DummyDB();
    private final Map<String, Long> db = new HashMap<String, Long>();

    private DummyDB() {
    }

    public synchronized void save(String key, Long value) throws StorageApi.StorageException {
        if (11 <= value && value <= 14)
            throw new StorageApi.StorageException("Simulated store failure " + value);
        db.put(key, value);
    }

    public synchronized Long load(String key) throws StorageApi.StorageException {
        return db.get(key);
    }
}
