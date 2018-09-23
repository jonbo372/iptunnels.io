package io.iptunnels.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Helper for dealing with configuration.
 */
public interface ConfigurationEnvironment<T> {

    String DEFAULT_CONFIG_FILE = "config";

    /**
     * Create a new {@link ConfigurationEnvironment} based off of the project name. By default, for
     * client side type of project (i.e. you run something on your own laptop e.g.), this is all you
     * need to specify and the default values will be based off of the "." (dot) name of the project
     * and it is assumed that there is a folder in the users HOME directory to store all project settings.
     *
     * @return
     */
    static <T> ProjectNameBuilder of(final Class<T> clazz) {
        return name -> new Builder<T>(clazz, name);
    }

    interface ProjectNameBuilder {
        Builder withProjectName(String name);
    }

    Path home();

    Path configFile();

    /**
     * Initialize the project home directory and configuration file. This will ensure that if
     * the project home and necessary files doesn't already exist, those will be created.
     */
    void init() throws IOException;

    T loadConfig() throws IOException;


    class Builder<T> {

        private final String projectName;

        /**
         * The configuration directory for this project. Unless explicitly set, it will be
         * based off of the users HOME directory and a "dot" version of the project name.
         */
        private final Optional<String> projectConfigHome = Optional.empty();

        /**
         * The name of the project configuration file containing all the settings.
         * By default the name of this file is simply "config" and is stored under
         * the project configuration home.
         */
        private final String configFile = DEFAULT_CONFIG_FILE;

        private final Class<T> configClass;

        private Builder(final Class<T> configClass, final String projectName) {
            this.configClass = configClass;
            this.projectName = projectName;
        }

        /**
         * Build the {@link ConfigurationEnvironment} and ensure that the home directory
         * exists and if not, it will get created and if that fails (due to permission) then
         * the build will fail.
         *
         * Also, if the configuration file for the project doesn't exist, a default one will
         * be created based off of the values in the main config object.
         *
         * @return
         */
        public ConfigurationEnvironment build() {
            final Path home = ensureHome();
            final Path config = home.resolve(configFile);
            return new DefaultConfigurationEnvironment(configClass, configureMapper(), home, config);
        }

        private Path ensureHome() {
            final String home = projectConfigHome.orElseGet(this::defaultProjectHome);
            return Paths.get(home);
        }

        private String defaultProjectHome() {
            return System.getProperty("user.home", calculateTempDir()) + File.separator + "." + projectName;
        }

        /**
         * If all else fails, i.e. an explicit project config home hasn't been specified, nor has
         * the system a HOME directory set then figure out a TMP location, which depends on the OS.
         *
         * @return
         */
        private String calculateTempDir() {
            return "/tmp";
        }

        private ObjectMapper configureMapper() {
            final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            final SimpleModule module = new SimpleModule();
            mapper.registerModule(module);
            return mapper;
        }

    }

    class DefaultConfigurationEnvironment<T> implements ConfigurationEnvironment {

        private final Class<T> configClass;
        private final ObjectMapper objectMapper;
        private final Path home;
        private final Path configFile;
        private boolean initialized = false;


        private DefaultConfigurationEnvironment(final Class<T> configClass, final ObjectMapper objectMapper, final Path home, final Path configFile) {
            this.configClass = configClass;
            this.objectMapper = objectMapper;
            this.home = home;
            this.configFile = configFile;
        }

        @Override
        public Path home() {
            return home;
        }

        @Override
        public Path configFile() {
            return configFile;
        }

        @Override
        public void init() throws IOException {
            Files.createDirectories(home);
            if (!Files.exists(configFile)) {
                Files.createFile(configFile);
                final T defaultConfig = createDefaultConfig();
                objectMapper.writeValue(configFile.toFile(), defaultConfig);
            }
            initialized = true;
        }

        @Override
        public T loadConfig() throws IOException {
            if (!initialized) {
                init();
            }
            final InputStream stream = Files.newInputStream(configFile);
            return objectMapper.readValue(stream, configClass);
        }

        private T createDefaultConfig() {
            try {
                return (T)configClass.getConstructors()[0].newInstance();
            } catch (final InstantiationException e) {
                e.printStackTrace();
            } catch (final IllegalAccessException e) {
                e.printStackTrace();
            } catch (final InvocationTargetException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * Helper method for loading a configuration.
     *
     * @param clazz
     * @param resource
     * @return
     * @throws Exception
     */
    static <T> T loadConfiguration(final Class<T> clazz, final String resource) throws Exception {
        // final InputStream stream = ConfigurationEnvironment.class.getResourceAsStream(resource);
        // return mapper.readValue(stream, clazz);
        return null;
    }
}
