package io.github.thebusybiscuit.slimefun4.libraries.dough.versions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.Server;

import io.github.bakedlibs.dough.versions.SemanticVersion;

/**
 * This is an extension of {@link SemanticVersion}, specifically designed
 * for Minecraft's versioning system.
 * <p>
 * This is a drop-in replacement for the bundled dough implementation which
 * could not parse two-digit major versions or the {@code "26.1.2.build.2592-stable"}
 * {@code Bukkit.getBukkitVersion()} format.
 */
public class MinecraftVersion extends SemanticVersion {

    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    /**
     * This method constructs a new {@link MinecraftVersion} with the given
     * version components.
     *
     * @param major
     *            The "major" version (according to semver)
     * @param minor
     *            The "minor" version (according to semver)
     * @param patch
     *            The "patch" version (according to semver)
     */
    public MinecraftVersion(int major, int minor, int patch) {
        super(major, minor, patch);
    }

    /**
     * Private helper constructor for {@link #of(Server)}.
     *
     * @param version
     *            The parsed {@link SemanticVersion}
     */
    private MinecraftVersion(@Nonnull SemanticVersion version) {
        this(version.getMajorVersion(), version.getMinorVersion(), version.getPatchVersion());
    }

    /**
     * This attempts to get the {@link MinecraftVersion} on which the given {@link Server}
     * is currently running on.
     *
     * @param server
     *            The {@link Server} instance
     *
     * @return The current {@link MinecraftVersion}
     *
     * @throws UnknownServerVersionException
     *             This exception is thrown when the {@link Server} version could not be identified
     */
    public static @Nonnull MinecraftVersion of(@Nonnull Server server) throws UnknownServerVersionException {
        String bukkitVersion = server.getBukkitVersion();

        try {
            Matcher matcher = VERSION_PATTERN.matcher(bukkitVersion);

            if (matcher.find()) {
                int major = Integer.parseInt(matcher.group(1));
                int minor = Integer.parseInt(matcher.group(2));
                int patch = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
                return new MinecraftVersion(major, minor, patch);
            }

            throw new IllegalArgumentException("Could not find a version pattern in \"" + bukkitVersion + "\"");
        } catch (Exception x) {
            // Something failed.
            throw new UnknownServerVersionException(bukkitVersion, x);
        }
    }

    /**
     * This attempts to get the {@link MinecraftVersion} on which the current {@link Server}
     * is running on.
     *
     * @return The current {@link MinecraftVersion}
     *
     * @throws UnknownServerVersionException
     *             This exception is thrown when the {@link Server} version could not be identified
     */
    public static @Nonnull MinecraftVersion get() throws UnknownServerVersionException {
        return of(Bukkit.getServer());
    }

    /**
     * This checks if the current Server instance is a mock (MockBukkit) and
     * whether we are in a Unit Test environment.
     *
     * @param server
     *            The {@link Server} implementation to investigate
     *
     * @return Whether the current Server instance is a mock
     */
    public static boolean isMocked(@Nonnull Server server) {
        Class<?> clazz = server.getClass();

        while (clazz != null) {
            if (clazz.getName().endsWith("mockbukkit.ServerMock")) {
                return true;
            } else {
                clazz = clazz.getSuperclass();
            }
        }

        return false;
    }

    /**
     * This checks if the current Server instance is a mock (MockBukkit) and
     * whether we are in a Unit Test environment.
     *
     * @return Whether the current Server instance is a mock
     */
    public static boolean isMocked() {
        return isMocked(Bukkit.getServer());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @Nonnull String getAsString() {
        return "Minecraft " + super.getAsString();
    }

}
