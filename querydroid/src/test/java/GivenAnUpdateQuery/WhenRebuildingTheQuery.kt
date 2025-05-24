package GivenAnUpdateQuery

import com.namehillsoftware.querydroid.SqLiteAssistants
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WhenRebuildingTheQuery {

    @Test
    fun `then the query is correct`() {
        val updateQueryBuilder = SqLiteAssistants.UpdateBuilder.fromTable("ANYTHING")
            .addSetter("9OAPmpysH4N")
            .addSetter("V30y8VlAsOv")
            .addSetter("xEtwtOtck")
            .setFilter("WHERE UBGK4K2C = 327")

        val firstQuery = updateQueryBuilder.buildQuery()

        updateQueryBuilder
            .addSetter("kAk34bARlin")
            .setFilter("WHERE MxOdSfDWAB7 IN (388)")

        val secondQuery = updateQueryBuilder.buildQuery()

        assertThat(firstQuery).isEqualTo("UPDATE ANYTHING SET 9OAPmpysH4N = @9OAPmpysH4N, V30y8VlAsOv = @V30y8VlAsOv, xEtwtOtck = @xEtwtOtck WHERE UBGK4K2C = 327")
        assertThat(secondQuery).isEqualTo("UPDATE ANYTHING SET 9OAPmpysH4N = @9OAPmpysH4N, V30y8VlAsOv = @V30y8VlAsOv, xEtwtOtck = @xEtwtOtck, kAk34bARlin = @kAk34bARlin WHERE MxOdSfDWAB7 IN (388)")
    }
}