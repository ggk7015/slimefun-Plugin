package io.github.thebusybiscuit.slimefun4.libraries.dough.versions;

/**
 * This exception is thrown when the Minecraft version could not be identified.
 * <p>
 * Drop-in replacement for the bundled dough implementation to allow
 * construction from the relocated package.
 */
public class UnknownServerVersionException extends Exception {

    private static final long serialVersionUID = -5932282005937704971L;

    public UnknownServerVersionException(String version, Exception x) {
        super("Could not recognize version string: " + version, x);
    }

}
