package io.github.thebusybiscuit.slimefun4.libraries.paperlib;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import io.github.thebusybiscuit.slimefun4.libraries.paperlib.features.blockstatesnapshot.BlockStateSnapshotResult;

/**
 * Drop-in stub for {@code io.papermc.lib.PaperLib} (PaperLib 1.0.8).
 * <p>
 * This class is woven in at build time (maven-shade-plugin) in place of the real
 * PaperLib library. We target Paper only, so all PaperLib API calls used by
 * Slimefun are forwarded to the native Paper / Bukkit API, keeping the final jar
 * free of the ~30 PaperLib classes while leaving the upstream source untouched.
 * <p>
 * The byte-code references of the upstream sources are relocated to this package
 * ({@code io.papermc.lib} -> {@code io.github.thebusybiscuit.slimefun4.libraries.paperlib}),
 * so the class name must match exactly what the real PaperLib would have exposed.
 */
public final class PaperLib {

    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    private PaperLib() {}

    public static boolean isSpigot() {
        return true;
    }

    public static boolean isPaper() {
        return true;
    }

    public static boolean isVersion(int version) {
        return getMinecraftVersion() == version;
    }

    public static boolean isVersion(int version, int patchVersion) {
        return getMinecraftVersion() == version && getMinecraftPatchVersion() == patchVersion;
    }

    public static int getMinecraftVersion() {
        return parseBukkitVersion()[0];
    }

    public static int getMinecraftPatchVersion() {
        return parseBukkitVersion()[1];
    }

    public static int getMinecraftPreReleaseVersion() {
        return 0;
    }

    public static int getMinecraftReleaseCandidateVersion() {
        return 0;
    }

    /**
     * Forwards to the native {@link Entity#teleportAsync(Location)} API of Paper.
     *
     * @param entity
     *            The entity to teleport
     * @param location
     *            The destination
     *
     * @return A {@link CompletableFuture} that completes with the result of the teleport
     */
    @ParametersAreNonnullByDefault
    public static @Nonnull CompletableFuture<Boolean> teleportAsync(Entity entity, Location location) {
        return entity.teleportAsync(location);
    }

    /**
     * Returns a live {@link org.bukkit.block.BlockState} for the given block.
     * The {@code forceSync} flag is ignored: on Paper, {@link Block#getState()} is
     * already a lightweight snapshot and safe to call from the main thread.
     *
     * @param block
     *            The block whose state is requested
     * @param forceSync
     *            Ignored (accepted for API compatibility)
     *
     * @return A {@link BlockStateSnapshotResult} wrapping the block state
     */
    @ParametersAreNonnullByDefault
    public static @Nonnull BlockStateSnapshotResult getBlockState(Block block, boolean forceSync) {
        org.bukkit.block.BlockState state = block.getState();

        return new BlockStateSnapshotResult() {

            @Override
            public org.bukkit.block.BlockState getState() {
                return state;
            }

            @Override
            public void run() {
                // The state is already live, nothing to apply.
            }
        };
    }

    /**
     * Prints a message suggesting the use of Paper.
     * Never called by Slimefun while this stub reports {@link #isPaper()} == true.
     *
     * @param plugin
     *            The plugin instance
     */
    @ParametersAreNonnullByDefault
    public static void suggestPaper(Plugin plugin) {
        suggestPaper(plugin, Level.WARNING);
    }

    /**
     * Prints a message suggesting the use of Paper.
     * Never called by Slimefun while this stub reports {@link #isPaper()} == true.
     *
     * @param plugin
     *            The plugin instance
     * @param level
     *            The log level to use
     */
    @ParametersAreNonnullByDefault
    public static void suggestPaper(Plugin plugin, Level level) {
        plugin.getLogger().log(level, "This plugin works best on Paper servers!");
    }

    /**
     * Parses the current server version.
     *
     * @return An array holding {@code [major, patch]}, e.g. {@code [26, 2]} for
     *         {@code 26.1.2} and {@code [16, 5]} for {@code 1.16.5}
     */
    private static int[] parseBukkitVersion() {
        String bukkitVersion = Bukkit.getBukkitVersion();
        Matcher matcher = VERSION_PATTERN.matcher(bukkitVersion);

        if (matcher.find()) {
            int first = Integer.parseInt(matcher.group(1));
            int second = Integer.parseInt(matcher.group(2));
            int third = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;

            if (first == 1) {
                // Legacy "1.x.y" scheme: 1.16.5 -> major 16, patch 5
                return new int[] { second, third };
            }

            return new int[] { first, third };
        }

        return new int[] { 0, 0 };
    }
}
