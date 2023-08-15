import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

const val tableName = "DataTypes"

class DataTypesDatabaseHelper(context: Context) : SQLiteOpenHelper(context, "test", null, 1) {
    override fun onCreate(sqLiteDatabase: SQLiteDatabase?) {
        sqLiteDatabase?.execSQL("""CREATE TABLE IF NOT EXISTS `$tableName` (
			`id` INTEGER PRIMARY KEY AUTOINCREMENT ,
			`IntegerColumn` INTEGER,
			`LongColumn` BIGINT,
			`BooleanColumn` SMALLINT ,
			`StringColumn` VARCHAR )""")
    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {
    }
}

class DataTypes {
    var id = 0
    var integerColumn = 0
    var longColumn = 0L
    var booleanColumn = false
    var stringColumn: String? = null
}