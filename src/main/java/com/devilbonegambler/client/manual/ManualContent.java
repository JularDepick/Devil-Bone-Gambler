/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.manual;

import com.devilbonegambler.DevilBoneGambler;
import com.devilbonegambler.manual.ManualKind;
import com.devilbonegambler.util.ModConstants;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

/**
 * Reads the markdown of a manual out of the mod resources, once per manual and language.
 * The content is shipped, never written and never generated at runtime, so a manual always reads the same text.
 */
public final class ManualContent
{
    private static final Map<String, List<ManualLine>> CACHE = new HashMap<String, List<ManualLine>>();

    private ManualContent()
    {
    }

    /**
     * Parsed lines of one manual in the language of the running client, never null.
     */
    public static List<ManualLine> lines(ManualKind kind)
    {
        String language = ManualKind.languageFor(Minecraft.getInstance().getLanguageManager().getSelected());
        String cacheKey = kind.itemId() + "/" + language;
        List<ManualLine> cached = CACHE.get(cacheKey);

        if (cached != null)
        {
            return cached;
        }

        List<ManualLine> parsed = ManualParser.parse(read(kind, language));
        CACHE.put(cacheKey, parsed);
        return parsed;
    }

    /**
     * Drops the cache, so a language change or a resource reload re-reads the files.
     */
    public static void clear()
    {
        CACHE.clear();
    }

    private static List<String> read(ManualKind kind, String language)
    {
        ResourceLocation location = ResourceLocation.tryParse(
                ModConstants.MOD_ID + ":" + kind.contentPath(language));

        if (location == null)
        {
            return missing();
        }

        try
        {
            Resource resource = Minecraft.getInstance().getResourceManager().getResource(location).orElse(null);

            if (resource == null)
            {
                DevilBoneGambler.LOGGER.warn("The manual content {} is missing", location);
                return missing();
            }

            try (InputStream stream = resource.open();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)))
            {
                List<String> lines = new ArrayList<String>();
                String line;

                while ((line = reader.readLine()) != null)
                {
                    lines.add(line);
                }

                return lines;
            }
        }
        catch (IOException exception)
        {
            DevilBoneGambler.LOGGER.warn("The manual content {} could not be read", location, exception);
            return missing();
        }
    }

    private static List<String> missing()
    {
        return List.of();
    }
}
