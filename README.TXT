/************************************************************** README *******************************************************************************/

TOC ***************************
                            *** 
- Summary                   ***
- Programmer's Guide        ***
    - Notes                 ***
    - Roadmap               ***    
- User's Guide              ***
    - Command line input    ***
    - Configuration file    ***    
                            *** 
*******************************

Summary //////////////////////////////////////////////////

The "Cluster Explorer" is a standalone Java application, developed to ease the exploration of city point data. Although the use case concerned a set of geolocated Tweets, the methods we used could be extended to any kind of timestamped, geospatial, point data.
To summarize the dataset, we used two cluster algorithms: DBSCAN and OPTICS; the first one generates a flat cluster partition, while the second one generates an hierarchical cluster partition. Due to their different characteristics, they could be applied in different contexts, or even combined to provide a richer analysis. The final results are displayed in a 3D globe, containing geographic datasets (e.g.: Blue Marble, OSM, Bing Maps), which provides context information and ease the interpretation of the results.
This application relies only in FOSS. The machine learning algorithms where provided by the Elki Data Mining Framework [1] and the virtual globe was provided by Nasa WorldWind [2], which uses OpenGL to provide hardware-accelerated rendering.

A public code repository is available, on this url: https://github.com/doublebyte1/ClusterExplorer.git

Programmer's Guide ///////////////////////////////////////

Notes:

- This project was developed using Maven. All information regarding dependencies is resolved in the pom.xml.
- The project is composed of four main blocks: core, data, geo and model. "Data" contains the database connector, and "model" implements the data model, which in this case represents a tweet. The block "geo" contains everything related to spatial intelligence and data structures. Finally, "core" contains the main class of the program, which reads the information from the MongoDB database, calculates the clusters, and instantiates a virtual globe, where it displays the final results.
- Each class/function has a corresponding test (TDD).
- The input of this application are the command line parameters, parsed in a function inside "core", and a properties file (mongodb.properties), whose path is read from an environmental variable ($MONGODB_PATH).
- The output of this application is a virtual globe, on which the resulting polygons (clusters) are displayed.
- This application was developed, using the latest version of the Elki library which was available during the development period (0.6.0). This version is known to have some issues:
    - The clusters generated using the OPTICS algorithm, show some "spike" effects; these were removed through additional coding, aimed at post-processing the results.
    - There are some problems (bugs) regarding the cluster calculations, when dealing with large arrays.
   Both these issues are known to be solved in the latest Elki version (0.7.0.). It is not 100% clear if *the new version is backwards compatible, and therefore The upgrade may require some code changes, in order to accomodate the new library*.

Roadmap:

- upgrade Elki to its latest version (0.7.0)
- Extend and Generalize Input: implement the ability to read points from a Shapefile.
- Implement Output Persistence: implement the ability to write the output (clusters) to a file, on a standard OGC format (e.g.: GeoJSON, Shapefile, KML).


User's Guide /////////////////////////////////////////////

The "Cluster Explorer" application generates a Java file, which can be used as stand-alone. Alternatively, the executable can be called with Maven.
This application requires two types of inputs: command line arguments, and a configuration file.

Command line Input:

To view a help menu regarding the command line arguments, run the program without arguments, or using the "-h" flag. The output is similar to this:

 -a,--alg <arg>       cluster algorithm to be used; possible values are
                      "optics", "dbscan" (default) and "all"(both)
 -e,--epsilon <arg>   neighbourhood radius (DBSCAN) and maximum
                      neighbourhood radius (OPTICS); e.g.: 500 (default);
 -g,--guess           suggests parameters for your dataset, based on an
                      informated guess;
 -h,--help            display this help message.
 -m,--minpts <arg>    number of points required to form a cluster; e.g.:
                      100 (default);
 -n,--end <arg>       end date; e.g.: "05/06/2014"
 -s,--start <arg>     start date; e.g.: "01/06/2014"
 -x,--xi <arg>        OPTICSxi contrast parameter: establishes the
                      relative decrease in density; e.g.: 0.03 (default);

The neighbourhood radius (--epsilon) and the number of points required to form a cluster (--minpts) are required arguments. If you have no idea what to put here, you can call the program using the "--guess" flag, and it will suggest you some values for these parameters, that can work as a starting point. The start and end date (-s,-n) are also required, in the format 'dd/mm/yyyy'. All other arguments (e.g.: algorithm, xi) are optional, as they have default values. *If you don't explicitly pass an argument for these parameters, be sure to familiarize yourself first with the default values*.                       

In the case of the clustering algorithm, the default value is "dbscan". Apart from "optics", we also support using the two clustering algorithms at a time, using the flag "all". In that case, the resulting clusters would be overlayed, using different colors, in the virtual globe.

This is an example of a command line input:

mvn -X  exec:java -Dexec.args="-s='01/01/2004' -n='31/12/2015' -e=1000 -xi=0.035 -minpts=10"

Configuration File:

Apart from the command line parameters, the "Cluster Explorer" requires the use of a configuration file, containing the connection parameters with MongoDB database. This file is called mongodb.properties, and its path is stored in the environmental variable "MONGODB_PATH"; If this variable was not set, the application would look for this file on the $HOME user folder.
This is the format of the "mongodb.properties" file. Each line contains the variable name and value, separated by a comma. We *do support* authenticated connections to the mongoDB database. 

dbUrl = [url of the database]
dbPort = [port of the database]
dbName = [database name]
tweetsCollectionName = [collection name]
locationField = [name of the geolocation field]
latField = [name of the latitude field]
lonField = [name of the longitude field]
dbusername = [database username]
dbpasswd = [database password]

The geolocation field, should have this exact structure:

    [geoLocation field] : {
        [longitude field] : [double],
        [latitude field] : [double]
    },

, where a parent geolocation field, contains both latitude and longitude.

[1] http://worldwind.arc.nasa.gov/java/
[2] http://elki.dbs.ifi.lmu.de/