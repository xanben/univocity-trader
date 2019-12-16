package com.univocity.trader.config;

import org.apache.commons.cli.*;
import org.slf4j.*;

import java.util.*;

/**
 * @author tom@khubla.com
 */
public class Configuration extends ConfigurationGroup {
	private static final Logger log = LoggerFactory.getLogger(Configuration.class);

	/**
	 * 'config file' option for command-line
	 */
	private static final String CONFIG_OPTION = "config";

	private static final String CONFIGURATION_FILE = "univocity-trader.properties";
	private static String[] configurationFiles = new String[]{CONFIGURATION_FILE};

	private static Configuration instance;

	public static Configuration getInstance() {
		if (instance == null) {
			throw new IllegalStateException("Configuration not defined. Use 'configure()', 'load(file)' or 'loadFromCommandLine()' to define your configuration");
		}
		return instance;
	}

	public static synchronized Configuration configure() {
		if (instance != null) {
			return instance;
		}
		return instance = new Configuration(false);
	}

	public static synchronized Configuration load() {
		return load(CONFIGURATION_FILE);
	}

	public static synchronized Configuration load(String filePath, String... alternativeFilePaths) {
		String[] original = configurationFiles.clone();

		configurationFiles = new String[alternativeFilePaths.length + 1];
		configurationFiles[0] = filePath;
		System.arraycopy(alternativeFilePaths, 0, configurationFiles, 1, alternativeFilePaths.length);

		try {
			Utils.noBlanks(configurationFiles, "Path to configuration file cannot be blank/null");
			if (instance != null) {
				instance.reload();
				return instance;
			}
			return instance = new Configuration(true);
		} catch (Throwable t) {
			configurationFiles = original;
			if (t instanceof IllegalConfigurationException) {
				throw (IllegalConfigurationException) t;
			} else {
				throw new IllegalConfigurationException("Unable to load configuration from " + Arrays.toString(configurationFiles), t);
			}
		}
	}

	public static Configuration loadFromCommandLine(String... args) {
		/*
		 * options
		 */
		final Options options = new Options();
		final Option oo = Option.builder().argName(CONFIG_OPTION).longOpt(CONFIG_OPTION).type(String.class).hasArg().required(true).desc("config file").build();
		options.addOption(oo);
		/*
		 * parse
		 */
		final CommandLineParser parser = new DefaultParser();
		String configFileName = null;
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
			/*
			 * get the file
			 */
			configFileName = cmd.getOptionValue(CONFIG_OPTION);
			if (null != configFileName) {
				return load(configFileName);
			}
		} catch (final Exception e) {
			if (configFileName != null) {
				log.error("Error loading configuration file: " + configFileName, e);
			}
			new HelpFormatter().printHelp("posix", options);
			System.exit(0);
		}
		return configure();
	}

	private boolean loadedFromFile = false;
	final DatabaseConfiguration databaseConfiguration = new DatabaseConfiguration(this);
	final EmailConfiguration emailConfiguration = new EmailConfiguration(this);

	private Configuration(boolean loadFromFile) {
		super(() -> instance);
		if (loadFromFile) {
			loadedFromFile = true;
			reload();
		}
	}

	public void reload() {
		if (!loadedFromFile) {
			return;
		}
		PropertyBasedConfiguration properties = new PropertyBasedConfiguration(configurationFiles);
		readProperties(properties);
	}

	@Override
	void readProperties(PropertyBasedConfiguration properties) {
		databaseConfiguration.readProperties(properties);
		emailConfiguration.readProperties(properties);
	}

	@Override
	public boolean isConfigured() {
		return databaseConfiguration.isConfigured();
	}
}