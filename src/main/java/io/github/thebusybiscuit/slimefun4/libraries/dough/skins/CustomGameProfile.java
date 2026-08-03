package io.github.thebusybiscuit.slimefun4.libraries.dough.skins;

import java.net.URL;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

/**
 * A lightweight {@link PlayerProfile} factory for custom head textures.
 * <p>
 * The original dough implementation extended {@code com.mojang.authlib.GameProfile}.
 * Since MC 26.x that class is final, so we wrap the necessary data instead and build
 * a {@link PlayerProfile} through the pure Bukkit API (no NMS / authlib required).
 */
public final class CustomGameProfile {

    /**
     * The player name for this profile.
     * "CS-CoreLib" for historical reasons and backwards compatibility.
     */
    private static final String PLAYER_NAME = "CS-CoreLib";

    private final UUID uniqueId;
    private final String texture;
    private final URL skinUrl;

    CustomGameProfile(@Nonnull UUID uuid, @Nullable String texture, @Nonnull URL url) {
        this.uniqueId = uuid;
        this.texture = texture;
        this.skinUrl = url;
    }

    /**
     * This creates a {@link PlayerProfile} with this skin applied.
     *
     * @return A {@link PlayerProfile} holding this skin
     */
    public @Nonnull PlayerProfile toPlayerProfile() {
        PlayerProfile profile = Bukkit.createPlayerProfile(uniqueId, PLAYER_NAME);
        PlayerTextures textures = profile.getTextures();
        textures.setSkin(skinUrl);
        profile.setTextures(textures);
        return profile;
    }

    /**
     * Get the base64 encoded texture for this profile.
     *
     * @return the base64 encoded texture.
     */
    @Nullable
    public String getBase64Texture() {
        return this.texture;
    }
}
