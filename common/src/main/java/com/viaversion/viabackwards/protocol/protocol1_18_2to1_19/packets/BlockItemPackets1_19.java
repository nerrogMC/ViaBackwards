/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2022 ViaVersion and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.viaversion.viabackwards.protocol.protocol1_18_2to1_19.packets;

import com.viaversion.viabackwards.api.rewriters.ItemRewriter;
import com.viaversion.viabackwards.protocol.protocol1_18_2to1_19.Protocol1_18_2To1_19;
import com.viaversion.viaversion.api.data.ParticleMappings;
import com.viaversion.viaversion.api.data.entity.EntityTracker;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.minecraft.chunks.DataPalette;
import com.viaversion.viaversion.api.minecraft.chunks.PaletteType;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.protocols.protocol1_16to1_15_2.data.RecipeRewriter1_16;
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.ServerboundPackets1_17;
import com.viaversion.viaversion.protocols.protocol1_18to1_17_1.types.Chunk1_18Type;
import com.viaversion.viaversion.protocols.protocol1_19to1_18_2.ClientboundPackets1_19;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.util.MathUtil;

public final class BlockItemPackets1_19 extends ItemRewriter<Protocol1_18_2To1_19> {

    public BlockItemPackets1_19(final Protocol1_18_2To1_19 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        final BlockRewriter blockRewriter = new BlockRewriter(protocol, Type.POSITION1_14);

        new RecipeRewriter1_16(protocol).registerDefaultHandler(ClientboundPackets1_19.DECLARE_RECIPES);

        registerSetCooldown(ClientboundPackets1_19.COOLDOWN);
        registerWindowItems1_17_1(ClientboundPackets1_19.WINDOW_ITEMS, Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT, Type.FLAT_VAR_INT_ITEM);
        registerSetSlot1_17_1(ClientboundPackets1_19.SET_SLOT, Type.FLAT_VAR_INT_ITEM);
        registerEntityEquipmentArray(ClientboundPackets1_19.ENTITY_EQUIPMENT, Type.FLAT_VAR_INT_ITEM);
        registerAdvancements(ClientboundPackets1_19.ADVANCEMENTS, Type.FLAT_VAR_INT_ITEM);
        registerClickWindow1_17_1(ServerboundPackets1_17.CLICK_WINDOW, Type.FLAT_VAR_INT_ITEM);

        blockRewriter.registerBlockAction(ClientboundPackets1_19.BLOCK_ACTION);
        blockRewriter.registerBlockChange(ClientboundPackets1_19.BLOCK_CHANGE);
        blockRewriter.registerVarLongMultiBlockChange(ClientboundPackets1_19.MULTI_BLOCK_CHANGE);
        blockRewriter.registerEffect(ClientboundPackets1_19.EFFECT, 1010, 2001);

        registerCreativeInvAction(ServerboundPackets1_17.CREATIVE_INVENTORY_ACTION, Type.FLAT_VAR_INT_ITEM);

        protocol.registerClientbound(ClientboundPackets1_19.TRADE_LIST, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // Container id
                handler(wrapper -> {
                    final int size = wrapper.read(Type.VAR_INT);
                    wrapper.write(Type.UNSIGNED_BYTE, (short) size);
                    for (int i = 0; i < size; i++) {
                        handleItemToClient(wrapper.passthrough(Type.FLAT_VAR_INT_ITEM)); // First item
                        handleItemToClient(wrapper.passthrough(Type.FLAT_VAR_INT_ITEM)); // Result

                        final Item secondItem = wrapper.read(Type.FLAT_VAR_INT_ITEM);
                        if (secondItem != null) {
                            handleItemToClient(secondItem);
                            wrapper.write(Type.BOOLEAN, true);
                            wrapper.write(Type.FLAT_VAR_INT_ITEM, secondItem);
                        } else {
                            wrapper.write(Type.BOOLEAN, false);
                        }

                        wrapper.passthrough(Type.BOOLEAN); // Out of stock
                        wrapper.passthrough(Type.INT); // Uses
                        wrapper.passthrough(Type.INT); // Max uses
                        wrapper.passthrough(Type.INT); // Xp
                        wrapper.passthrough(Type.INT); // Special price diff
                        wrapper.passthrough(Type.FLOAT); // Price multiplier
                        wrapper.passthrough(Type.INT); //Demand
                    }
                });
            }
        });

        registerWindowPropertyEnchantmentHandler(ClientboundPackets1_19.WINDOW_PROPERTY);

        protocol.registerClientbound(ClientboundPackets1_19.BLOCK_CHANGED_ACK, null, new PacketRemapper() {
            @Override
            public void registerMap() {
                read(Type.VAR_INT); // Sequence
                handler(PacketWrapper::cancel); // This is fine:tm:
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19.SPAWN_PARTICLE, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT, Type.INT); // Particle id
                map(Type.BOOLEAN); // Override limiter
                map(Type.DOUBLE); // X
                map(Type.DOUBLE); // Y
                map(Type.DOUBLE); // Z
                map(Type.FLOAT); // Offset X
                map(Type.FLOAT); // Offset Y
                map(Type.FLOAT); // Offset Z
                map(Type.FLOAT); // Max speed
                map(Type.INT); // Particle Count
                handler(wrapper -> {
                    final int id = wrapper.get(Type.INT, 0);
                    final ParticleMappings particleMappings = protocol.getMappingData().getParticleMappings();
                    if (id == particleMappings.id("sculk_charge")) {
                        //TODO
                        wrapper.set(Type.INT, 0, -1);
                        wrapper.cancel();
                    } else if (id == particleMappings.id("shriek")) {
                        //TODO
                        wrapper.set(Type.INT, 0, -1);
                        wrapper.cancel();
                    } else if (id == particleMappings.id("vibration")) {
                        // Can't do without the position
                        wrapper.set(Type.INT, 0, -1);
                        wrapper.cancel();
                    }
                });
                handler(getSpawnParticleHandler(Type.FLAT_VAR_INT_ITEM));
            }
        });


        protocol.registerClientbound(ClientboundPackets1_19.CHUNK_DATA, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    final EntityTracker tracker = protocol.getEntityRewriter().tracker(wrapper.user());
                    final Chunk1_18Type chunkType = new Chunk1_18Type(tracker.currentWorldSectionHeight(),
                            MathUtil.ceilLog2(protocol.getMappingData().getBlockStateMappings().mappedSize()),
                            MathUtil.ceilLog2(tracker.biomesSent()));
                    final Chunk chunk = wrapper.passthrough(chunkType);
                    for (final ChunkSection section : chunk.getSections()) {
                        final DataPalette blockPalette = section.palette(PaletteType.BLOCKS);
                        for (int i = 0; i < blockPalette.size(); i++) {
                            final int id = blockPalette.idByIndex(i);
                            blockPalette.setIdByIndex(i, protocol.getMappingData().getNewBlockStateId(id));
                        }
                    }
                });
            }
        });

        // The server does nothing but track the sequence, so we can just set it as 0
        protocol.registerServerbound(ServerboundPackets1_17.PLAYER_DIGGING, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // Action
                map(Type.POSITION1_14); // Block position
                map(Type.UNSIGNED_BYTE); // Direction
                create(Type.VAR_INT, 0); // Sequence
            }
        });
        protocol.registerServerbound(ServerboundPackets1_17.PLAYER_BLOCK_PLACEMENT, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // Hand
                map(Type.POSITION1_14); // Block position
                map(Type.VAR_INT); // Direction
                map(Type.FLOAT); // X
                map(Type.FLOAT); // Y
                map(Type.FLOAT); // Z
                map(Type.BOOLEAN); // Inside
                create(Type.VAR_INT, 0); // Sequence
            }
        });
        protocol.registerServerbound(ServerboundPackets1_17.USE_ITEM, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // Hand
                create(Type.VAR_INT, 0); // Sequence
            }
        });

        protocol.registerServerbound(ServerboundPackets1_17.SET_BEACON_EFFECT, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    final int primaryEffect = wrapper.read(Type.VAR_INT);
                    if (primaryEffect != -1) {
                        wrapper.write(Type.BOOLEAN, true);
                        wrapper.write(Type.VAR_INT, primaryEffect);
                    } else {
                        wrapper.write(Type.BOOLEAN, false);
                    }

                    final int secondaryEffect = wrapper.read(Type.VAR_INT);
                    if (secondaryEffect != -1) {
                        wrapper.write(Type.BOOLEAN, true);
                        wrapper.write(Type.VAR_INT, secondaryEffect);
                    } else {
                        wrapper.write(Type.BOOLEAN, false);
                    }
                });
            }
        });
    }
}
