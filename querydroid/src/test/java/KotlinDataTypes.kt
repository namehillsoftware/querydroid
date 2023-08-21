data class KotlinDataTypes(
    var id: Int = 0,
    var integerColumn: Int = 0,
    var longColumn: Long = 0L,
    var floatColumn: Float = 0f,
    var doubleColumn: Double = 0.0,
    var booleanColumn: Boolean = false,
    var stringColumn: String? = null,
) {
    companion object {

        @JvmStatic
        private var special = 0
    }
}