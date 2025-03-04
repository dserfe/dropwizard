package com.example.request_log;

import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.configuration.ResourceConfigurationSourceProvider;
import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.util.Duration;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.util.Collections;
import java.util.List;

@ExtendWith(DropwizardExtensionsSupport.class)
public abstract class AbstractRequestLogPatternIntegrationTest {

    public static class TestApplication extends Application<Configuration> {
        public static void main(String[] args) throws Exception {
            new TestApplication().run(args);
        }

        @Override
        public void run(Configuration configuration, Environment environment) {
            environment.jersey().register(TestResource.class);
            environment.healthChecks().register("dummy", new HealthCheck() {
                @Override
                protected Result check() {
                    return Result.healthy();
                }
            });
        }
    }

    @Path("/greet")
    public static class TestResource {
        @GET
        public String get(@QueryParam("name") String name, @Context HttpServletRequest httpRequest) {
            return String.format("Hello, %s!", name);
        }
    }

    @TempDir
    static java.nio.file.Path tempDir;

    protected java.nio.file.Path requestLogFile = tempDir.resolve("request-logs");
    protected Client client;

    DropwizardAppExtension<Configuration> dropwizardAppRule = new DropwizardAppExtension<>(TestApplication.class,
        "request_log/test-custom-request-log.yml",
        new ResourceConfigurationSourceProvider(),
        configOverrides().toArray(new ConfigOverride[0]));

    protected List<ConfigOverride> configOverrides() {
        return Collections.singletonList(ConfigOverride.config("server.requestLog.appenders[0].currentLogFilename", requestLogFile.toString()));
    }

    @BeforeEach
    public void setUp() {
        final JerseyClientConfiguration configuration = new JerseyClientConfiguration();
        configuration.setTimeout(Duration.seconds(2));
        client = new JerseyClientBuilder(dropwizardAppRule.getEnvironment())
            .using(configuration)
            .build("test-request-logs");
    }

    @AfterEach
    public void tearDown() {
        client.close();
    }
}
