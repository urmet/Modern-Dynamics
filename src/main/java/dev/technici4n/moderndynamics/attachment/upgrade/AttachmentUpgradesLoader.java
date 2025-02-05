/*
 * Modern Dynamics
 * Copyright (C) 2021 shartte & Technici4n
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package dev.technici4n.moderndynamics.attachment.upgrade;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import dev.technici4n.moderndynamics.ModernDynamics;
import dev.technici4n.moderndynamics.hooks.ResourceReloadFinished;
import dev.technici4n.moderndynamics.hooks.ServerSendPacketEvent;
import dev.technici4n.moderndynamics.util.MdId;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;

public class AttachmentUpgradesLoader extends SimplePreparableReloadListener<List<JsonObject>> implements IdentifiableResourceReloadListener {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    // A bit dirty... could maybe use a better fabric API hook?
    private static final Map<ResourceManager, LoadedUpgrades> LOADED_UPGRADES = new WeakHashMap<>();

    private AttachmentUpgradesLoader() {
    }

    @Override
    public ResourceLocation getFabricId() {
        return MdId.of("attachment_upgrades_loader");
    }

    @Override
    protected List<JsonObject> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        List<JsonObject> result = new ArrayList<>();

        for (var location : resourceManager.listResources("attachment_upgrades", s -> s.endsWith(".json"))) {
            try (var resource = resourceManager.getResource(location);
                    var inputStream = resource.getInputStream();
                    var reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                result.add(JsonParser.parseReader(reader).getAsJsonObject());
            } catch (IOException | JsonParseException | IllegalStateException exception) { // getAsJsonObject can throw ISE
                ModernDynamics.LOGGER.error("Error when loading Modern Dynamics attachment upgrade with path %s".formatted(location), exception);
            }
        }

        return result;
    }

    @Override
    protected void apply(List<JsonObject> array, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Item, UpgradeType> map = new IdentityHashMap<>();
        List<Item> list = new ArrayList<>();

        for (JsonObject obj : array) {
            if (!ResourceConditions.objectMatchesConditions(obj)) {
                continue;
            }

            try {
                var item = GsonHelper.getAsItem(obj, "item");
                var deserialized = GSON.fromJson(obj, UpgradeType.class);
                // TODO validate

                if (!map.containsKey(item)) {
                    list.add(item);
                }
                map.put(item, deserialized);
            } catch (Exception exception) {
                ModernDynamics.LOGGER.error("Failed to read attachment upgrade entry " + obj, exception);
            }
        }

        LOADED_UPGRADES.put(resourceManager, new LoadedUpgrades(map, list));
    }

    public static void setup() {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new AttachmentUpgradesLoader());
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LoadedUpgrades.upload(AttachmentUpgradesLoader.LOADED_UPGRADES.get(server.getResourceManager()));
        });
        ResourceReloadFinished.EVENT.register((server, resourceManager) -> {
            LoadedUpgrades.upload(AttachmentUpgradesLoader.LOADED_UPGRADES.get(resourceManager));

            // TODO: should maybe invalidate all cached filters?
        });
        ServerSendPacketEvent.EVENT.register((player, packet) -> {
            if (packet instanceof ClientboundUpdateRecipesPacket) {
                LoadedUpgrades.syncToClient(player);
            }
        });
    }
}
