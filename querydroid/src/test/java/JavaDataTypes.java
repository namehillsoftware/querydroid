import java.util.Arrays;
import java.util.Objects;

public class JavaDataTypes {

    private static String $superPrivateStaticField = "worship";
    private int id;
    private int integerColumn = 0;
    private long  longColumn = 0L;
    private float floatColumn = 0f;
    private double doubleColumn = 0.0;
    private boolean booleanColumn = false;
    private String stringColumn = null;
    private byte[] byteArray;
    private TestEnum testEnumColumn;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIntegerColumn() {
        return integerColumn;
    }

    public void setIntegerColumn(int integerColumn) {
        this.integerColumn = integerColumn;
    }

    public long getLongColumn() {
        return longColumn;
    }

    public void setLongColumn(long longColumn) {
        this.longColumn = longColumn;
    }

    public float getFloatColumn() {
        return floatColumn;
    }

    public void setFloatColumn(float floatColumn) {
        this.floatColumn = floatColumn;
    }

    public double getDoubleColumn() {
        return doubleColumn;
    }

    public void setDoubleColumn(double doubleColumn) {
        this.doubleColumn = doubleColumn;
    }

    public boolean getBooleanColumn() {
        return booleanColumn;
    }

    public void setBooleanColumn(boolean booleanColumn) {
        this.booleanColumn = booleanColumn;
    }

    public String getStringColumn() {
        return stringColumn;
    }

    public void setStringColumn(String stringColumn) {
        this.stringColumn = stringColumn;
    }

    public byte[] getByteArrayColumn() {
        return byteArray;
    }

    public void setByteArrayColumn(byte[] byteArray) {
        this.byteArray = byteArray;
    }

    private int get$SecretData() {
        return -1;
    }

    public TestEnum getTestEnumColumn() {
        return testEnumColumn;
    }

    public void setTestEnumColumn(TestEnum testEnumColumn) {
        this.testEnumColumn = testEnumColumn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JavaDataTypes javaDataTypes = (JavaDataTypes) o;
        return id == javaDataTypes.id
                && integerColumn == javaDataTypes.integerColumn
                && longColumn == javaDataTypes.longColumn
                && Float.compare(javaDataTypes.floatColumn, floatColumn) == 0
                && Double.compare(javaDataTypes.doubleColumn, doubleColumn) == 0
                && booleanColumn == javaDataTypes.booleanColumn
                && stringColumn.equals(javaDataTypes.stringColumn)
                && testEnumColumn.equals(javaDataTypes.testEnumColumn)
                && Arrays.equals(byteArray, javaDataTypes.byteArray);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, integerColumn, longColumn, floatColumn, doubleColumn, booleanColumn, stringColumn, testEnumColumn, Arrays.hashCode(byteArray));
    }
}
