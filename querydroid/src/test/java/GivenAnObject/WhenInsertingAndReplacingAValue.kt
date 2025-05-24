package GivenAnObject

import DataTypesDatabaseHelper
import KotlinDataTypes
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.namehillsoftware.querydroid.SqLiteAssistants
import com.namehillsoftware.querydroid.SqLiteCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import tableName

@RunWith(AndroidJUnit4::class)
class WhenInsertingAndReplacingAValue {

    @Test
    fun `then the value is correct`() {
        DataTypesDatabaseHelper(ApplicationProvider.getApplicationContext()).use { databaseHelper ->
            val dataId = databaseHelper.writableDatabase.use {
                SqLiteAssistants.insertValue(
                    it,
                    tableName,
                    KotlinDataTypes(
                        booleanColumn = true,
                        integerColumn = 673,
                        longColumn = 76772878174L,
                        floatColumn = 222.18f,
                        doubleColumn = 733.72,
                        stringColumn = "winter"
                    )
                )

                val resultQuery = SqLiteCommand(
                    it,
                    "SELECT * FROM $tableName"
                )

                val insertedData = resultQuery.fetch(KotlinDataTypes::class.java).maxBy { d -> d.id }

                val insertBuilder = SqLiteAssistants.InsertBuilder
                    .fromTable(tableName)
                    .addColumn("id")
                    .addColumn("BooleanColumn")
                    .addColumn("IntegerColumn")
                    .addColumn("FloatColumn")
                    .addColumn("DoubleColumn")
                    .addColumn("StringColumn")
                    .withReplacement()

                SqLiteCommand(it, insertBuilder.buildQuery())
                    .addParameter("id", insertedData.id)
                    .addParameter("BooleanColumn", false)
                    .addParameter("IntegerColumn", 942)
                    .addParameter("FloatColumn", 232.47f)
                    .addParameter("DoubleColumn", 605.067)
                    .addParameter("StringColumn", "EE8PaSZm")
                    .execute()

                insertedData.id
            }

            databaseHelper.readableDatabase.use {
                val resultQuery = SqLiteCommand(
                    it,
                    "SELECT * FROM $tableName WHERE id = @id"
                ).addParameter("id", dataId)

                val dataTypesResult = resultQuery.fetchFirst(KotlinDataTypes::class.java)

                assertThat(dataTypesResult).isEqualTo(
                    KotlinDataTypes(
                        id = dataId,
                        booleanColumn = false,
                        integerColumn = 942,
                        longColumn = 0,
                        floatColumn = 232.47f,
                        doubleColumn = 605.067,
                        stringColumn = "EE8PaSZm",
                    )
                )
            }
        }
    }
}