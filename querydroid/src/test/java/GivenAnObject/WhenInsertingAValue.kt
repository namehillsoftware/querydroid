package GivenAnObject

import DataTypesDatabaseHelper
import JavaDataTypes
import KotlinDataTypes
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.namehillsoftware.querydroid.SqLiteAssistants
import com.namehillsoftware.querydroid.SqLiteCommand
import org.assertj.core.api.Assertions.*
import org.junit.Test
import org.junit.runner.RunWith
import tableName

@RunWith(AndroidJUnit4::class)
class WhenInsertingAValue {

    @Test
    fun `then the value is correct`() {
        DataTypesDatabaseHelper(ApplicationProvider.getApplicationContext()).use { databaseHelper ->
            databaseHelper.writableDatabase.use {
                SqLiteAssistants.insertValue(
                    it,
                    tableName,
                    JavaDataTypes().apply {
                        booleanColumn = true
                        integerColumn = 841
                        longColumn = 76772878174L
                        floatColumn = 222.18f
                        doubleColumn = 745.00
                        stringColumn = "arrest"
                        byteArrayColumn = byteArrayOf(
                            (180 % 128).toByte(),
                            (368 % 128).toByte(),
                            (181 % 128).toByte(),
                        )
                    }
                )
            }

            databaseHelper.readableDatabase.use {
                val resultQuery = SqLiteCommand(
                    it,
                    "SELECT * FROM $tableName"
                )

                val dataTypesResult = resultQuery.fetchFirst(KotlinDataTypes::class.java)
                assertThat(dataTypesResult).isEqualTo(
                    KotlinDataTypes(
                        id = 1,
                        booleanColumn = true,
                        integerColumn = 841,
                        longColumn = 76772878174L,
                        floatColumn = 222.18f,
                        doubleColumn = 745.00,
                        stringColumn = "arrest",
                        byteArrayColumn = byteArrayOf(
                            (180 % 128).toByte(),
                            (368 % 128).toByte(),
                            (181 % 128).toByte(),
                        )
                    )
                )
            }
        }
    }
}