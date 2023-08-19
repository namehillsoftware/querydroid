package com.namehillsoftware.querydroid;

import android.database.sqlite.SQLiteDatabase;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class SqLiteAssistants {

    private static class ClassCache {
        private static final Map<Class<?>, ClassReflections> classCache = new HashMap<>();

        static synchronized <T extends Class<?>> ClassReflections getReflections(T cls) {
            if (!classCache.containsKey(cls))
                classCache.put(cls, new ClassReflections(cls));

            return classCache.get(cls);
        }
    }

    private static class ClassReflections {

        final Map<String, Field> fieldMap = new HashMap<>();
        final Map<String, Method> getterMap = new HashMap<>();

        <T extends Class<?>> ClassReflections(T cls) {
            for (final Field f : cls.getFields()) {
                fieldMap.put(f.getName().toLowerCase(Locale.ROOT), f);
            }

            // prepare methods. Methods will override fields, if both exists.
            for (final Method method : cls.getMethods()) {
                final String name = method.getName();
                if (name.equals("getClass")) continue;

                if (name.startsWith("get")) {
                    getterMap.put(name.replaceFirst("get", "").toLowerCase(Locale.ROOT), method);
                    continue;
                }

                if (name.startsWith("is")) {
                    final Class<?> returnCls = method.getReturnType();
                    if (Boolean.class.equals(returnCls) || Boolean.TYPE.equals(returnCls))
                        getterMap.put(name.toLowerCase(Locale.ROOT), method);
                }
            }
        }
    }

    private static final ConcurrentHashMap<Class<?>, String> cachedInsertStatements = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, String> cachedUpdateStatements = new ConcurrentHashMap<>();

    public static <T> long insertValue(SQLiteDatabase database, String table, T value) throws InvocationTargetException, IllegalAccessException {
        final InsertBuilder insertBuilder = InsertBuilder.fromTable(table);

        final Class<?> cls = value.getClass();
        final ClassReflections classReflections = ClassCache.getReflections(cls);

        String insertCommand = cachedInsertStatements.get(cls);
        if (insertCommand == null) {
            for (String getterKey : classReflections.getterMap.keySet()) {
                if (!Objects.equals(getterKey, "id"))
                    insertBuilder.addColumn(getterKey);
            }

            insertCommand = insertBuilder.buildQuery();
            cachedInsertStatements.put(cls, insertCommand);
        }

        final SqLiteCommand command = new SqLiteCommand(database, insertCommand);
        for (Map.Entry<String, Method> getterEntry : classReflections.getterMap.entrySet()) {
            command.addParameter(getterEntry.getKey(), getterEntry.getValue().invoke(value));
        }

        return command.execute();
    }

    public static  <T> long updateValue(SQLiteDatabase database, String table, T value) throws InvocationTargetException, IllegalAccessException {
        final UpdateBuilder updateBuilder = UpdateBuilder.fromTable(table);

        final Class<?> cls = value.getClass();
        final ClassReflections classReflections = ClassCache.getReflections(cls);

        String updateCommand = cachedUpdateStatements.get(cls);
        if (updateCommand == null) {
            for (String getterKey : classReflections.getterMap.keySet()) {
                if (!Objects.equals(getterKey, "id"))
                    updateBuilder.addSetter(getterKey);
            }

            updateBuilder.setFilter("where id = @id");

            updateCommand = updateBuilder.buildQuery();
            cachedUpdateStatements.put(cls, updateCommand);
        }

        final SqLiteCommand command = new SqLiteCommand(database, updateCommand);
        for (Map.Entry<String, Method> getterEntry : classReflections.getterMap.entrySet()) {
            command.addParameter(getterEntry.getKey(), getterEntry.getValue().invoke(value));
        }

        return command.execute();
    }

    public static class InsertBuilder {
        private final StringBuilder sqlStringBuilder;
        private final ArrayList<String> columns = new ArrayList<>();

        public static InsertBuilder fromTable(String tableName) {
            return new InsertBuilder(tableName);
        }

        private InsertBuilder(String tableName) {
            sqlStringBuilder = new StringBuilder("INSERT INTO " + tableName + " (");
        }

        public InsertBuilder addColumn(String column) {
            columns.add(column);

            return this;
        }

        /** @noinspection StringEquality*/
        public String buildQuery() {
            for (String column : columns) {
                sqlStringBuilder.append(column);
                if (column != columns.get(columns.size() - 1))
                    sqlStringBuilder.append(", ");
            }

            sqlStringBuilder.append(") VALUES (");

            for (String column : columns) {
                sqlStringBuilder.append('@').append(column);
                if (column != columns.get(columns.size() - 1))
                    sqlStringBuilder.append(", ");
            }

            return sqlStringBuilder.append(')').toString();
        }
    }

    public static class UpdateBuilder {
        private final StringBuilder sqlStringBuilder;
        private final ArrayList<String> setters = new ArrayList<>();
        private String filter = "";

        public static UpdateBuilder fromTable(String tableName) {
            return new UpdateBuilder(tableName);
        }

        private UpdateBuilder(String tableName) {
            sqlStringBuilder = new StringBuilder("UPDATE " + tableName + " SET ");
        }

        public UpdateBuilder addSetter(String columnName) {
            setters.add(columnName);
            return this;
        }

        public UpdateBuilder setFilter(String filter) {
            this.filter = filter;
            return this;
        }

        /** @noinspection StringEquality*/
        public String buildQuery() {
            for (String setter : setters) {
                sqlStringBuilder.append(setter).append(" = @").append(setter);
                if (setter != setters.get(setters.size()  - 1))
                    sqlStringBuilder.append(", ");
            }

            return sqlStringBuilder.append(' ').append(filter).toString();
        }
    }
}
