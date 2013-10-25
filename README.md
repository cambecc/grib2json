grib2json
=========

A command line utility that decodes [GRIB2](http://en.wikipedia.org/wiki/GRIB) files as JSON.

This utility uses the netCDF-Java GRIB decoder, part of the [THREDDS](https://github.com/Unidata/thredds) project.

Installation
------------

```
git clone <this project>
mvn package
```

This creates a .tar.gz in the target directory. Unzip and untar the package in a location of choice.

Usage
-----

The `grib2json` launch script is located in the `bin` directory and requires the `JAVA_HOME` environment
variable to be defined.

```
> grib2json --help
Usage: grib2json [options] FILE
	[--compact -c] : enable compact Json formatting
	[--data -d] : print GRIB record data
	[--filter.category --fc value] : select records with this numeric category
	[--filter.parameter --fp value] : select records with this numeric parameter
	[--filter.surface --fs value] : select records with this numeric surface type
	[--filter.value --fv value] : select records with this numeric surface value
	[--help -h] : display this help
	[--names -n] : print names of numeric codes
	[--output -o value] : write output to the specified file (default is stdout)
	[--verbose -v] : enable logging to stdout
```

For example, the following command outputs the records for parameter 2 (U-component_of_wind), with
surface type 103 (Specified height level above ground), and surface value 10.0 meters:

```
> grib2json --fp 2 --fs 103 --fv 10.0 --names gfs.t18z.pgrbf00.2p5deg.grib2

[
    {
        "header":{
            "discipline":0,
            "disciplineName":"Meteorological products",
            "gribEdition":2,
            "gribLength":27759,
            "center":7,
            "centerName":"US National Weather Service - NCEP(WMC)",
            "subcenter":0,
            ...
        }
    }
]
```
