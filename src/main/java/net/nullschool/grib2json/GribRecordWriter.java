package net.nullschool.grib2json;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import ucar.grib.grib2.*;

import javax.json.stream.JsonGenerator;
import java.io.IOException;

import static ucar.grib.grib1.Grib1Tables.*;
import static ucar.grib.grib2.Grib2Tables.*;
import static ucar.grib.grib2.ParameterTable.*;
import static ucar.grib.GribNumbers.*;

/**
 * 2013-10-25<p/>
 *
 * Writes a Grib2 record to a JSON generator.
 *
 * This class was initially based on Grib2Dump, part of the netCDF-Java library written by University
 * Corporation for Atmospheric Research/Unidata. However, what appears below is a complete rewrite.
 *
 * @author Cameron Beccario
 */
final class GribRecordWriter extends AbstractRecordWriter {

    private final Grib2Record record;
    private final Grib2IndicatorSection ins;
    private final Grib2IdentificationSection ids;
    private final Grib2Pds pds;
    private final Grib2GDSVariables gds;

    GribRecordWriter(JsonGenerator jg, Grib2Record record, Options options) {
        super(jg, options);
        this.record = record;
        this.ins = record.getIs();
        this.ids = record.getId();
        this.pds = record.getPDS().getPdsVars();
        this.gds = record.getGDS().getGdsVars();
    }

