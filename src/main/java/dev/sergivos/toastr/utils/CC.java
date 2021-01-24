package dev.sergivos.toastr.utils;

import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParseException;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class CC {
    private static final MiniMessage fancy = MiniMessage.get();
    private static final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();

    /**
     * Translates a text replacing the pair of placeholders
     *
     * @param text         The text to be translated. It will use AdventureAPI if it starts with a '#'
     * @param placeholders A list of placeholders to be replaced
     * @return The translated component and with the replaced placeholders
     */
    @NonNull
    public static Component translate(@NonNull String text, final String... placeholders) {
        if(text.charAt(0) == '#' && text.length() > 1) {
            return fancy.parse(text.substring(1), placeholders);
        }

        if(placeholders.length % 2 != 0) {
            throw new ParseException("Invalid number placeholders defined, usage: parseFormat(format, key, value, key, value...)");
        } else {
            for(int i = 0; i < placeholders.length; i += 2) {
                text = text.replace("<" + placeholders[i] + ">", placeholders[i + 1]);
            }
            return legacy.deserialize(text);
        }
    }

    /**
     * Translates with a suggestion to copy to clipboard via Click & Hover event
     * Reference {@link #translate(String, String...)} )}
     *
     * @param text         The text to be translated.
     * @param suggest      The text to be suggested to copy
     * @param placeholders A list of placeholders to be replaced.
     * @return The translated component and with the replaced placeholders and the click & hover action
     */
    @NonNull
    public static Component suggest(@NonNull String text, @NonNull final String suggest, final String... placeholders) {
        return translate(text, placeholders)
                .clickEvent(ClickEvent.suggestCommand(suggest))
                .hoverEvent(HoverEvent.showText(CC.translate("&7Click to copy")));
    }

}
