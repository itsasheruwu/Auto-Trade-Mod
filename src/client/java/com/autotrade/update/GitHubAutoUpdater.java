package com.autotrade.update;

import com.autotrade.config.AutoTradeConfig;
import com.autotrade.config.ConfigManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class GitHubAutoUpdater {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String USER_AGENT = "autotrade-mod-updater";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "autotrade-updater");
        thread.setDaemon(true);
        return thread;
    });

    private static final Object CHECK_LOCK = new Object();

    private static volatile boolean checkStarted;
    private static volatile String pendingClientMessage;

    private GitHubAutoUpdater() {
    }

    public static void runAsyncStartupCheck() {
        startCheck(true);
    }

    public static void onClientTick(MinecraftClient client) {
        if (pendingClientMessage == null || client.player == null) {
            return;
        }

        String message = pendingClientMessage;
        pendingClientMessage = null;
        sendClientMessage(client, message);
    }

    private static void startCheck(boolean async) {
        synchronized (CHECK_LOCK) {
            if (checkStarted) {
                return;
            }
            checkStarted = true;
        }

        Runnable check = () -> {
            try {
                Optional<String> resultMessage = checkAndApplyUpdate();
                resultMessage.ifPresent(message -> {
                    pendingClientMessage = message;
                    LOGGER.info(message);
                });
            } catch (Exception exception) {
                LOGGER.warn("Auto update startup check failed", exception);
            }
        };

        if (async) {
            CompletableFuture.runAsync(check, EXECUTOR);
            return;
        }

        check.run();
    }

    private static Optional<String> checkAndApplyUpdate() {
        AutoTradeConfig config = ConfigManager.getConfig();
        if (!config.autoUpdateEnabled) {
            return Optional.empty();
        }

        String owner = config.autoUpdateGithubOwner == null ? "" : config.autoUpdateGithubOwner.trim();
        String repo = config.autoUpdateGithubRepo == null ? "" : config.autoUpdateGithubRepo.trim();
        if (owner.isEmpty() || repo.isEmpty()) {
            return Optional.empty();
        }

        Path currentJar = getCurrentJarPath();
        if (currentJar == null) {
            LOGGER.info("Auto update skipped: running outside a jar (development environment).");
            return Optional.empty();
        }

        String currentVersion = getCurrentVersion().orElse("0.0.0");
        Optional<ReleaseInfo> latestRelease = fetchLatestRelease(owner, repo);
        if (latestRelease.isEmpty()) {
            return Optional.empty();
        }

        ReleaseInfo release = latestRelease.get();
        if (!isVersionNewer(currentVersion, release.tagName())) {
            return Optional.empty();
        }

        Optional<AssetInfo> asset = pickJarAsset(release.assets());
        if (asset.isEmpty()) {
            LOGGER.warn("Auto update skipped: no jar asset found for release {}", release.tagName());
            return Optional.empty();
        }

        try {
            Path downloadedJar = downloadAsset(asset.get(), currentJar);
            boolean applied = swapJar(currentJar, downloadedJar);
            if (applied) {
                return Optional.of("AutoTrade updated to " + release.tagName() + ". Restart Minecraft to load updated code.");
            }
        } catch (Exception exception) {
            LOGGER.warn("Auto update failed", exception);
        }

        return Optional.empty();
    }

    private static Optional<ReleaseInfo> fetchLatestRelease(String owner, String repo) {
        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/releases/latest";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();
        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOGGER.warn("Release check failed with status {}", response.statusCode());
                return Optional.empty();
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            String tag = getString(json, "tag_name").orElse("");
            if (tag.isBlank()) {
                return Optional.empty();
            }

            JsonArray assetsArray = json.has("assets") && json.get("assets").isJsonArray()
                    ? json.getAsJsonArray("assets")
                    : new JsonArray();
            List<AssetInfo> assets = new ArrayList<>();
            for (JsonElement element : assetsArray) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject asset = element.getAsJsonObject();
                String name = getString(asset, "name").orElse("");
                String downloadUrl = getString(asset, "browser_download_url").orElse("");
                if (!name.isBlank() && !downloadUrl.isBlank()) {
                    assets.add(new AssetInfo(name, downloadUrl));
                }
            }

            return Optional.of(new ReleaseInfo(tag, assets));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Release check interrupted", exception);
            return Optional.empty();
        } catch (IOException exception) {
            LOGGER.warn("Release check failed", exception);
            return Optional.empty();
        }
    }

    private static Optional<AssetInfo> pickJarAsset(List<AssetInfo> assets) {
        return assets.stream()
                .filter(asset -> asset.name().endsWith(".jar"))
                .filter(asset -> !asset.name().contains("-sources"))
                .sorted(Comparator.comparing((AssetInfo asset) -> !asset.name().toLowerCase().contains("autotrade")))
                .findFirst();
    }

    private static Path downloadAsset(AssetInfo asset, Path currentJar) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(asset.downloadUrl()))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/octet-stream")
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();
        HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Download failed with status " + response.statusCode());
        }

        Path tempJar = currentJar.resolveSibling(currentJar.getFileName() + ".download");
        try (InputStream stream = response.body()) {
            Files.copy(stream, tempJar, StandardCopyOption.REPLACE_EXISTING);
        }
        return tempJar;
    }

    private static boolean swapJar(Path currentJar, Path downloadedJar) {
        if (!Files.exists(currentJar) || !Files.exists(downloadedJar)) {
            return false;
        }

        Path backupJar = currentJar.resolveSibling(currentJar.getFileName() + ".backup");
        try {
            Files.move(currentJar, backupJar, StandardCopyOption.REPLACE_EXISTING);
            try {
                Files.move(downloadedJar, currentJar, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(downloadedJar, currentJar, StandardCopyOption.REPLACE_EXISTING);
            }
            LOGGER.info("Auto update applied: {}", currentJar);
            return true;
        } catch (IOException exception) {
            LOGGER.warn("Failed to apply update", exception);
            if (!Files.exists(currentJar) && Files.exists(backupJar)) {
                try {
                    Files.move(backupJar, currentJar, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException restoreException) {
                    LOGGER.warn("Failed to restore original jar after update failure", restoreException);
                }
            }
            return false;
        } finally {
            try {
                Files.deleteIfExists(downloadedJar);
            } catch (IOException ignored) {
            }
        }
    }

    private static Optional<Path> getCodeSourcePath() {
        try {
            CodeSource source = GitHubAutoUpdater.class.getProtectionDomain().getCodeSource();
            if (source == null || source.getLocation() == null) {
                return Optional.empty();
            }
            return Optional.of(Path.of(source.getLocation().toURI()).toAbsolutePath().normalize());
        } catch (URISyntaxException exception) {
            return Optional.empty();
        }
    }

    private static Path getCurrentJarPath() {
        Optional<Path> sourcePath = getCodeSourcePath();
        if (sourcePath.isEmpty()) {
            return null;
        }

        Path path = sourcePath.get();
        return path.toString().endsWith(".jar") ? path : null;
    }

    private static Optional<String> getCurrentVersion() {
        return FabricLoader.getInstance()
                .getModContainer("autotrade")
                .map(container -> container.getMetadata().getVersion().getFriendlyString());
    }

    private static boolean isVersionNewer(String currentVersion, String latestTag) {
        String current = normalizeVersion(currentVersion);
        String latest = normalizeVersion(latestTag);
        return compareVersion(latest, current) > 0;
    }

    private static String normalizeVersion(String version) {
        String value = version == null ? "" : version.trim();
        if (value.startsWith("v") || value.startsWith("V")) {
            value = value.substring(1);
        }
        int plusIndex = value.indexOf('+');
        if (plusIndex >= 0) {
            value = value.substring(0, plusIndex);
        }
        int dashIndex = value.indexOf('-');
        if (dashIndex >= 0) {
            value = value.substring(0, dashIndex);
        }
        return value;
    }

    private static int compareVersion(String left, String right) {
        String[] leftParts = left.split("\\.");
        String[] rightParts = right.split("\\.");
        int max = Math.max(leftParts.length, rightParts.length);
        for (int i = 0; i < max; i++) {
            int leftValue = i < leftParts.length ? parseLeadingInt(leftParts[i]) : 0;
            int rightValue = i < rightParts.length ? parseLeadingInt(rightParts[i]) : 0;
            if (leftValue != rightValue) {
                return Integer.compare(leftValue, rightValue);
            }
        }
        return 0;
    }

    private static int parseLeadingInt(String value) {
        int end = 0;
        while (end < value.length() && Character.isDigit(value.charAt(end))) {
            end++;
        }
        if (end == 0) {
            return 0;
        }
        try {
            return Integer.parseInt(value.substring(0, end));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static Optional<String> getString(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) {
            return Optional.empty();
        }
        return Optional.ofNullable(object.get(key).getAsString());
    }

    private static void sendClientMessage(MinecraftClient client, String message) {
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(Text.literal("[AutoTrade] " + message), false);
            }
        });
    }

    private record ReleaseInfo(String tagName, List<AssetInfo> assets) {
    }

    private record AssetInfo(String name, String downloadUrl) {
    }
}
