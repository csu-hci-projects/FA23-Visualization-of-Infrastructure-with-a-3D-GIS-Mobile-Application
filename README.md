# FA23-Visualization-of-Infrastructure-with-a-3D-GIS-Mobile-Application
Rene Martinez, Audrey Dorin and Lindsey Nielsen

## A short description of your application
We created an Augmented Reality Geographical Information System application for utility workers in infastructure to help with time, costs and stress. This app lets these workers locate buried assets like pipes in a AR mobile application. We researched, developed and tested our app to create the best application we could make. We used a a GeoServer to manage the assets for the android application and used ARCore and Blender for the Augmented Reality aspect of the mobile app. We then tested our app on 8 participants including 4 GIS professionals with a series of surveys and semi-structure interviews to get feedback and direction of where to go next with this app. Overall the app was well recieved and we think it could really make a difference in utility workers day to day life on the job. We hope you enjoy!

## Hardware/Software Requirements and Instructions

Linux Environment:  
RAM: 16 GB  
Ubuntu 20.04.6  
GeoServer - Latest  
PostgreSQL - Latest  
PostGIS - Latest  
pgAdmin - Latest  
QGIS Desktop - Latest  
Apache2 2.4.41 or later (Reverse Proxy)  

1. Follow the publicly available steps in installing PostgreSQL.  
2. Follow the available steps to install the PostGIS Extension (easiest through pgAdmin).  
3. Create a database and enable spatial support with PostGIS.  
4. Follow the publicly available steps to install GeoServer.  
5. Using QGIS Desktop, create a database connection and follow the steps in creating a table.  
6. Configure a database connection to PostGres to GeoServer.  
7. Publish the WFS Layer using the target database in PostGres.  
8. Install Apache2 and follow the steps required to configure a reverse proxy from port 80 (public) to port 8080 (private).  
9. In your modem's networking settings, forward port 80.  
10. If you can reach your GeoServer instance you are in good shape! Otherwise there is likely a networking problem with port forwarding.  
ensure that any other intermediate devices are configured to forward the appropriate ports!  
11. Good Luck!

Android Application:  

Minimum API Version: 26  
Android Studio: Giraffe   

ARCore GeoSpatial Anchors:  

Must create a reference to the project in Google Cloud Platform followed  
by the steps found here: https://developers.google.com/ar/develop/java/geospatial/enable#kotlin  

Simply build and run the application after debugging has been enabled on your device!  

*** Note: You will have to change the target hostname for in the MapViewFragment to match your GeoServer Instance!  

## Links to our short and presentation videos
[Demo+Code Video.mp4]()

[Presentation Video.mp4](https://youtu.be/fIRqFf2k25I)

[Short Video.mp4](https://youtu.be/SeqPhlnhU8w)

## Link to our GitHub project
[Link to our Github project](https://github.com/csu-hci-projects/FA23-Visualization-of-Infrastructure-with-a-3D-GIS-Mobile-Application)

## Link to our Overleaf project
[Link to our Overleaf project](https://www.overleaf.com/read/mpvgkjbrqcfx#a887c1 )

