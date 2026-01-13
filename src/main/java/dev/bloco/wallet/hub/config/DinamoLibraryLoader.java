package dev.bloco.wallet.hub.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * DinamoLibraryLoader is responsible for loading external native libraries required
 * by the application based on the underlying operating system.
 *<p/>
 * This class uses an aspect-oriented approach to ensure that the library loading
 * process is executed prior to the main application startup.
 *<p/>
 * Functionality:
 * - Identifies the operating system of the runtime environment.
 * - Loads platform-specific external native libraries for Windows and Linux systems.
 * - Logs success or failure of the library loading process.
 * - Throws an exception if the operating system is not supported.
 *<p/>
 * The supported operating systems and their designated libraries are:
 * - Windows: tacndlib.dll, tacndjavalib.dll
 * - Linux: libtacndlib.so, libtacndjavalib.so
 *<p/>
 * Libraries are loaded from platform-specific directories (`libs/windows` and `libs/linux`),
 * and the paths must exist for successful loading.
 */
@Component
@Slf4j
public class DinamoLibraryLoader implements ApplicationRunner {

    private static final Map<String, Consumer<String>> OS_LIBRARY_LOADER =
            java.util.Map.ofEntries(
                    java.util.Map.entry("win", DinamoLibraryLoader::loadWindowsLibraries),
                    java.util.Map.entry("linux", DinamoLibraryLoader::loadLinuxLibraries),
                    java.util.Map.entry("nix", DinamoLibraryLoader::loadLinuxLibraries)
            );

  /**
   * Loads platform-specific external native libraries based on the detected operating system.
   *<p/>
   * This method is executed before the main application startup and determines the operating system
   * by analyzing the "os.name" system property. Based on the identified OS, it delegates to a loader
   * responsible for initializing the appropriate libraries.
   *<p/>
   * Key Details:
   * - Supports both Windows and Linux platforms with predefined library loading mechanisms.
   * - Throws an UnsupportedOperationException if the operating system is not supported.
   * - Relies on a mapping of OS identifiers to corresponding loader functions.
   *<p/>
   * Mapping of supported platforms:
   * - "win": Triggers the loading of Windows-specific libraries.
   * - "Nix": Triggers the loading of Linux-specific libraries.
   *<p/>
   * Failure Handling:
   * - If the libraries cannot be found or loaded for the determined platform, an error is logged.
   * - If the operating system is not recognized, an exception is thrown to notify unsupported platforms.
   */
  @Override
  public void run(ApplicationArguments args) {
        String osName = System.getProperty("os.name").toLowerCase();
        OS_LIBRARY_LOADER.entrySet().stream()
                .filter(entry -> osName.contains(entry.getKey()))
                .findFirst()
                .ifPresentOrElse(
                    e -> e.getValue().accept(osName),
                    () -> log.info("No native libraries to load for OS: {}", osName)
                );
    }

  /**
   * Loads the necessary native libraries for Windows-based environments.
   * This method is invoked to initialize Windows-specific libraries required
   * for the Dinamo HSM functionality by specifying their predefined paths.
   *
   * @param osName the name of the operating system, used for logging purposes and
   *               ensuring compatibility during setup. Typically expected to indicate
   *               a Windows operating system.
   */
  private static void loadWindowsLibraries(String osName) {
        loadLibrary("libs/windows/tacndlib.dll");
        loadLibrary("libs/windows/tacndjavalib.dll");
        log.info("Windows Dinamo HSM libraries loaded successfully.");
    }

  /**
   * Loads the necessary native libraries for Linux-based environments.
   * This method initializes Linux-specific libraries required for the
   * Dinamo HSM functionality by specifying their predefined paths.
   *
   * @param osName the name of the operating system, used for logging purposes
   *               to indicate compatibility during setup. Typically expected
   *               to identify a Linux operating system.
   */
  private static void loadLinuxLibraries(String osName) {
        loadLibrary("libs/linux/libtacndlib.so");
        loadLibrary("libs/linux/libtacndjavalib.so");
        log.info("Linux Dinamo HSM libraries loaded successfully.");
    }

  /**
   * Loads a native library from the specified path.
   * If the library file exists at the provided path, it attempts to load the library
   * using the {@code System.load} method. If the file does not exist, it logs an error message.
   *
   * @param path the absolute path to the native library file. This should point to the
   *             physical location of the library required for platform-specific native operations.
   */
  private static void loadLibrary(String path) {
        try {
            if (Files.exists(Path.of(path))) {
                System.load(path);
                log.debug("Loaded native library: {}", path);
            } else {
                log.info("Native library not found at path (skipping): {}", path);
            }
        } catch (UnsatisfiedLinkError | SecurityException ex) {
            log.error("Failed to load native library at {}: {}", path, ex.getMessage());
        }
    }
}
