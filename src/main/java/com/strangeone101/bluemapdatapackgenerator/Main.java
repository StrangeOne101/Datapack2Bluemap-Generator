package com.strangeone101.bluemapdatapackgenerator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.nio.charset.Charset;
import java.nio.file.Files;


public class Main {


    public static void main(String[] args) {
        String[] options = {"Continue", "Cancel"};

        File input = new File("DatapackInput");
        if (!input.exists()) input.mkdir();

        int n = JOptionPane.showOptionDialog(null, "Please unzip your datapack into the created 'DatapackInput' \nfolder! " +
                "The pack.mcmeta should be within this folder and\n not in and sub-folder!", "Bluemap Datapack Generator",
                JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

        if (n == JOptionPane.NO_OPTION) {
            System.out.println("User exited the program");
            System.exit(0);
            return;
        }

        File mcmeta = new File(input, "pack.mcmeta");
        while (!mcmeta.exists()) {
            System.out.println("Failed to locate pack.mcmeta at " + mcmeta.getAbsolutePath());
            options = new String[] {"Try Again", "Cancel"};
            n = JOptionPane.showOptionDialog(null, "pack.mcmeta not found! Be sure you unzip the datapack as" +
                            "\na whole into the DatapackInput folder The pack.mcmeta cannot\nbe in any sub-folder, either!", "Bluemap Datapack Generator",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);

            if (n == JOptionPane.NO_OPTION) {
                System.out.println("User exited the program.");
                System.exit(0);
                return;
            }
        }

        String pack = readMCMeta(mcmeta);
        System.out.println("Found datapack " + pack);

        JsonObject output = new JsonObject();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try {
            for (File namespace : new File(input, "data").listFiles(File::isDirectory)) {
                File biomeFolder = new File(namespace, "worldgen" + File.separator + "biome");
                for (File biomeFile : biomeFolder.listFiles()) {
                    if (biomeFile.getName().endsWith(".json")) {
                        Biome biome = gson.fromJson(new FileReader(biomeFile), Biome.class);
                        OutputBiome outBiome = new OutputBiome();
                        outBiome.foliagecolor = biome.effects.foliage_color;
                        outBiome.grasscolor = biome.effects.grass_color;
                        outBiome.watercolor = biome.effects.water_color;
                        outBiome.humidity = biome.downfall;
                        outBiome.temperature = biome.temperature;
                        String name = namespace.getName() + ":" + biomeFile.getName().replace(".json", "");
                        output.add(name, gson.toJsonTree(outBiome));
                        System.out.println("Read biome data for biome " + name);
                    }
                }
            }
            System.out.println("Done reading biomes!");

            File outputFile = new File("output.json");
            String outString = gson.toJson(output);
            Files.writeString(outputFile.toPath(), outString, Charset.defaultCharset());
            System.out.println("Written all biome data to output.json");

            JOptionPane.showMessageDialog(null, "Done! Created all biome files for " + pack, "Bluemap Datapack Generator",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showConfirmDialog(null, e.getMessage(), "Bluemap Datapack Generator - Error",
                    JOptionPane.ERROR_MESSAGE);
        }


    }

    public static String readMCMeta(File file) {
        try {
            Gson gson = new Gson();
            PackMeta meta = gson.fromJson(new FileReader(file), PackMeta.class);

            return meta.pack.description;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Unknown";
    }
}
