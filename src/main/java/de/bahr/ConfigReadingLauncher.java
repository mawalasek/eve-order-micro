package de.bahr;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class ConfigReadingLauncher extends io.vertx.core.Launcher {

    private static final String CONFIG_PATH = "src/config/config.json";

    public static void main(String[] args) {
        new ConfigReadingLauncher().dispatch(args);
    }

    @Override
    public void beforeDeployingVerticle(DeploymentOptions deploymentOptions) {
        super.beforeDeployingVerticle(deploymentOptions);

        if (deploymentOptions.getConfig() == null) {
            deploymentOptions.setConfig(new JsonObject());
        }

        File configFile = new File(CONFIG_PATH);
        deploymentOptions.getConfig().mergeIn(readConfiguration(configFile));
    }

    private JsonObject readConfiguration(File configFile) {
        JsonObject conf = new JsonObject();
        if (configFile.isFile()) {
            System.out.println("Reading config file: " + configFile.getAbsolutePath());
            try (Scanner scanner = new Scanner(configFile).useDelimiter("\\A")) {
                String sconf = scanner.next();
                try {
                    conf = new JsonObject(sconf);
                } catch (DecodeException e) {
                    System.err.println("Configuration file " + sconf + " does not contain a valid JSON object");
                }
            } catch (FileNotFoundException e) {
                // Ignore it.
            }
        } else {
            System.out.println("Config file not found " + configFile.getAbsolutePath());
        }
        return conf;
    }
}
