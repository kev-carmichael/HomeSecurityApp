module com.udacity.catpoint.imageService {
    requires java.desktop;
    requires miglayout;
    requires org.slf4j;
    requires software.amazon.awssdk.auth;
    requires software.amazon.awssdk.core;
    requires software.amazon.awssdk.regions;
    requires software.amazon.awssdk.services.rekognition;
    //exports com.udacity.catpoint.service to com.udacity.catpoint.securityService; OLD CODE
    exports com.udacity.catpoint.service;

}
