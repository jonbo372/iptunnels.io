package io.iptunnels.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Map;

import static org.junit.Assert.assertThat;

public class ConfigurationEnvironmentTest {

    private Map<String, String> env;

    private static final String DEFAULT_HOME = "/tmp/nisse";

    private final String project = "helloworld";

    private ConfigurationEnvironment<Object> configEnv;

    @Before
    public void setUp() throws Exception {
        System.setProperty("user.home", DEFAULT_HOME);
        configEnv = ConfigurationEnvironment.of(ConfigTest.class).withProjectName(project).build();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testCorrectHomeDirectory() throws Exception {
        final String expectedProjectHome = DEFAULT_HOME + File.separator + "." + project;
        final String expectedConfig = expectedProjectHome + File.separator + "config";

        assertThat(configEnv.home().toString(), CoreMatchers.is(expectedProjectHome));
        assertThat(configEnv.configFile().toString(), CoreMatchers.is(expectedConfig));
    }

    @Test
    public void testInitializeConfigEnv() throws Exception {
        configEnv.init();
    }

    public static class ConfigTest {
        @JsonProperty("whatever")
        private final String whatever = "sure";
    }
}