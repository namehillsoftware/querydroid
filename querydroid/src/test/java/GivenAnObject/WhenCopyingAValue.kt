package GivenAnObject

import DataTypesDatabaseHelper
import JavaDataTypes
import KotlinDataTypes
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.namehillsoftware.querydroid.SqLiteAssistants
import com.namehillsoftware.querydroid.SqLiteCommand
import copyTableName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import tableName

@RunWith(AndroidJUnit4::class)
class WhenCopyingAValue {

    data class ReducedDataTypes(
        var id: Int = 0,
        var integerColumn: Int = 0,
        var longColumn: Long = 0L,
        var floatColumn: Float = 0f,
        var doubleColumn: Double = 0.0,
        var booleanColumn: Boolean = false,
    )

    @Test
    fun `then the value is correct`() {
        DataTypesDatabaseHelper(ApplicationProvider.getApplicationContext()).use { databaseHelper ->
            databaseHelper.writableDatabase.use {
                SqLiteAssistants.insertValue(
                    it,
                    tableName,
                    ReducedDataTypes(
                        booleanColumn = false,
                        integerColumn = 993,
                        longColumn = 636979907L,
                        floatColumn = 77.25f,
                        doubleColumn = 551.44,
                    )
                )

                val dataTypesResult = SqLiteCommand(it, "SELECT * FROM $tableName").fetchFirst(JavaDataTypes::class.java)
                dataTypesResult.stringColumn = "resist"

                SqLiteAssistants.insertValue(
                    it,
                    copyTableName,
                    dataTypesResult
                )
            }

            databaseHelper.readableDatabase.use {
                val resultQuery = SqLiteCommand(
                    it,
                    "SELECT * FROM $copyTableName"
                )

                val dataTypesResult = resultQuery.fetchFirst(KotlinDataTypes::class.java)
                assertThat(dataTypesResult).isEqualTo(
                    KotlinDataTypes(
                        id = 1,
                        booleanColumn = false,
                        integerColumn = 993,
                        longColumn = 636979907L,
                        floatColumn = 77.25f,
                        doubleColumn = 551.44,
                        stringColumn = "resist",
                    )
                )
            }
        }
    }
}