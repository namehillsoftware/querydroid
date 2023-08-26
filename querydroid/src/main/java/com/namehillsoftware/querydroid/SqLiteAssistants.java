package com.namehillsoftware.querydroid;

import android.database.sqlite.SQLiteDatabase;

import com.namehillsoftware.lazyj.AbstractSynchronousLazy;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class SqLiteAssistants {

    private static class ClassCache {
        private static final Map<Class<?>, ClassReflections> classCache = new ConcurrentHashMap<>();

        static <T extends Class<?>> ClassReflections getReflections(T cls) {
            if (!classCache.containsKey(cls))
                classCache.put(cls, new ClassReflections(cls));

            return classCache.get(cls);
        }
    }

    private static class ClassReflections {
        final AbstractSynchronousLazy<Map<String, IGetter>> getterMap;

        <T extends Class<?>> ClassReflections(T cls) {
            getterMap = new AbstractSynchronousLazy<>() {
                @Override
                protected Map<String, IGetter> create() {
                    final HashMap<String, IGetter> newMap = new HashMap<>();
                    for (final Field f : cls.getDeclaredFields()) {
                        if (!Modifier.isStatic(f.getModifiers()) && Modifier.isPublic(f.getModifiers()))
                            newMap.put(f.getName().toLowerCase(Locale.ROOT), new FieldGetter(f));
                    }

                    // prepare methods. Methods will override fields, if both exists.
                    for (final Method method : cls.getDeclaredMethods()) {
                        final int modifiers = method.getModifiers();
                        if (Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) continue;

                        final String name = method.getName();
                        if (name.equals("getClass")) continue;

                        if (name.startsWith("get")) {
                            newMap.put(name.substring(3).toLowerCase(Locale.ROOT), new MethodGetter(method));
                            continue;
                        }

                        if (name.startsWith("is")) {
                            final Class<?> returnCls = method.getReturnType();
                            if (Boolean.class.equals(returnCls) || Boolean.TYPE.equals(returnCls))
                                newMap.put(name.toLowerCase(Locale.ROOT), new MethodGetter(method));
                        }
                    }

                    return newMap;
                }
            };
        }
    }

    private static class FieldGetter implements IGetter {
       private final Field receiver;

        private FieldGetter(Field receiver) {
            this.receiver = receiver;
        }

        @Override
        public Object get(Object target) {
            try {
                return receiver.get(target);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class MethodGetter implements IGetter {
        private final Method receiver;

        private MethodGetter(Method receiver) {
            this.receiver = receiver;
        }

        @Override
        public Object get(Object target) {
            try {
                return receiver.invoke(target);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private interface IGetter {
        Object get(Object target);
    }

    private static final ConcurrentHashMap<String, String> cachedInsertStatements = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> cachedUpdateStatements = new ConcurrentHashMap<>();

    public static <T> long insertValue(SQLiteDatabase database, String table, T value) {
        final Class<?> cls = value.getClass();
        final ClassReflections classReflections = ClassCache.getReflections(cls);

        final Map<String, IGetter> getterMap = classReflections.getterMap.getObject();

        table = table.toLowerCase(Locale.ROOT);
        String insertCommand = cachedInsertStatements.get(table);
        if (insertCommand == null) {
            final InsertBuilder insertBuilder = InsertBuilder.fromTable(table);
            for (String getterKey : getterMap.keySet()) {
                if (!Objects.equals(getterKey, "id"))
                    insertBuilder.addColumn(getterKey);
            }

            insertCommand = insertBuilder.buildQuery();
            cachedInsertStatements.put(table, insertCommand);
        }

        final SqLiteCommand command = new SqLiteCommand(database, insertCommand);
        for (Map.Entry<String, IGetter> getterEntry : getterMap.entrySet()) {
            command.addParameter(getterEntry.getKey(), getterEntry.getValue().get(value));
        }

        return command.execute();
    }

    public static  <T> long updateValue(SQLiteDatabase database, String table, T value) {
        final Class<?> cls = value.getClass();
        final ClassReflections classReflections = ClassCache.getReflections(cls);

        final Map<String, IGetter> getterMap = classReflections.getterMap.getObject();

        table = table.toLowerCase(Locale.ROOT);
        String updateCommand = cachedUpdateStatements.get(table);
        if (updateCommand == null) {
            final UpdateBuilder updateBuilder = UpdateBuilder.fromTable(table);
            for (String getterKey : getterMap.keySet()) {
                if (!Objects.equals(getterKey, "id"))
                    updateBuilder.addSetter(getterKey);
            }

            updateBuilder.setFilter("where id = @id");

            updateCommand = updateBuilder.buildQuery();
            cachedUpdateStatements.put(table, updateCommand);
        }

        final SqLiteCommand command = new SqLiteCommand(database, updateCommand);
        for (Map.Entry<String, IGetter> getterEntry : getterMap.entrySet()) {
            command.addParameter(getterEntry.getKey(), getterEntry.getValue().get(value));
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
