module com.caven.redistool {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.base;
    requires lettuce.core;
    requires com.google.gson;
    requires org.slf4j;


    opens com.caven.redistool.controller to javafx.fxml;
    opens com.caven.redistool to javafx.fxml;
    opens com.caven.redistool.utils to javafx.fxml;
    opens com.caven.redistool.config to com.google.gson;
    opens com.caven.redistool.entity to com.google.gson;
    exports com.caven.redistool;
    exports com.caven.redistool.controller;
    exports com.caven.redistool.utils;
    exports com.caven.redistool.entity;

}