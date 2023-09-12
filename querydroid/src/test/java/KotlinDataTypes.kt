data class KotlinDataTypes(
    var id: Int = 0,
    var integerColumn: Int = 0,
    var longColumn: Long = 0L,
    var floatColumn: Float = 0f,
    var doubleColumn: Double = 0.0,
    var booleanColumn: Boolean = false,
    var stringColumn: String? = null,
    var byteArrayColumn: ByteArray? = null,
) {
    companion object {

        @JvmStatic
        private var special = 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KotlinDataTypes

        if (id != other.id) return false
        if (integerColumn != other.integerColumn) return false
        if (longColumn != other.longColumn) return false
        if (floatColumn != other.floatColumn) return false
        if (doubleColumn != other.doubleColumn) return false
        if (booleanColumn != other.booleanColumn) return false
        if (stringColumn != other.stringColumn) return false
        if (byteArrayColumn != null) {
            if (other.byteArrayColumn == null) return false
            if (!byteArrayColumn.contentEquals(other.byteArrayColumn)) return false
        } else if (other.byteArrayColumn != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + integerColumn
        result = 31 * result + longColumn.hashCode()
        result = 31 * result + floatColumn.hashCode()
        result = 31 * result + doubleColumn.hashCode()
        result = 31 * result + booleanColumn.hashCode()
        result = 31 * result + (stringColumn?.hashCode() ?: 0)
        result = 31 * result + (byteArrayColumn?.contentHashCode() ?: 0)
        return result
    }
}