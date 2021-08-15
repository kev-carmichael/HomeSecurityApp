module com.udacity.catpoint.securityService {
    requires java.datatransfer;
    requires java.desktop;
    requires java.prefs;
    requires com.google.gson;
    requires com.google.common;
    requires com.udacity.catpoint.imageService;
    requires miglayout;
    //exports com.udacity.catpoint.data to com.udacity.catpoint.imageService; OLD CODE
    opens com.udacity.catpoint.data to com.google.gson;
    //exports com.udacity.catpoint.servicesecurity; OLD CODE
}