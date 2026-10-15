/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.block;

import com.devilbonegambler.registry.ModBlockEntities;
import com.devilbonegambler.util.ModConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity of the devil's altar.
 * It owns a single persistent value, the accumulated offering value, and keeps the client copy
 * in sync because the range mask rendering needs to read it.
 */
public class DevilsAltarBlockEntity extends BlockEntity
{
    private long offeringValue;

    public DevilsAltarBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.DEVILS_ALTAR.get(), pos, state);
    }

    /**
     * Adds an offering value and clamps the total to the configured maximum.
     * The change is marked dirty and pushed to the tracking clients.
     */
    public void addValue(long value)
    {
        long total = this.offeringValue + Math.max(0L, value);

        if (total < this.offeringValue)
        {
            total = Long.MAX_VALUE;
        }

        this.offeringValue = Math.min(ModConstants.ALTAR_MAX_VALUE, total);
        setChanged();

        if (this.level != null)
        {
            this.level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    /**
     * Overwrites the stored value, used when a carrying item is placed back into the world.
     */
    public void setOfferingValue(long value)
    {
        this.offeringValue = Math.min(ModConstants.ALTAR_MAX_VALUE, Math.max(0L, value));
        setChanged();
    }

    public long offeringValue()
    {
        return this.offeringValue;
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.putLong(ModConstants.ALTAR_VALUE_TAG, this.offeringValue);
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        this.offeringValue = Math.min(ModConstants.ALTAR_MAX_VALUE,
                Math.max(0L, tag.getLong(ModConstants.ALTAR_VALUE_TAG)));
    }

    @Override
    public CompoundTag getUpdateTag()
    {
        return saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
