/*
 * Devil Bone Gambler
 * Copyright (c) 2026 JularDepick
 * Author: JularDepick (https://github.com/JularDepick)
 */

package com.devilbonegambler.client.manual;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.Font;

/**
 * The small markdown subset the manuals are written in.
 * Supported: headings of three levels, bullet lists, numbered lists, bold, inline code, separator rules and blank
 * lines. Anything else is a plain paragraph, so an unsupported construct still reads as its own text.
 * The parser also wraps the parsed lines into the rows a page draws, which is why it needs the font and the page
 * width.
 */
public final class ManualParser
{
    private static final Pattern NUMBERED = Pattern.compile("^(\\d+)\\.\\s+(.*)$");
    private static final String BULLET_MARKER = "- ";

    private ManualParser()
    {
    }

    /**
     * Parses the markdown lines of one manual.
     */
    public static List<ManualLine> parse(List<String> rawLines)
    {
        List<ManualLine> lines = new ArrayList<ManualLine>();

        for (String raw : rawLines)
        {
            String line = raw == null ? "" : raw.strip();

            if (line.isEmpty())
            {
                lines.add(ManualLine.spacer());
                continue;
            }

            if (line.equals("---") || line.equals("***") || line.equals("___"))
            {
                lines.add(ManualLine.separator());
                continue;
            }

            if (line.startsWith("### "))
            {
                lines.add(ManualLine.of(ManualLine.Block.MINOR, "", inline(line.substring(4))));
                continue;
            }

            if (line.startsWith("## "))
            {
                lines.add(ManualLine.of(ManualLine.Block.SUBHEADING, "", inline(line.substring(3))));
                continue;
            }

            if (line.startsWith("# "))
            {
                lines.add(ManualLine.of(ManualLine.Block.HEADING, "", inline(line.substring(2))));
                continue;
            }

            if (line.startsWith(BULLET_MARKER) || line.startsWith("* "))
            {
                lines.add(ManualLine.of(ManualLine.Block.BULLET, "", inline(line.substring(2))));
                continue;
            }

            Matcher numbered = NUMBERED.matcher(line);

            if (numbered.matches())
            {
                lines.add(ManualLine.of(ManualLine.Block.NUMBER, numbered.group(1) + ".",
                        inline(numbered.group(2))));
                continue;
            }

            lines.add(ManualLine.of(ManualLine.Block.PARAGRAPH, "", inline(line)));
        }

        return lines;
    }

    /**
     * Splits bold and inline code out of one line.
     */
    private static List<ManualText> inline(String text)
    {
        List<ManualText> parts = new ArrayList<ManualText>();
        StringBuilder plain = new StringBuilder();
        int index = 0;

        while (index < text.length())
        {
            if (text.startsWith("**", index))
            {
                int end = text.indexOf("**", index + 2);

                if (end > index)
                {
                    flush(plain, parts);
                    parts.add(new ManualText(text.substring(index + 2, end), ManualText.Style.BOLD));
                    index = end + 2;
                    continue;
                }
            }

            if (text.charAt(index) == '\u0060')
            {
                int end = text.indexOf('\u0060', index + 1);

                if (end > index)
                {
                    flush(plain, parts);
                    parts.add(new ManualText(text.substring(index + 1, end), ManualText.Style.CODE));
                    index = end + 1;
                    continue;
                }
            }

            plain.append(text.charAt(index));
            index++;
        }

        flush(plain, parts);
        return parts;
    }

    private static void flush(StringBuilder plain, List<ManualText> parts)
    {
        if (plain.length() > 0)
        {
            parts.add(new ManualText(plain.toString(), ManualText.Style.BODY));
            plain.setLength(0);
        }
    }

    /**
     * Wraps parsed lines into the rows of a page.
     */
    public static List<ManualRow> layout(List<ManualLine> lines, Font font, int width)
    {
        List<ManualRow> rows = new ArrayList<ManualRow>();

        for (ManualLine line : lines)
        {
            if (line.block() == ManualLine.Block.SEPARATOR)
            {
                rows.add(ManualRow.separator());
                continue;
            }

            if (line.block() == ManualLine.Block.SPACER)
            {
                rows.add(ManualRow.spacer());
                continue;
            }

            wrap(line, font, Math.max(1, width), rows);
        }

        return rows;
    }

    private static void wrap(ManualLine line, Font font, int width, List<ManualRow> rows)
    {
        String marker = markerOf(line, font);
        int markerWidth = marker.isEmpty() ? 0 : font.width(marker) + font.width(" ");
        ManualRow current = ManualRow.of(line.block(), marker, 0, new ArrayList<ManualText>());
        int available = width - markerWidth;

        for (ManualText part : line.texts())
        {
            String text = part.text();

            while (!text.isEmpty())
            {
                int room = available - widthOf(current, font);
                String fit = fitPrefix(text, room, font);

                if (fit.isEmpty())
                {
                    if (!current.isEmpty())
                    {
                        rows.add(current);
                        current = ManualRow.of(line.block(), "", markerWidth, new ArrayList<ManualText>());
                        available = width - markerWidth;
                        continue;
                    }

                    /* A single character never fits the remaining room, so it is placed anyway to keep progressing. */
                    fit = text.substring(0, 1);
                }

                current = current.withText(new ManualText(fit, part.style()));
                text = text.substring(fit.length());

                if (!text.isEmpty())
                {
                    rows.add(current);
                    current = ManualRow.of(line.block(), "", markerWidth, new ArrayList<ManualText>());
                    available = width - markerWidth;
                }
            }
        }

        if (!current.isEmpty())
        {
            rows.add(current);
        }
    }

    private static String markerOf(ManualLine line, Font font)
    {
        if (line.block() == ManualLine.Block.BULLET)
        {
            return "-";
        }

        if (line.block() == ManualLine.Block.NUMBER)
        {
            return line.marker();
        }

        return "";
    }

    private static int widthOf(ManualRow row, Font font)
    {
        int width = 0;

        for (ManualText text : row.texts())
        {
            width += font.width(text.text());
        }

        return width;
    }

    /**
     * Longest prefix of the text that still fits the room, preferring the last space so Latin words stay whole.
     */
    private static String fitPrefix(String text, int room, Font font)
    {
        if (room <= 0)
        {
            return "";
        }

        if (font.width(text) <= room)
        {
            return text;
        }

        int cut = 0;
        int width = 0;

        for (int index = 0; index < text.length(); index++)
        {
            int characterWidth = font.width(String.valueOf(text.charAt(index)));

            if (width + characterWidth > room)
            {
                break;
            }

            width += characterWidth;
            cut = index + 1;
        }

        if (cut <= 0)
        {
            return "";
        }

        int space = text.lastIndexOf(' ', cut - 1);

        if (space > cut / 2)
        {
            return text.substring(0, space + 1).stripTrailing();
        }

        return text.substring(0, cut);
    }
}
