package net.nullschool.grib2json;

import javax.json.JsonNumber;
import java.math.BigDecimal;
import java.math.BigInteger;


/**
 * 2013-10-24<p/>
 *
 * A Json float value. This class uses Float.toString to produce the Json text for a float. This avoids the
 * noise caused by the default JsonGenerator when it widens to double.
 *
 * This class also defines the Json representations for NaN, Infinity, and -Infinity to be their equivalent
 * String representations. For example: [1.0, 2.3, "NaN", 0.7, "Infinity"]
 *
 * @author Cameron Beccario
 */
final class FloatValue implements JsonNumber {

    private final float value;
    private BigDecimal bd;

    FloatValue(float value) {
        this.value = value;
    }

    @Override public ValueType getValueType() {
        return ValueType.NUMBER;
    }

    @Override public String toString() {
        if (Float.isNaN(value)) {
            return "\"NaN\"";
        }
        else if (value == Float.POSITIVE_INFINITY) {
            return "\"-Infinity\"";
        }
        else if (value == Float.NEGATIVE_INFINITY) {
            return "\"Infinity\"";
        }
        else {
            return Float.toString(value);
        }
    }

    @Override public boolean isIntegral() {
        return bigDecimalValue().scale() == 0;
    }

    @Override public int intValue() {
        return (int)value;
    }

    @Override public int intValueExact() {
        return bigDecimalValue().intValueExact();
    }

    @Override public long longValue() {
        return (long)value;
    }

    @Override public long longValueExact() {
        return bigDecimalValue().longValueExact();
    }

    @Override public BigInteger bigIntegerValue() {
        return bigDecimalValue().toBigInteger();
    }

    @Override public BigInteger bigIntegerValueExact() {
        return bigDecimalValue().toBigIntegerExact();
    }

    @Override public double doubleValue() {
        return (double)value;
    }

    @Override public BigDecimal bigDecimalValue() {
        return bd != null ? bd : (bd = new BigDecimal(value));
    }

    @Override public boolean equals(Object that) {
        return that instanceof JsonNumber && this.bigDecimalValue().equals(((JsonNumber)that).bigDecimalValue());
    }

    @Override public int hashCode() {
        return bigDecimalValue().hashCode();
    }
}
