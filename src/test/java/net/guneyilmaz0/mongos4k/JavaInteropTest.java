package net.guneyilmaz0.mongos4k;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import net.guneyilmaz0.mongos4k.exceptions.MongoSException;
import net.guneyilmaz0.mongos4k.exceptions.MongoSReadException;
import org.bson.Document;
import org.junit.jupiter.api.Test;

/** Verifies the public API is usable from plain Java (compile-time and runtime). */
class JavaInteropTest {
    static class Person extends MongoSObject {
        String name = "Ada";
    }

    @Test
    void constantsAreStaticFields() {
        assertEquals("key", Database.KEY_FIELD);
        assertEquals("value", Database.VALUE_FIELD);
        assertNotNull(Database.gson);
    }

    @Test
    void numberConversionFromJava() {
        assertEquals(5, Database.convertNumber(5L, Integer.class));
        assertEquals(5L, Database.convertNumber(5, long.class));
    }

    @Test
    void apiShapesCompileFromJava() throws Exception {
        Database db = new Database();
        Document doc = db.convertJsonToDocument("{\"a\":1}");
        assertEquals(1, doc.getInteger("a"));
        // Overloads with defaults / Class<T> must be reachable; just reference them.
        Runnable r = () -> {
            db.set("c", "k", "v");
            db.set("c", "k", "v", true);
            String s = db.get("c", "k", String.class);
            String d = db.get("c", "k", String.class, "fallback");
            String n = db.getOrNull("c", "k", String.class);
            db.getAllList("c", String.class);
            db.getAllMap("c", String.class, Collections.emptyMap());
        };
        assertNotNull(r);
        Person p = new Person();
        assertTrue(p.isValid());
        assertNotNull(p.toDocument());
    }

    @Test
    void exceptionsConstructibleFromJava() {
        MongoSException e = new MongoSReadException("boom");
        assertEquals("boom", e.getMessage());
        assertNotNull(new MongoSReadException("boom", new RuntimeException()).getCause());
    }
}
