package com.namehillsoftware.artful;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.namehillsoftware.lazyj.AbstractSynchronousLazy;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Artful {
	private final SQLiteDatabase database;
	private final String sqlQuery;
	private final HashMap<String, String> parameters = new HashMap<>();

	public Artful(SQLiteDatabase database, String sqlQuery) {
		this.database = database;
		this.sqlQuery = sqlQuery;
	}

	public Artful addParameter(String parameter, String value) {
		parameters.put(parameter, value);
		return this;
	}

	public <E extends Enum<E>> Artful addParameter(String parameter, Enum<E> value) {
		return addParameter(parameter, value != null ? value.name() : null);
	}

	public Artful addParameter(String parameter, short value) {
		return addParameter(parameter, String.valueOf(value));
	}

	public Artful addParameter(String parameter, int value) {
		return addParameter(parameter, String.valueOf(value));
	}

	public Artful addParameter(String parameter, long value) {
		return addParameter(parameter, String.valueOf(value));
	}

	public Artful addParameter(String parameter, float value) {
		return addParameter(parameter, String.valueOf(value));
	}

	public Artful addParameter(String parameter, double value) {
		return addParameter(parameter, String.valueOf(value));
	}

	public Artful addParameter(String parameter, boolean value) {
		return addParameter(parameter, value ? 1 : 0);
	}

	public Artful addParameter(String parameter, Boolean value) {
		return addNullable(parameter, value)
			? this
			: addParameter(parameter, value.booleanValue());
	}

	public Artful addParameter(String parameter, Object value) {
		if (addNullable(parameter, value)) {
			return this;
		}

		final Class<?> valueClass = value.getClass();

		if (Boolean.TYPE.equals(valueClass)) {
			addParameter(parameter, (boolean)value);
			return this;
		}

		if (Boolean.class.equals(valueClass)) {
			addParameter(parameter, (Boolean) value);
			return this;
		}

		if (Short.TYPE.equals(valueClass)) {
			addParameter(parameter, (short)value);
			return this;
		}

		if (Integer.TYPE.equals(valueClass)) {
			addParameter(parameter, (int)value);
			return this;
		}

		if (Long.TYPE.equals(valueClass)) {
			addParameter(parameter, (long)value);
			return this;
		}

		if (Float.TYPE.equals(valueClass)) {
			addParameter(parameter, (float)value);
			return this;
		}

		if (Double.TYPE.equals(valueClass)) {
			addParameter(parameter, (double)value);
			return this;
		}

		if (valueClass.isEnum()) {
			addParameter(parameter, (Enum<?>) value);
		}

		return addParameter(parameter, value.toString());
	}

	public Artful addParameters(Map<String, Object> parameters) {
		for (Map.Entry<String, Object> parameter : parameters.entrySet()) {
			final String key = parameter.getKey();
			final Object value = parameter.getValue();

			addParameter(key, value);
		}

		return this;
	}

	public <T> List<T> fetch(Class<T> cls) throws SQLException {
        try (Cursor cursor = getCursorForQuery()) {
            if (!cursor.moveToFirst()) return new ArrayList<>();

            final ArrayList<T> returnObjects = new ArrayList<>(cursor.getCount());
            do {
                returnObjects.add(mapDataFromCursorToClass(cursor, cls));
            } while (cursor.moveToNext());

            return returnObjects;
        }
	}

	public <T> T fetchFirst(Class<T> cls) {
        try (Cursor cursor = getCursorForQuery()) {
            if (!cursor.moveToFirst() || cursor.getCount() == 0) return null;

            return mapDataFromCursorToClass(cursor, cls);
        }
	}

	private boolean addNullable(String parameter, Object value) {
		if (value == null) {
			parameters.put(parameter, null);
			return true;
		}

		return false;
	}

	private Cursor getCursorForQuery() {
		final Map.Entry<String, String[]> compatibleSqlQuery = QueryCache.getSqlQuery(sqlQuery, parameters);

		return database.rawQuery(compatibleSqlQuery.getKey(), compatibleSqlQuery.getValue());
	}

	private static <T> T mapDataFromCursorToClass(Cursor cursor, Class<T> cls) {
		final ClassReflections reflections = ClassCache.getReflections(cls);

		final T newObject;
		try {
			newObject = cls.newInstance();
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}

		for (int i = 0; i < cursor.getColumnCount(); i++) {
			String colName = cursor.getColumnName(i).toLowerCase(Locale.ROOT);

			if (reflections.setterMap.containsKey(colName)) {
				reflections.setterMap.get(colName).set(newObject, cursor.getString(i));
				continue;
			}

			if (!colName.startsWith("is")) continue;

			colName = colName.substring(2);
			if (reflections.setterMap.containsKey(colName))
				reflections.setterMap.get(colName).set(newObject, cursor.getString(i));
		}

		return newObject;
	}

	public long execute() throws SQLException {
		final Map.Entry<String, String[]> compatibleSqlQuery = QueryCache.getSqlQuery(sqlQuery, parameters);

		final String sqlQuery = compatibleSqlQuery.getKey();

        try (SQLiteStatement sqLiteStatement = database.compileStatement(sqlQuery)) {
            final String[] args = compatibleSqlQuery.getValue();
            for (int i = 0; i < args.length; i++) {
                final String arg = args[i];
                if (arg != null)
                    sqLiteStatement.bindString(i + 1, arg);
                else
                    sqLiteStatement.bindNull(i + 1);
            }

            return executeSpecial(sqLiteStatement, sqlQuery);
        }
	}

	private static long executeSpecial(SQLiteStatement sqLiteStatement, String sqlQuery) {
		final String sqlQueryType = sqlQuery.trim().substring(0, 3).toLowerCase(Locale.ROOT);
		if (sqlQueryType.equals("upd") || sqlQueryType.equals("del"))
			return sqLiteStatement.executeUpdateDelete();
		if (sqlQueryType.equals("ins"))
			return sqLiteStatement.executeInsert();
		return sqLiteStatement.simpleQueryForLong();
	}

	private static class QueryCache {
		private static final Map<String, Map.Entry<String, String[]>> queryCache = new HashMap<>();

		static synchronized Map.Entry<String, String[]> getSqlQuery(String sqlQuery, Map<String, String> parameters) {
			sqlQuery = sqlQuery.trim();
			if (queryCache.containsKey(sqlQuery))
				return getOrderedSqlParameters(queryCache.get(sqlQuery), parameters);

			final ArrayList<String> sqlParameters = new ArrayList<>();
			final StringBuilder sqlQueryBuilder = new StringBuilder(sqlQuery);
			int paramIndex;

			for (int i = 0; i < sqlQueryBuilder.length(); i++) {
				final char queryChar = sqlQueryBuilder.charAt(i);

				if (queryChar == '\'') {
					i = sqlQueryBuilder.indexOf("'", ++i);

					if (i < 0) break;

					continue;
				}

				if (queryChar != '@') continue;

				paramIndex = i;
				final StringBuilder paramStringBuilder = new StringBuilder();
				while (++paramIndex < sqlQueryBuilder.length()) {
					final char paramChar = sqlQueryBuilder.charAt(paramIndex);

					// A parameter needs to look like a Java identifier
					if (paramIndex == i + 1 && !Character.isJavaIdentifierStart(paramChar)) break;
					if (!Character.isJavaIdentifierPart(paramChar)) break;

					paramStringBuilder.append(paramChar);
				}

				sqlParameters.add(paramStringBuilder.toString());
				sqlQueryBuilder.replace(paramIndex - paramStringBuilder.length() - 1, paramIndex, "?");
			}

			final Map.Entry<String, String[]> entry = new AbstractMap.SimpleImmutableEntry<>(sqlQueryBuilder.toString(), sqlParameters.toArray(new String[sqlParameters.size()]));

			queryCache.put(sqlQuery, entry);

			return getOrderedSqlParameters(entry, parameters);
		}

		private static Map.Entry<String, String[]> getOrderedSqlParameters(Map.Entry<String, String[]> cachedQuery, Map<String, String> parameters) {
			final String[] parameterHolders = cachedQuery.getValue();
			final String[] newParameters = new String[parameterHolders.length];
			for (int i = 0; i < parameterHolders.length; i++) {
				final String parameterName = parameterHolders[i];
				if (!parameters.containsKey(parameterName)) continue;

				final String parameterValue = parameters.get(parameterName);
				newParameters[i] = parameterValue;
			}

			return new AbstractMap.SimpleImmutableEntry<>(cachedQuery.getKey(), newParameters);
		}
	}

	private static class ClassCache {
		private static final Map<Class<?>, ClassReflections> classCache = new HashMap<>();

		static synchronized <T extends Class<?>> ClassReflections getReflections(T cls) {
			if (!classCache.containsKey(cls))
				classCache.put(cls, new ClassReflections(cls));

			return classCache.get(cls);
		}
	}

	private interface ISetter {
		void set(Object object, String value);
	}

	private static class ClassReflections {
		final Map<String, ISetter> setterMap = new HashMap<>();

		<T extends Class<?>> ClassReflections(T cls) {

			for (final Field f : cls.getFields()) {
				setterMap.put(f.getName().toLowerCase(Locale.ROOT), new FieldSetter(f));
			}

			// prepare methods. Methods will override fields, if both exists.
			for (Method m : cls.getMethods()) {
				if (m.getParameterTypes().length == 1 && m.getName().startsWith("set"))
					setterMap.put(m.getName().substring(3).toLowerCase(Locale.ROOT), new MethodSetter(m));
			}
		}
	}

	private static class FieldSetter implements ISetter {
		private final Field field;
		private final Class<?> type;

		FieldSetter(Field field) {
			this.field = field;
			type = field.getType();
		}

		public void set(Object object, String value) {
			Class<?> currentType = type;
			while (currentType != Object.class) {
				if (setters.getObject().containsKey(currentType)) {
					setters.getObject().get(type).getObject().setFromString(field, object, value);
					break;
				}
				currentType = type.getSuperclass();
			}
		}

		private static final AbstractSynchronousLazy<HashMap<Type, AbstractSynchronousLazy<SetFields>>> setters = new AbstractSynchronousLazy<HashMap<Type, AbstractSynchronousLazy<SetFields>>>() {
			@Override
			protected final HashMap<Type, AbstractSynchronousLazy<SetFields>> create() {
				final HashMap<Type, AbstractSynchronousLazy<SetFields>> newHashMap = new HashMap<>();

				newHashMap.put(Boolean.TYPE, new AbstractSynchronousLazy<SetFields>() {
					@Override
					protected final SetFields create() {
						return (field, target, value) -> {
							try {
								if (isSqlValueNotNull(value))
									field.setBoolean(target, parseSqlBoolean(value));
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Boolean.class, new AbstractSynchronousLazy<SetFields>() {
					@Override
					protected final SetFields create() {
						return (field, target, value) -> {
							try {
								field.set(target, isSqlValueNotNull(value) ? parseSqlBoolean(value) : null);
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Short.TYPE, new AbstractSynchronousLazy<SetFields>() {
					@Override
					protected final SetFields create() {
						return (field, target, value) -> {
							try {
								if (isSqlValueNotNull(value))
									field.setShort(target, Short.parseShort(value));
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Short.class, new AbstractSynchronousLazy<SetFields>() {
					@Override
					protected final SetFields create() {
						return (field, target, value) -> {
							try {
								field.set(target, isSqlValueNotNull(value) ? Short.parseShort(value) : null);
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Integer.TYPE, new AbstractSynchronousLazy<SetFields>() {
					@Override
					protected final SetFields create() {
						return (field, target, value) -> {
							try {
								if (isSqlValueNotNull(value))
									field.setInt(target, Integer.parseInt(value));
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Integer.class, new AbstractSynchronousLazy<SetFields>() {
					@Override
					protected final SetFields create() {
						return (field, target, value) -> {
							try {
								field.set(target, isSqlValueNotNull(value) ? Integer.parseInt(value) : null);
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Long.TYPE, new AbstractSynchronousLazy<SetFields>() {
					@Override
					protected final SetFields create() {
						return (field, target, value) -> {
							try {
								if (isSqlValueNotNull(value))
									field.setLong(target, Long.parseLong(value));
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Long.class, new AbstractSynchronousLazy<SetFields>() {
					@Override
					protected final SetFields create() {
						return (field, target, value) -> {
							try {
								field.set(target, isSqlValueNotNull(value) ? Long.parseLong(value) : null);
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Float.TYPE, new AbstractSynchronousLazy<SetFields>() {
					@Override
					protected final SetFields create() {
						return (field, target, value) -> {
							try {
								if (isSqlValueNotNull(value))
									field.setFloat(target, Float.parseFloat(value));
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Float.class, new AbstractSynchronousLazy<SetFields>() {
					@Override
					protected final SetFields create() {
						return (field, target, value) -> {
							try {
								field.set(target, isSqlValueNotNull(value) ? Float.parseFloat(value) : null);
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Double.TYPE, new AbstractSynchronousLazy<SetFields>() {
					@Override
					protected final SetFields create() {
						return (field, target, value) -> {
							try {
								if (isSqlValueNotNull(value))
									field.setDouble(target, Double.parseDouble(value));
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Double.class, new AbstractSynchronousLazy<SetFields>() {
					@Override
					protected final SetFields create() {
						return (field, target, value) -> {
							try {
								field.set(target, isSqlValueNotNull(value) ? Double.parseDouble(value) : null);
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(String.class, new AbstractSynchronousLazy<SetFields>() {
					@Override
					protected final SetFields create() {
						return (field, target, value) -> {
							try {
								field.set(target, value);
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Enum.class, new AbstractSynchronousLazy<SetFields>() {
					@Override
					protected final SetFields create() {
						return (field, target, value) -> {
							try {
								if (isSqlValueNotNull(value))
									field.set(target, Enum.valueOf((Class<? extends Enum>) field.getType(), value));
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				return newHashMap;
			}
		};

		private interface SetFields {
			void setFromString(Field field, Object target, String value);
		}
	}

	private static class MethodSetter implements ISetter {
		private final Method method;
		private final Class<?> type;

		MethodSetter(Method method) {
			this.method = method;
			type = method.getParameterTypes()[0];
		}

		public void set(Object object, String value) {
			Class<?> currentType = type;
			while (currentType != Object.class) {
				if (setters.getObject().containsKey(currentType)) {
					setters.getObject().get(currentType).getObject().setFromString(method, object, value);
					break;
				}
				currentType = type.getSuperclass();
			}
		}

		private static final AbstractSynchronousLazy<HashMap<Class<?>, AbstractSynchronousLazy<SetMethods>>> setters = new AbstractSynchronousLazy<HashMap<Class<?>, AbstractSynchronousLazy<SetMethods>>>() {
			@Override
			protected final HashMap<Class<?>, AbstractSynchronousLazy<SetMethods>> create() {
				final HashMap<Class<?>, AbstractSynchronousLazy<SetMethods>> newHashMap = new HashMap<>();

				newHashMap.put(Boolean.TYPE, new AbstractSynchronousLazy<SetMethods>() {
					@Override
					protected final SetMethods create() {
						return (method, target, value) -> {
							try {
								if (isSqlValueNotNull(value))
									method.invoke(target, parseSqlBoolean(value));
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							} catch (InvocationTargetException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Boolean.class, new AbstractSynchronousLazy<SetMethods>() {
					@Override
					protected final SetMethods create() {
						return (method, target, value) -> {
							try {
								method.invoke(target, isSqlValueNotNull(value) ? parseSqlBoolean(value) : null);
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							} catch (InvocationTargetException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Short.TYPE, new AbstractSynchronousLazy<SetMethods>() {
					@Override
					protected final SetMethods create() {
						return (method, target, value) -> {
							try {
								if (isSqlValueNotNull(value))
									method.invoke(target, Short.parseShort(value));
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							} catch (InvocationTargetException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Short.class, new AbstractSynchronousLazy<SetMethods>() {
					@Override
					protected final SetMethods create() {
						return (method, target, value) -> {
							try {
								method.invoke(target, isSqlValueNotNull(value) ? Short.parseShort(value) : null);
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							} catch (InvocationTargetException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Integer.TYPE, new AbstractSynchronousLazy<SetMethods>() {
					@Override
					protected final SetMethods create() {
						return (method, target, value) -> {
							try {
								if (isSqlValueNotNull(value))
									method.invoke(target, Integer.parseInt(value));
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							} catch (InvocationTargetException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Integer.class, new AbstractSynchronousLazy<SetMethods>() {
					@Override
					protected final SetMethods create() {
						return (method, target, value) -> {
							try {
								method.invoke(target, isSqlValueNotNull(value) ? Integer.parseInt(value) : null);
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							} catch (InvocationTargetException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Long.TYPE, new AbstractSynchronousLazy<SetMethods>() {
					@Override
					protected final SetMethods create() {
						return (method, target, value) -> {
							try {
								if (isSqlValueNotNull(value))
									method.invoke(target, Long.parseLong(value));
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							} catch (InvocationTargetException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Long.class, new AbstractSynchronousLazy<SetMethods>() {
					@Override
					protected final SetMethods create() {
						return (method, target, value) -> {
							try {
								method.invoke(target, isSqlValueNotNull(value) ? Long.parseLong(value) : null);
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							} catch (InvocationTargetException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Float.TYPE, new AbstractSynchronousLazy<SetMethods>() {
					@Override
					protected final SetMethods create() {
						return (method, target, value) -> {
							try {
								if (isSqlValueNotNull(value))
									method.invoke(target, Float.parseFloat(value));
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							} catch (InvocationTargetException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Float.class, new AbstractSynchronousLazy<SetMethods>() {
					@Override
					protected final SetMethods create() {
						return (method, target, value) -> {
							try {
								method.invoke(target, isSqlValueNotNull(value) ? Float.parseFloat(value) : null);
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							} catch (InvocationTargetException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Double.TYPE, new AbstractSynchronousLazy<SetMethods>() {
					@Override
					protected final SetMethods create() {
						return (method, target, value) -> {
							try {
								if (isSqlValueNotNull(value))
									method.invoke(target, Double.parseDouble(value));
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							} catch (InvocationTargetException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Double.class, new AbstractSynchronousLazy<SetMethods>() {
					@Override
					protected final SetMethods create() {
						return (method, target, value) -> {
							try {
								method.invoke(target, isSqlValueNotNull(value) ? Double.parseDouble(value) : null);
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							} catch (InvocationTargetException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(String.class, new AbstractSynchronousLazy<SetMethods>() {
					@Override
					protected final SetMethods create() {
						return (method, target, value) -> {
							try {
								method.invoke(target, value);
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							} catch (InvocationTargetException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Enum.class, new AbstractSynchronousLazy<SetMethods>() {
					@Override
					protected final SetMethods create() {
						return (method, target, value) -> {
							try {
								if (isSqlValueNotNull(value))
									method.invoke(target, Enum.valueOf((Class<? extends Enum>) method.getParameterTypes()[0], value));
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							} catch (InvocationTargetException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				return newHashMap;
			}
		};

		private interface SetMethods {
			void setFromString(Method method, Object target, String value);
		}
	}

	private static boolean parseSqlBoolean(String booleanValue) {
		return Integer.parseInt(booleanValue) != 0;
	}

	private static boolean isSqlValueNotNull(String sqlValue) {
		return sqlValue != null && !"NULL".equals(sqlValue);
	}
}
