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
package dev.technici4n.moderndynamics.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.technici4n.moderndynamics.attachment.RenderedAttachment;
import dev.technici4n.moderndynamics.init.MdBlocks;
import dev.technici4n.moderndynamics.pipe.PipeBlock;
import dev.technici4n.moderndynamics.util.MdId;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.HashCache;

public class PipeModelsProvider implements DataProvider {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final FabricDataGenerator gen;

    public PipeModelsProvider(FabricDataGenerator gen) {
        this.gen = gen;
    }

    @Override
    public void run(HashCache cache) throws IOException {
        registerPipeModels(cache);
        registerAttachments(cache);
    }

    private void registerPipeModels(HashCache cache) throws IOException {
        registerPipeModel(cache, MdBlocks.ITEM_PIPE, "base/item/basic", "connector/iron", true);
        registerPipeModel(cache, MdBlocks.FLUID_PIPE, "base/fluid/basic", "connector/copper", true);

        /*
         * registerPipeModel(cache, MdBlocks.BASIC_ITEM_PIPE_OPAQUE, "base/item/basic_opaque", "connector/tin", false);
         * registerPipeModel(cache, MdBlocks.FAST_ITEM_PIPE, "lead", "connection_lead");
         * registerPipeModel(cache, MdBlocks.FAST_ITEM_PIPE_OPAQUE, "lead", "connection_lead");
         * registerPipeModel(cache, MdBlocks.CONDUCTIVE_ITEM_PIPE, "lead", "connection_lead");
         * registerPipeModel(cache, MdBlocks.CONDUCTIVE_ITEM_PIPE_OPAQUE, "lead", "connection_lead");
         * registerPipeModel(cache, MdBlocks.CONDUCTIVE_FAST_ITEM_PIPE, "lead", "connection_lead");
         * registerPipeModel(cache, MdBlocks.CONDUCTIVE_FAST_ITEM_PIPE_OPAQUE, "lead", "connection_lead");
         * 
         * registerPipeModel(cache, MdBlocks.BASIC_FLUID_PIPE_OPAQUE, "lead", "connection_lead");
         * registerPipeModel(cache, MdBlocks.FAST_FLUID_PIPE, "lead", "connection_lead");
         * registerPipeModel(cache, MdBlocks.FAST_FLUID_PIPE_OPAQUE, "lead", "connection_lead");
         * registerPipeModel(cache, MdBlocks.CONDUCTIVE_FLUID_PIPE, "lead", "connection_lead");
         * registerPipeModel(cache, MdBlocks.CONDUCTIVE_FLUID_PIPE_OPAQUE, "lead", "connection_lead");
         * registerPipeModel(cache, MdBlocks.CONDUCTIVE_FAST_FLUID_PIPE, "lead", "connection_lead");
         * registerPipeModel(cache, MdBlocks.CONDUCTIVE_FAST_FLUID_PIPE_OPAQUE, "lead", "connection_lead");
         * 
         * registerPipeModel(cache, MdBlocks.BASIC_ENERGY_PIPE, "base/energy/lead", "connector/lead");
         * registerPipeModel(cache, MdBlocks.IMPROVED_ENERGY_PIPE, "base/energy/invar", "connector/invar");
         * registerPipeModel(cache, MdBlocks.ADVANCED_ENERGY_PIPE, "base/energy/electrum", "connector/electrum");
         * 
         * registerPipeModel(cache, MdBlocks.EMPTY_REINFORCED_ENERGY_PIPE, "lead", "connection_lead");
         * registerPipeModel(cache, MdBlocks.EMPTY_SIGNALUM_ENERGY_PIPE, "lead", "connection_lead");
         * registerPipeModel(cache, MdBlocks.EMPTY_RESONANT_ENERGY_PIPE, "lead", "connection_lead");
         * registerPipeModel(cache, MdBlocks.EMPTY_SUPERCONDUCTING_PIPE, "lead", "connection_lead");
         */
    }

    private void registerPipeModel(HashCache cache, PipeBlock pipe, String texture, String connectionTexture, boolean transparent)
            throws IOException {
        var baseFolder = gen.getOutputFolder().resolve("assets/%s/models/pipe/%s".formatted(gen.getModId(), pipe.id));

        var noneModel = registerPipePart(cache, baseFolder, pipe, "none", texture, transparent);
        var inventoryModel = registerPipePart(cache, baseFolder, pipe, "inventory", connectionTexture, transparent);
        var pipeModel = registerPipePart(cache, baseFolder, pipe, "pipe", texture, transparent);

        var modelJson = new JsonObject();
        modelJson.addProperty("connection_none", noneModel);
        modelJson.addProperty("connection_inventory", inventoryModel);
        modelJson.addProperty("connection_pipe", pipeModel);
        DataProvider.save(GSON, cache, modelJson, baseFolder.resolve("main.json"));
    }

    /**
     * Register a simple textures pipe part model, and return the id of the model.
     */
    private String registerPipePart(HashCache cache, Path baseFolder, PipeBlock pipe, String kind, String texture, boolean transparentSuffix)
            throws IOException {
        var obj = new JsonObject();
        obj.addProperty("parent", MdId.of("base/pipe_%s%s".formatted(kind, transparentSuffix ? "_transparent" : "")).toString());
        var textures = new JsonObject();
        obj.add("textures", textures);
        textures.addProperty("0", MdId.of(texture).toString());

        DataProvider.save(GSON, cache, obj, baseFolder.resolve(kind + ".json"));

        var id = "pipe/%s/%s".formatted(pipe.id, kind);
        return MdId.of(id).toString();
    }

    private void registerAttachments(HashCache cache) throws IOException {
        // Register each model.
        for (var attachment : RenderedAttachment.getAllAttachments()) {
            registerAttachment(cache, attachment, "attachment/" + attachment.id.toLowerCase(Locale.ROOT));
        }
    }

    /**
     * Register a simple attachment part model, and return the id of the model.
     */
    private void registerAttachment(HashCache cache, RenderedAttachment attachment, String texture) throws IOException {
        var obj = new JsonObject();
        obj.addProperty("parent", MdId.of("base/pipe_inventory_transparent").toString());
        var textures = new JsonObject();
        obj.add("textures", textures);
        textures.addProperty("0", MdId.of(texture).toString());

        DataProvider.save(GSON, cache, obj,
                gen.getOutputFolder().resolve("assets/%s/models/attachment/%s.json".formatted(gen.getModId(), attachment.id)));
    }

    @Override
    public String getName() {
        return "Pipe Models";
    }
}
