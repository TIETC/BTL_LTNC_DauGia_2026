module vn.edu.uet.daugia {

    requires javafx.controls;
    requires javafx.fxml;

    requires com.google.gson;

    requires java.sql;

    // =========================
    // LOGIN
    // =========================


    // =========================
    // LIST
    // =========================


    // =========================
    // SHARED MODEL
    // =========================

    opens vn.edu.uet.daugia.shared.model
            to com.google.gson;

    exports vn.edu.uet.daugia.shared.model;

    // =========================
    // CLIENT
    // =========================

    exports vn.edu.uet.daugia.client;

    opens vn.edu.uet.daugia.client
            to javafx.graphics,
            javafx.fxml;

    // =========================
    // CLIENT CONTROLLER
    // =========================

    exports vn.edu.uet.daugia.client.Controller;

    opens vn.edu.uet.daugia.client.Controller
            to javafx.fxml;

    // =========================
    // CLIENT MODEL
    // =========================

    exports vn.edu.uet.daugia.client.model;

    opens vn.edu.uet.daugia.client.model
            to javafx.base;

    // =========================
    // UTIL
    // =========================

    exports vn.edu.uet.daugia.client.network;

    exports vn.edu.uet.daugia.client.util;

    opens vn.edu.uet.daugia.shared.model.item to com.google.gson;
    exports vn.edu.uet.daugia.shared.model.item;

    opens vn.edu.uet.daugia.shared.model.user to com.google.gson;
    exports vn.edu.uet.daugia.shared.model.user;
}