    private boolean isSelected(String filterParameter) {
        try {
            return
                filterParameter == null ||
                "wind".equals(filterParameter) && (pds.getParameterNumber() == 2 || pds.getParameterNumber() == 3) ||
                Integer.parseInt(filterParameter) == pds.getParameterNumber();
        }
        catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Return true if the specified command line options do not filter out this record.
     */
    boolean isSelected() {
        return
            (options.getFilterDiscipline() == null || options.getFilterDiscipline() == ins.getDiscipline()) &&
            (options.getFilterCategory() == null || options.getFilterCategory() == pds.getParameterCategory()) &&
            (options.getFilterSurface() == null  || options.getFilterSurface() == pds.getLevelType1()) &&
            (options.getFilterValue() == null    || options.getFilterValue() == pds.getLevelValue1()) &&
            isSelected(options.getFilterParameter());
    }

    /**
     * Write contents of the record's indicator section.
     */
    private void writeIndicator() {
        write("discipline", ins.getDiscipline(), ins.getDisciplineName());
        write("gribEdition", ins.getGribEdition());
        write("gribLength", ins.getGribLength());
    }

    /**
     * Write contents of the record's identification section.
     */
    private void writeIdentification() {
        write("center", ids.getCenter_id(), getCenter_idName(ids.getCenter_id()));
        write("subcenter", ids.getSubcenter_id());
        write("refTime", new DateTime(ids.getRefTime()).withZone(DateTimeZone.UTC).toString());
        write("significanceOfRT", ids.getSignificanceOfRT(), ids.getSignificanceOfRTName());
        write("productStatus", ids.getProductStatus(), ids.getProductStatusName());
        write("productType", ids.getProductType(), ids.getProductTypeName());
    }

    /**
     * Write contents of the record's product section.
     */
    private void writeProduct() {
        final int productDef = pds.getProductDefinitionTemplate();
        final int discipline = ins.getDiscipline();
        final int paramCategory = pds.getParameterCategory();
        final int paramNumber = pds.getParameterNumber();

        write("productDefinitionTemplate", productDef, codeTable4_0(productDef));
        write("parameterCategory", paramCategory, getCategoryName(discipline, paramCategory));
        write("parameterNumber", paramNumber, getParameterName(discipline, paramCategory, paramNumber));
        write("parameterUnit", getParameterUnit(discipline, paramCategory, paramNumber));
        write("genProcessType", pds.getGenProcessType(), codeTable4_3(pds.getGenProcessType()));
        write("forecastTime", pds.getForecastTime());
        write("surface1Type", pds.getLevelType1(), codeTable4_5(pds.getLevelType1()));
        write("surface1Value", pds.getLevelValue1());
        write("surface2Type", pds.getLevelType2(), codeTable4_5(pds.getLevelType2()));
        write("surface2Value", pds.getLevelValue2());
    }

    private void writeGridShape() {
        // See http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table3-2.shtml
        write("shape", gds.getShape(), codeTable3_2(gds.getShape()));
        switch (gds.getShape()) {
            case 1:  // Earth assumed spherical with radius specified (in m) by data producer
                write("earthRadius", gds.getEarthRadius());
                break;
            case 3:  // Earth assumed oblate spheroid with major and minor axes specified (in km) by data producer
                write("majorAxis", gds.getMajorAxis());
                write("minorAxis", gds.getMinorAxis());
                break;
        }
    }

    private void writeGridSize() {
        write("gridUnits", gds.getGridUnits());
        write("resolution", gds.getResolution());
        write("winds", isBitSet(gds.getResolution(), BIT_5) ? "relative" : "true");
        write("scanMode", gds.getScanMode());
        write("nx", gds.getNx());  // Number of points on x-axis or parallel
        write("ny", gds.getNy());  // Number of points on y-axis or meridian
    }

    private void writeLonLatBounds() {
        writeIfSet("lo1", gds.getLo1());  // longitude of first grid point
        writeIfSet("la1", gds.getLa1());  // latitude of first grid point
        writeIfSet("lo2", gds.getLo2());  // longitude of last grid point
        writeIfSet("la2", gds.getLa2());  // latitude of last grid point
        writeIfSet("dx", gds.getDx());    // i direction increment
        writeIfSet("dy", gds.getDy());    // j direction increment
    }

    private void writeRotationAndStretch() {
        writeIfSet("spLon", gds.getSpLon());  // longitude of the southern pole of projection
        writeIfSet("spLat", gds.getSpLat());  // latitude of the southern pole of projection
        writeIfSet("rotationAngle", gds.getRotationAngle());
        writeIfSet("poleLon", gds.getPoleLon());  // longitude of the pole stretching
        writeIfSet("poleLat", gds.getPoleLat());  // latitude of the pole of stretching
        writeIfSet("stretchingFactor", gds.getStretchingFactor());
    }

    private void writeAngle() {
        writeIfSet("angle", gds.getAngle());  // orientation of the grid
        writeIfSet("basicAngle", gds.getBasicAngle());
        writeIfSet("subDivisions", gds.getSubDivisions());
    }

    private void writeLonLatGrid() {
        writeGridShape();
        writeGridSize();
        writeAngle();
        writeLonLatBounds();
        writeRotationAndStretch();
        writeIfSet("np", gds.getNp());  // number of paralells between a pole and the equator
    }

    private void writeMercatorGrid() {
        writeGridShape();
        writeGridSize();
        writeAngle();
        writeLonLatBounds();
    }

    private void writePolarStereographicGrid() {
        writeGridShape();
        writeGridSize();
        writeLonLatBounds();
    }

    private void writeLambertConformalGrid() {
        writeGridShape();
        writeGridSize();
        writeLonLatBounds();
        writeRotationAndStretch();

        write("laD", gds.getLaD());
        write("loV", gds.getLoV());
        write("projectionFlag", gds.getProjectionFlag());
        write("latin1", gds.getLatin1());  // first latitude from the pole at which the secant cone cuts the sphere
        write("latin2", gds.getLatin2());  // second latitude from the pole at which the secant cone cuts the sphere
    }

    private void writeSpaceOrOrthographicGrid() {
        writeGridShape();
        writeGridSize();
        writeAngle();
        writeLonLatBounds();

        write("lop", gds.getLop());  // longitude of sub-satellite point
        write("lap", gds.getLap());  // latitude of sub-satellite point
        write("xp", gds.getXp());    // x-coordinate of sub-satellite point
        write("yp", gds.getYp());    // y-coordinate of sub-satellite point
        write("nr", gds.getNr());    // altitude of the camera from the Earth's center
        write("xo", gds.getXo());    // x-coordinate of origin of sector image
        write("yo", gds.getYo());    // y-coordinate of origin of sector image
    }

    private void writeCurvilinearGrid() {
        writeGridShape();
        writeGridSize();
    }

    /**
     * Write contents of the record's grid definition section.
     * See http://www.nco.ncep.noaa.gov/pmb/docs/grib2/grib2_table3-1.shtml
     */
    private void writeGridDefinition() {
        final int gridTemplate = gds.getGdtn();

        write("gridDefinitionTemplate", gridTemplate, codeTable3_1(gridTemplate));
        write("numberPoints", gds.getNumberPoints());

        switch (gridTemplate) {
            case 0:  // Template 3.0
            case 1:  // Template 3.1
            case 2:  // Template 3.2
            case 3:  // Template 3.3
                writeLonLatGrid();
                break;
            case 10:  // Template 3.10
                writeMercatorGrid();
                break;
            case 20:  // Template 3.20
                writePolarStereographicGrid();
                break;
            case 30:  // Template 3.30
                writeLambertConformalGrid();
                break;
            case 40:  // Template 3.40
            case 41:  // Template 3.41
            case 42:  // Template 3.42
            case 43:  // Template 3.43
                writeLonLatGrid();
                break;
            case 90:  // Template 3.90
                writeSpaceOrOrthographicGrid();
                break;
            case 204:  // Template 3.204
                writeCurvilinearGrid();
                break;
        }
    }

    /**
     * Write the record's header as a Json object: "header": { ... }
     */
    void writeHeader() {
        jg.writeStartObject("header");
        writeIndicator();
        writeIdentification();
        writeProduct();
        writeGridDefinition();
        jg.writeEnd();
    }

    /**
     * Write the record's data as a Json array: "data": [ ... ]
     */
    void writeData(Grib2Data gd) throws IOException {
        float[] data = gd.getData(record.getGdsOffset(), record.getPdsOffset(), ids.getRefTime());
        if (data != null) {
            jg.writeStartArray("data");
            for (float value : data) {
                jg.write(new FloatValue(value));
            }
            jg.writeEnd();
        }
    }
}
