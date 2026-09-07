package org.example;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/**
 * Publishes the random embedded-server port to the desktop launcher and closes
 * the server if its Electron parent disappears unexpectedly. It is inert for
 * normal web and Docker launches because the port-file property is absent.
 */
@Component
@Profile("desktop")
final class DesktopLifecycle implements ApplicationListener<WebServerInitializedEvent> {
    private final ConfigurableApplicationContext application;
    private final Path portFile;
    private final long parentPid;

    DesktopLifecycle(ConfigurableApplicationContext application,
            @Value("${blackjack.desktop.port-file}") String portFile,
            @Value("${blackjack.desktop.parent-pid:0}") long parentPid) {
        this.application = application;
        this.portFile = Path.of(portFile).toAbsolutePath();
        this.parentPid = parentPid;
    }

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        publishPort(event.getWebServer().getPort());
        if (parentPid > 0) {
            Thread.ofVirtual().name("desktop-parent-monitor").start(this::monitorParent);
        }
    }

    private void publishPort(int port) {
        try {
            Files.createDirectories(portFile.getParent());
            var temporary = portFile.resolveSibling(portFile.getFileName() + ".tmp");
            Files.writeString(temporary, Integer.toString(port));
            try {
                Files.move(temporary, portFile, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, portFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot publish the desktop server port", exception);
        }
    }

    private void monitorParent() {
        while (application.isActive()) {
            var parent = ProcessHandle.of(parentPid);
            if (parent.isEmpty() || !parent.get().isAlive()) {
                application.close();
                return;
            }
            try {
                Thread.sleep(Duration.ofSeconds(1));
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
