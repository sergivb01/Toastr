package services.vortex.toastr.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

public class Config {

    private static final Gson gson = new Gson();

    private final Object plugin;
    private final File file;
    @Getter
    private JsonObject object;

    public Config(Object plugin, Path folder, String name) throws IOException {
        this.plugin = plugin;
        File folderFile = folder.toFile();
        if(!folderFile.exists())
            folderFile.mkdir();

        file = new File(folderFile, name + ".json");
        if(!file.exists())
            createFile();

        reload();
    }

    /**
     * This method reloads the Config
     *
     * @throws FileNotFoundException When the file doesn't exist
     */
    public void reload() throws FileNotFoundException {
        object = gson.fromJson(new FileReader(file), JsonObject.class);
    }

    /**
     * This method loads a Component using MiniMessage
     *
     * @param name         The message name
     * @param placeholders The placeholders
     * @return The Component
     */
    public Component getMessage(String name, String... placeholders) {
        return MiniMessage.get().parse(object.getAsJsonObject("messages").get(name).getAsString(), placeholders);
    }

    public Collection<Component> getMessages(String name, String... placeholders) {
        Collection<Component> res = new ArrayList<>();

        final JsonArray messages = object.getAsJsonObject("messages").get(name).getAsJsonArray();
        for(JsonElement msg : messages) {
            res.add(MiniMessage.get().parse(msg.getAsString(), placeholders));
        }

        return res;
    }

    private void createFile() throws IOException {
        InputStream stream = plugin.getClass().getClassLoader().getResourceAsStream(file.getName());

        file.createNewFile();

        byte[] buffer = new byte[stream.available()];
        stream.read(buffer);

        new FileOutputStream(file).write(buffer);
    }

}
