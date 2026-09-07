package org.example;

import com.vaadin.flow.server.InitParameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

/** Tests the deployable artifact, including its actual runtime mode. */
class ProductionPackageIT {

    @TempDir
    Path temporaryDirectory;

    @Test
    void packagedApplicationRunsInProductionWithoutDevelopmentTools() throws Exception {
        var artifact = Path.of(System.getProperty("blackjack.jar")).toAbsolutePath();
        try (var jar = new ZipFile(artifact.toFile())) {
            var unwanted = jar.stream().map(entry -> entry.getName())
                    .filter(name -> name.startsWith("BOOT-INF/lib/"))
                    .filter(name -> name.contains("vaadin-dev") || name.contains("copilot")
                            || name.contains("spring-boot-devtools") || name.contains("testbench"))
                    .toList();
            assertTrue(unwanted.isEmpty(), () -> "Development libraries packaged: " + unwanted);

            var configuration = new Properties();
            try (var input = jar.getInputStream(jar.getEntry("BOOT-INF/classes/application.properties"))) {
                configuration.load(input);
            }
            assertEquals("true", configuration.getProperty(
                    "vaadin." + InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE));

            var buildInfo = jar.getEntry("META-INF/VAADIN/config/flow-build-info.json");
            assertNotNull(buildInfo, "Missing Vaadin production build metadata");
            try (var input = jar.getInputStream(buildInfo)) {
                var json = new String(input.readAllBytes(), UTF_8);
                assertTrue(Pattern.compile("\"productionMode\"\\s*:\\s*true").matcher(json).find(),
                        "Vaadin frontend must be compiled for production");
            }
        }

        var log = temporaryDirectory.resolve("application.log");
        var java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        var portFile = temporaryDirectory.resolve("desktop-port");
        var builder = new ProcessBuilder(java, "-jar", artifact.toString(),
                "--server.address=127.0.0.1", "--server.port=0",
                "--spring.profiles.active=desktop",
                "--blackjack.desktop.port-file=" + portFile,
                "--blackjack.desktop.parent-pid=" + ProcessHandle.current().pid())
                .directory(temporaryDirectory.toFile())
                .redirectErrorStream(true)
                .redirectOutput(log.toFile());
        // Test the packaged defaults, independently of local development settings.
        builder.environment().remove("SPRING_APPLICATION_JSON");
        builder.environment().remove("SPRING_PROFILES_ACTIVE");
        builder.environment().remove("VAADIN_PRODUCTION_MODE");
        var process = builder.start();
        try (var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
            var port = awaitPublishedPort(process, log, portFile);
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/"))
                    .timeout(Duration.ofSeconds(10)).GET().build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("VAADIN/"), "The application must serve its Vaadin frontend");
            var russian = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/ru"))
                    .timeout(Duration.ofSeconds(10)).GET().build();
            assertEquals(200, client.send(russian, HttpResponse.BodyHandlers.discarding()).statusCode());
            var spanish = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/es"))
                    .timeout(Duration.ofSeconds(10)).GET().build();
            assertEquals(200, client.send(spanish, HttpResponse.BodyHandlers.discarding()).statusCode());
            var stylesheet = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/styles/blackjack.css"))
                    .timeout(Duration.ofSeconds(10)).GET().build();
            var css = client.send(stylesheet, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, css.statusCode());
            assertTrue(css.body().contains(".blackjack-app .playing-card"), "Packaged card styles must be served");
            assertTrue(css.body().contains("max-width: 540px"), "Packaged mobile styles must be served");
            assertTrue(css.body().contains("height: 100dvh"), "Packaged single-screen layout must be served");
            var output = Files.readString(log);
            assertTrue(output.contains("Vaadin is running in production mode."), output);
            assertFalse(output.contains("Vaadin is running in DEVELOPMENT mode"), output);
        } finally {
            process.destroy();
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(10, TimeUnit.SECONDS);
            }
        }
    }

    private int awaitPublishedPort(Process process, Path log, Path portFile)
            throws IOException, InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        while (process.isAlive() && System.nanoTime() < deadline) {
            if (Files.isRegularFile(portFile)) {
                return Integer.parseInt(Files.readString(portFile));
            }
            Thread.sleep(Duration.ofMillis(100));
        }
        throw new AssertionError("Packaged desktop server did not publish its port:\n"
                + Files.readString(log));
    }
}
