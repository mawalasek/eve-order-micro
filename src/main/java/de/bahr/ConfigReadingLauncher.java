package de.bahr;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class ConfigReadingLauncher extends io.vertx.core.Launcher {

    private static final String CONFIG_PATH = "src/config/config.json";

    private Logger logger;

    public static void main(String[] args) {
        new ConfigReadingLauncher().dispatch(args);
    }

    @Override
    public void beforeDeployingVerticle(DeploymentOptions deploymentOptions) {
        super.beforeDeployingVerticle(deploymentOptions);

        initLogger();

        if (deploymentOptions.getConfig() == null) {
            deploymentOptions.setConfig(new JsonObject());
        }

        File configFile = new File(CONFIG_PATH);
        deploymentOptions.getConfig().mergeIn(readConfiguration(configFile));
    }

    private JsonObject readConfiguration(File configFile) {
        JsonObject conf = new JsonObject();
        if (configFile.isFile()) {
            logger.debug("Reading config file: " + configFile.getAbsolutePath());
            try (Scanner scanner = new Scanner(configFile).useDelimiter("\\A")) {
                String sconf = scanner.next();
                try {
                    conf = new JsonObject(sconf);
                } catch (DecodeException e) {
                    logger.debug("Configuration file " + sconf + " does not contain a valid JSON object");
                }
            } catch (FileNotFoundException e) {
                // Ignore it.
            }
        } else {
            logger.debug("Config file not found " + configFile.getAbsolutePath());
        }
        return conf;
    }

    private void initLogger() {
        BasicConfigurator.configure();
        logger = Logger.getLogger(ConfigReadingLauncher.class.getName());
    }
}
