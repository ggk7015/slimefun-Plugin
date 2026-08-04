package io.github.thebusybiscuit.slimefun4.libraries.paperlib.features.blockstatesnapshot;

import javax.annotation.Nonnull;

import org.bukkit.block.BlockState;

/**
 * Drop-in stub for {@code io.papermc.lib.features.blockstatesnapshot.BlockStateSnapshotResult}.
 * <p>
 * Wraps a {@link BlockState} obtained from the native Bukkit API.
 */
public interface BlockStateSnapshotResult {

    /**
     * Returns the requested {@link BlockState}.
     *
     * @return The requested {@link BlockState}
     */
    @Nonnull
    BlockState getState();

    /**
     * Applies this snapshot. In this implementation the state is already live,
     * so nothing has to be applied.
     */
    void run();
}
