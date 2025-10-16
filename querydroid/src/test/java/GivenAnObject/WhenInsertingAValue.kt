package GivenAnObject

import DataTypesDatabaseHelper
import JavaDataTypes
import KotlinDataTypes
import TestEnum
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.namehillsoftware.querydroid.SqLiteAssistants
import com.namehillsoftware.querydroid.SqLiteCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import tableName
import kotlin.use

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
                        testEnumColumn = TestEnum.OPTION_TWO
                        byteArrayColumn = byteArrayOf(
                            (180 % 128).toByte(),
                            (368 % 128).toByte(),
                            (181 % 128).toByte(),
                        )
                    }
                )
            }

            val dataTypesResult = databaseHelper.readableDatabase.use {
                val resultQuery = SqLiteCommand(
                    it,
                    "SELECT * FROM $tableName"
                )

                resultQuery.fetchFirst(KotlinDataTypes::class.java)
            }

            val stringResult = databaseHelper.readableDatabase.use {
                val resultQuery = SqLiteCommand(
                    it,
                    "SELECT stringColumn FROM $tableName"
                )

                resultQuery.fetchFirst(String::class.java)
            }

            val booleanResult = databaseHelper.readableDatabase.use {
                val resultQuery = SqLiteCommand(
                    it,
                    "SELECT booleanColumn FROM $tableName"
                )

                resultQuery.fetchFirst(Boolean::class.java)
            }

            val integerResult = databaseHelper.readableDatabase.use {
                val resultQuery = SqLiteCommand(
                    it,
                    "SELECT integerColumn FROM $tableName"
                )

                resultQuery.fetchFirst(Integer::class.java)
            }

            val longResult = databaseHelper.readableDatabase.use {
                val resultQuery = SqLiteCommand(
                    it,
                    "SELECT longColumn FROM $tableName"
                )

                resultQuery.fetchFirst(Long::class.java)
            }

            val floatResult = databaseHelper.readableDatabase.use {
                val resultQuery = SqLiteCommand(
                    it,
                    "SELECT floatColumn FROM $tableName"
                )

                resultQuery.fetchFirst(Float::class.java)
            }

            val doubleResult = databaseHelper.readableDatabase.use {
                val resultQuery = SqLiteCommand(
                    it,
                    "SELECT doubleColumn FROM $tableName"
                )

                resultQuery.fetchFirst(Double::class.java)
            }

            val enumResult = databaseHelper.readableDatabase.use {
                val resultQuery = SqLiteCommand(
                    it,
                    "SELECT testEnumColumn FROM $tableName"
                )

                resultQuery.fetchFirst<TestEnum>(TestEnum::class.java)
            }

            val blobResult = databaseHelper.readableDatabase.use {
                val resultQuery = SqLiteCommand(
                    it,
                    "SELECT byteArrayColumn FROM $tableName"
                )

                resultQuery.fetchFirst(ByteArray::class.java)
            }

            assertThat(dataTypesResult).isEqualTo(
                KotlinDataTypes(
                    id = 1,
                    booleanColumn = true,
                    integerColumn = 841,
                    longColumn = 76772878174L,
                    floatColumn = 222.18f,
                    doubleColumn = 745.00,
                    stringColumn = "arrest",
                    testEnumColumn = TestEnum.OPTION_TWO,
                    byteArrayColumn = byteArrayOf(
                        (180 % 128).toByte(),
                        (368 % 128).toByte(),
                        (181 % 128).toByte(),
                    )
                )
            )

            assertThat(stringResult).isEqualTo(dataTypesResult.stringColumn)
            assertThat(booleanResult).isEqualTo(dataTypesResult.booleanColumn)
            assertThat(integerResult).isEqualTo(dataTypesResult.integerColumn)
            assertThat(longResult).isEqualTo(dataTypesResult.longColumn)
            assertThat(floatResult).isEqualTo(dataTypesResult.floatColumn)
            assertThat(doubleResult).isEqualTo(dataTypesResult.doubleColumn)
            assertThat(enumResult).isEqualTo(dataTypesResult.testEnumColumn)
            assertThat(blobResult).isEqualTo(dataTypesResult.byteArrayColumn)
        }
    }
}