# QueryDroid

A simple library that helps with serializing Android SQLite queries to and from Java objects:

```java
public Collection<Library> getLibraries(Context context) {
  try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
      return repositoryAccessHelper
          .mapSql("SELECT * FROM library")
          .fetch(Library.class);
  }
}
```

What problems does it solve? 

The standard Android library has for ages had numerous ways to pull back data from its built-in SQLite database. However, none of these ways either a) are easy to use, or b) give developers the power they need to develop performant applications.

In the C# world, developers have long had the excellent [Dapper](https://github.com/StackExchange/dapper-dot-net) library, which maps C# types nicely to and from C# objects without getting in the way of how you want to write your queries. QueryDroid started off with Dapper as an inspiration and made querying your data in the built-in SQLite library much more natural and cleaner!

# Installation

Currently, QueryDroid is available on Maven Central.

```groovy
dependencies {
    implementation 'com.namehillsoftware:querydroid:0.4.0'
}
```


# Usage

QueryDroid will handle serializing any objects with either public fields or with getter's and setters. For example, the class used in the above example could look like this:

## Fetching

QueryDroid is very fetching, and will always be when fetching you data. To fetch, pass the type you wish to fetch into the `fetch` method, as above:

```java
public Collection<Library> getLibraries(Context context) {
  try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
      return repositoryAccessHelper
          .mapSql("SELECT * FROM library")
          .fetch(Library.class);
  }
}
```

`fetch` will return a collection of `Library` objects. If you know you'll only need one `Library`, then you can use `fetchFirst`. Parameters can also be added to your query. These can be any of the primitive types listed above. For example:

```java
public Library getLibrary(Context context) {
    try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
        return repositoryAccessHelper
            .mapSql("SELECT * FROM library WHERE id = @myKnownId")\
            .addParameter("myKnownId", 14)
            .fetchFirst(Library.class);
    }
}
```

The `in` filter clause is not yet supported, and neither are fetching primitive types.

### Type mappings

```java
public class Library {

    private int id;
    private String libraryName;
    private String accessCode;
    private String authKey;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLibraryName() {
        return libraryName;
    }

    public void setLibraryName(String libraryName) {
        this.libraryName = libraryName;
    }

    public String getAccessCode() {
        return accessCode;
    }

    public void setAccessCode(String accessCode) {
        this.accessCode = accessCode;
    }

    public String getAuthKey() {
        return authKey;
    }

    public void setAuthKey(String authKey) {
        this.authKey = authKey;
    }
}
```

And QueryDroid will gladly serialize this class, following some simple serialization rules. The rules are:

1. Fields that you wish to have serialized/deserialized must be `public`, and they **must** match the name of the column you wish to deserialize
2. Getter/Setter methods that you wish to use instead for serialization/deserialization must be `public`, and they **must** follow these rules:

    1. Getters must be named either `getFieldName()`, or `isFieldTrue()` for `boolean` fields
    2. Setters must be named `setFieldName(Fieldtype value)`, including `boolean` fields, e.g. `setIsFieldTrue(boolean value)`
  
3. Either fields or the getters/setters can basically be the primitive Java types:

    1. `int`/`Integer`
    2. `long`/`Long`
    3. `boolean`/`Boolean`
    4. `short`/`Short`
    5. `double`/`Double`
    6. `String`
    7. `Enum`
      `Enum` fields, as always, deserve special mention: `Enum` fields are serialized to/from `String` fields.

## Commands

Inserts/Updates will also work using the above example, the main difference being you call the `execute()` method:

```java
public long getLibrary(Context context) {
    try (RepositoryAccessHelper repositoryAccessHelper = new RepositoryAccessHelper(context)) {
        return repositoryAccessHelper
            .mapSql("INSERT INTO library (libraryName, accessCode, authKey) " +
                    "VALUES (@libraryName, @accessCode, @authKey)")
            .addParameter("libraryName", "New Library!")
            .addParameter("accessCode", "secret")
            .addParameter("authKey", "123@!")
            .execute();
    }
}
```

`execute()` will return a long reflecting the number of affected rows.

There are also some helpers that make inserting and updating a breeze. For example, take this Kotlin class (it can be a Java POJO as well):

```kt
data class JavaDataTypes(
    var id: Int = 0,
    var integerColumn: Int = 0,
    var longColumn: Long = 0L,
    var floatColumn: Float = 0f,
    var doubleColumn: Double = 0.0,
    var booleanColumn: Boolean = false,
    var stringColumn: String? = null,
)
```

Inserting a new value is as simple as making a call like this:

```kt
SqLiteAssistants.insertValue(
  it,
  tableName,
  JavaDataTypes(
      booleanColumn = true,
      integerColumn = 673,
      longColumn = 76772878174L,
      floatColumn = 222.18f,
      doubleColumn = 733.72,
      stringColumn = "winter"
  )
)
```

And updating is similar:

```kt
 val dataUpdate = insertedData.copy(
   floatColumn = 370.03f,
   stringColumn = null,
   integerColumn = 275,
   booleanColumn = false,
)

SqLiteAssistants.updateValue(it, tableName, dataUpdate)
```

See a full example in the [tests](./querydroid/src/test/java/GivenAnObject/WhenInsertingAndUpdatingAValue.kt).