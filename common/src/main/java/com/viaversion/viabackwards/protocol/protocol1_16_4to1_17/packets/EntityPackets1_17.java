/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2021 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.protocol1_16_4to1_17.packets;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.protocol1_16_4to1_17.Protocol1_16_4To1_17;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_16_2Types;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_17Types;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.metadata.MetaType;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.minecraft.metadata.types.MetaType1_14;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.Particle;
import com.viaversion.viaversion.api.type.types.version.Types1_14;
import com.viaversion.viaversion.api.type.types.version.Types1_17;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.protocol1_16_2to1_16_1.ClientboundPackets1_16_2;
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.ClientboundPackets1_17;

public class EntityPackets1_17 extends EntityRewriter<Protocol1_16_4To1_17> {

    public EntityPackets1_17(Protocol1_16_4To1_17 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        registerSpawnTrackerWithData(ClientboundPackets1_17.SPAWN_ENTITY, Entity1_16_2Types.FALLING_BLOCK);
        registerSpawnTracker(ClientboundPackets1_17.SPAWN_MOB);
        registerExtraTracker(ClientboundPackets1_17.SPAWN_EXPERIENCE_ORB, Entity1_16_2Types.EXPERIENCE_ORB);
        registerExtraTracker(ClientboundPackets1_17.SPAWN_PAINTING, Entity1_16_2Types.PAINTING);
        registerExtraTracker(ClientboundPackets1_17.SPAWN_PLAYER, Entity1_16_2Types.PLAYER);
        registerEntityDestroy(ClientboundPackets1_17.DESTROY_ENTITIES);
        registerMetadataRewriter(ClientboundPackets1_17.ENTITY_METADATA, Types1_17.METADATA_LIST, Types1_14.METADATA_LIST);
        protocol.registerOutgoing(ClientboundPackets1_17.JOIN_GAME, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // Entity ID
                map(Type.BOOLEAN); // Hardcore
                map(Type.UNSIGNED_BYTE); // Gamemode
                map(Type.BYTE); // Previous Gamemode
                map(Type.STRING_ARRAY); // Worlds
                map(Type.NBT); // Dimension registry
                map(Type.NBT); // Current dimension data
                handler(wrapper -> {
                    byte previousGamemode = wrapper.get(Type.BYTE, 0);
                    if (previousGamemode == -1) { // "Unset" gamemode removed
                        wrapper.set(Type.BYTE, 0, (byte) 0);
                    }
                });
                handler(getTrackerHandler(Entity1_16_2Types.PLAYER, Type.INT));
                handler(getWorldDataTracker(1));
                handler(wrapper -> {
                    CompoundTag registry = wrapper.get(Type.NBT, 0);
                    CompoundTag biomeRegsitry = registry.get("minecraft:worldgen/biome");
                    ListTag biomes = biomeRegsitry.get("value");
                    for (Tag biome : biomes) {
                        CompoundTag biomeCompound = ((CompoundTag) biome).get("element");
                        StringTag category = biomeCompound.get("category");
                        if (category.getValue().equalsIgnoreCase("underground")) {
                            category.setValue("none");
                        }
                    }

                    CompoundTag dimensionRegistry = registry.get("minecraft:dimension_type");
                    ListTag dimensions = dimensionRegistry.get("value");
                    for (Tag dimension : dimensions) {
                        CompoundTag dimensionCompound = ((CompoundTag) dimension).get("element");
                        reduceExtendedHeight(dimensionCompound, false);
                    }

                    reduceExtendedHeight(wrapper.get(Type.NBT, 1), true);
                });
            }
        });
        protocol.registerOutgoing(ClientboundPackets1_17.RESPAWN, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.NBT); // Dimension data
                handler(getWorldDataTracker(0));
                handler(wrapper -> {
                    reduceExtendedHeight(wrapper.get(Type.NBT, 0), true);
                });
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_17.PLAYER_POSITION, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.DOUBLE);
                map(Type.DOUBLE);
                map(Type.DOUBLE);
                map(Type.FLOAT);
                map(Type.FLOAT);
                map(Type.BYTE);
                map(Type.VAR_INT);
                handler(wrapper -> {
                    // Dismount vehicle ¯\_(ツ)_/¯
                    wrapper.read(Type.BOOLEAN);
                });
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_17.ENTITY_PROPERTIES, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // Entity id
                handler(wrapper -> {
                    wrapper.write(Type.INT, wrapper.read(Type.VAR_INT)); // Collection length
                });
            }
        });

        protocol.mergePacket(ClientboundPackets1_17.COMBAT_ENTER, ClientboundPackets1_16_2.COMBAT_EVENT, 0);
        protocol.mergePacket(ClientboundPackets1_17.COMBAT_END, ClientboundPackets1_16_2.COMBAT_EVENT, 1);
        protocol.mergePacket(ClientboundPackets1_17.COMBAT_KILL, ClientboundPackets1_16_2.COMBAT_EVENT, 2);
    }

    @Override
    protected void registerRewrites() {
        registerMetaHandler().handle(e -> {
            Metadata meta = e.getData();
            meta.setMetaType(MetaType1_14.byId(meta.getMetaType().getTypeID()));

            MetaType type = meta.getMetaType();
            if (type == MetaType1_14.Slot) {
                meta.setValue(protocol.getBlockItemPackets().handleItemToClient((Item) meta.getValue()));
            } else if (type == MetaType1_14.BlockID) {
                meta.setValue(protocol.getMappingData().getNewBlockStateId((int) meta.getValue()));
            } else if (type == MetaType1_14.OptChat) {
                JsonElement text = meta.getCastedValue();
                if (text != null) {
                    protocol.getTranslatableRewriter().processText(text);
                }
            } else if (type == MetaType1_14.PARTICLE) {
                Particle particle = (Particle) meta.getValue();
                if (particle.getId() == 15) { // Dust / Dust Transition
                    // Remove transition target color values 4-6
                    particle.getArguments().subList(4, 7).clear();
                } else if (particle.getId() == 36) { // Vibration Signal
                    // No nice mapping possible without tracking entity positions and doing particle tasks
                    particle.setId(0);
                    particle.getArguments().clear();
                    return meta;
                }

                rewriteParticle(particle);
            } else if (type == MetaType1_14.Pose) {
                // Goat LONG_JUMP added at 6
                int pose = meta.getCastedValue();
                if (pose == 6) {
                    meta.setValue(1); // FALL_FLYING
                } else if (pose > 6) {
                    meta.setValue(pose - 1);
                }
            }
            return meta;
        });

        mapTypes(Entity1_17Types.values(), Entity1_16_2Types.class);
        registerMetaHandler().filter(Entity1_17Types.AXOLOTL, 17).removed();
        registerMetaHandler().filter(Entity1_17Types.AXOLOTL, 18).removed();
        registerMetaHandler().filter(Entity1_17Types.AXOLOTL, 19).removed();

        registerMetaHandler().filter(Entity1_17Types.GLOW_SQUID, 16).removed();

        registerMetaHandler().filter(Entity1_17Types.GOAT, 16).removed();

        mapEntity(Entity1_17Types.AXOLOTL, Entity1_17Types.TROPICAL_FISH).jsonName("Axolotl");
        mapEntity(Entity1_17Types.GOAT, Entity1_17Types.SHEEP).jsonName("Goat");

        mapEntity(Entity1_17Types.GLOW_SQUID, Entity1_17Types.SQUID).jsonName("Glow Squid");
        mapEntity(Entity1_17Types.GLOW_ITEM_FRAME, Entity1_17Types.ITEM_FRAME);

        registerMetaHandler().filter(Entity1_17Types.SHULKER).handle(meta -> {
            if (meta.getIndex() >= 17) {
                meta.getData().setId(meta.getIndex() + 1); // TODO Handle attachment pos?
            }
            return meta.getData();
        });

        registerMetaHandler().filter(7).removed(); // Ticks frozen
        registerMetaHandler().handle(meta -> {
            if (meta.getIndex() > 7) {
                meta.getData().setId(meta.getIndex() - 1);
            }
            return meta.getData();
        });
    }

    @Override
    protected EntityType getTypeFromId(int typeId) {
        return Entity1_17Types.getTypeFromId(typeId);
    }

    private void reduceExtendedHeight(CompoundTag tag, boolean warn) {
        IntTag minY = tag.get("min_y");
        IntTag height = tag.get("height");
        IntTag logicalHeight = tag.get("logical_height");
        if (minY.asInt() != 0 || height.asInt() > 256 || logicalHeight.asInt() > 256) {
            if (warn) {
                ViaBackwards.getPlatform().getLogger().severe("Custom worlds heights are NOT SUPPORTED for 1.16 players and older and may lead to errors!");
                ViaBackwards.getPlatform().getLogger().severe("You have min/max set to " + minY.asInt() + "/" + height.asInt());
            }

            height.setValue(Math.min(256, height.asInt()));
            logicalHeight.setValue(Math.min(256, logicalHeight.asInt()));
        }
    }
}
