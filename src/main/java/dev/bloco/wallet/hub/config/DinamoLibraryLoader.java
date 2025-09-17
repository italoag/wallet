package dev.bloco.wallet.hub.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class DinamoLibraryLoader {

    private static final Map<String, Consumer<String>> OS_LIBRARY_LOADER =
            Map.of(
                    "win", DinamoLibraryLoader::loadWindowsLibraries,
                    "nix", DinamoLibraryLoader::loadLinuxLibraries);

    @Before("execution(* dev.bloco.wallet.hub.WalletHubApplication.main(..))")
    public void loadLibrariesBasedOnOS() {
        String osName = System.getProperty("os.name").toLowerCase();
        OS_LIBRARY_LOADER.entrySet().stream()
                .filter(entry -> osName.contains(entry.getKey()))
                .findFirst()
                .orElseThrow(
                        () -> new UnsupportedOperationException("Unsupported operating system: " + osName))
                .getValue()
                .accept(osName);
    }

    private static void loadWindowsLibraries(String osName) {
        loadLibrary("libs/windows/tacndlib.dll");
        loadLibrary("libs/windows/tacndjavalib.dll");
        log.info("Windows Dinamo HSM libraries loaded successfully.");
    }

    private static void loadLinuxLibraries(String osName) {
        loadLibrary("libs/linux/liibtacndlib.so");
        loadLibrary("libs/linux/libtacndjavalib.so");
        log.info("Linux Dinamo HSM libraries loaded successfully.");
    }

    private static void loadLibrary(String path) {
        if (Files.exists(Path.of(path))) {
            System.load(path);
        } else {
            log.error("Library not found at path: {}", path);
        }
    }
}
