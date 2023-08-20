package GivenAQuery.AndABooleanValue

import DataTypes
import DataTypesDatabaseHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.namehillsoftware.querydroid.SqLiteCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import tableName

@RunWith(AndroidJUnit4::class)
class WhenAddingATrueValueThroughObjectMethod {

    @Test
    fun `then the value is correct`() {
        DataTypesDatabaseHelper(ApplicationProvider.getApplicationContext()).use { databaseHelper ->
            databaseHelper.writableDatabase.use {
                val insert = SqLiteCommand(
                    it,
                    "INSERT INTO $tableName (`BooleanColumn`) VALUES (@BoolValue)"
                )
                insert.addParameter("BoolValue", true as Any?).execute()
            }

            databaseHelper.readableDatabase.use {
                val resultQuery = SqLiteCommand(
                    it,
                    "SELECT * FROM $tableName"
                )

                val dataTypesResult = resultQuery.fetchFirst(DataTypes::class.java)
                assertThat(dataTypesResult.booleanColumn).isTrue
            }
        }
    }
}