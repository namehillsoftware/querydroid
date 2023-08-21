package GivenAnObject

import KotlinDataTypes
import DataTypesDatabaseHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.namehillsoftware.querydroid.SqLiteAssistants
import com.namehillsoftware.querydroid.SqLiteCommand
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import tableName

@RunWith(AndroidJUnit4::class)
class WhenInsertingAndUpdatingAValue {

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
                val dataUpdate = insertedData.copy(
                    floatColumn = 370.03f,
                    stringColumn = null,
                    integerColumn = 275,
                    booleanColumn = false,
                )

                SqLiteAssistants.updateValue(it, tableName, dataUpdate)
                dataUpdate.id
            }

            databaseHelper.readableDatabase.use {
                val resultQuery = SqLiteCommand(
                    it,
                    "SELECT * FROM $tableName WHERE id = @id"
                ).addParameter("id", dataId)

                val dataTypesResult = resultQuery.fetchFirst(KotlinDataTypes::class.java)

                Assertions.assertThat(dataTypesResult).isEqualTo(
                    KotlinDataTypes(
                        id = dataId,
                        booleanColumn = false,
                        integerColumn = 275,
                        longColumn = 76772878174L,
                        floatColumn = 370.03f,
                        doubleColumn = 733.72,
                        stringColumn = null
                    )
                )
            }
        }
    }
}