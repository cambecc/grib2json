package net.nullschool.grib2json;

import javax.json.stream.JsonGenerator;

import java.util.Objects;

import static ucar.grib.GribNumbers.UNDEFINED;


/**
 * 2014-01-15<p/>
 *
 * @author Cameron Beccario
 */
abstract class AbstractRecordWriter {

    protected final JsonGenerator jg;
    protected final Options options;

    protected AbstractRecordWriter(JsonGenerator jg, Options options) {
        this.jg = Objects.requireNonNull(jg);
        this.options = Objects.requireNonNull(options);
    }

    /**
     * Write a "key":int Json pair.
     */
    protected void write(String key, int value) {
        jg.write(key, value);
    }

    /**
     * Write a "key":int Json pair only if the value is not {@link ucar.grib.GribNumbers#UNDEFINED}.
     */
    protected void writeIfSet(String key, int value) {
        if (value != UNDEFINED) {
            jg.write(key, value);
        }
    }

    /**
     * Write a "key":long Json pair.
     */
    protected void write(String key, long value) {
        jg.write(key, value);
    }

    /**
     * Write a "key":float Json pair.
     */
    protected void write(String key, float value) {
        jg.write(key, new FloatValue(value));
    }

    /**
     * Write a "key":float Json pair only if the value is not {@link ucar.grib.GribNumbers#UNDEFINED}.
     */
    protected void writeIfSet(String key, float value) {
        if (value != UNDEFINED) {
            jg.write(key, new FloatValue(value));
        }
    }

    /**
     * Write a "key":double Json pair.
     */
    protected void write(String key, double value) {
        jg.write(key, value);
    }

    /**
     * Write a "key":"value" Json pair.
     */
    protected void write(String key, String value) {
        jg.write(key, value);
    }

    /**
     * Write a "key":"value" Json pair, and a second "keyName":"name" pair if the command line options
     * have name printing enabled.
     */
    protected void write(String key, int code, String name) {
        write(key, code);
        if (options.getPrintNames()) {
            write(key + "Name", name);
        }
    }
}
