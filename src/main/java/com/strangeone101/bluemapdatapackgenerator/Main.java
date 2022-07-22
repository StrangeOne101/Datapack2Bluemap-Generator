package com.strangeone101.bluemapdatapackgenerator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


public class Main {


    public static void main(String[] args) {
        String[] options = {"Continue", "Cancel"};

        File input = new File("DatapackInput");
        if (!input.exists()) input.mkdir();

        int n = JOptionPane.showOptionDialog(null, "Please unzip your datapack into the created 'DatapackInput' \nfolder! " +
                "The pack.mcmeta should be within this folder and\nnot in and sub-folder!", "Bluemap Datapack Generator",
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
        int amount = 0;

        try {
            for (File namespace : new File(input, "data").listFiles(File::isDirectory)) {
                File biomeFolder = new File(namespace, "worldgen" + File.separator + "biome");
                if (biomeFolder == null || !biomeFolder.exists()) continue;
                for (File biomeFile : biomeFolder.listFiles()) {
                    if (biomeFile.getName().endsWith(".json")) {
                        Biome biome = gson.fromJson(new FileReader(biomeFile), Biome.class);
                        OutputBiome outBiome = new OutputBiome();
                        outBiome.foliagecolor = biome.effects.foliage_color != 0 ? biome.effects.foliage_color : null;
                        outBiome.grasscolor = biome.effects.grass_color != 0 ? biome.effects.grass_color : null;
                        outBiome.watercolor = biome.effects.water_color != 0 ? biome.effects.water_color : null;
                        outBiome.humidity = biome.downfall;
                        outBiome.temp = biome.temperature;
                        String name = namespace.getName() + ":" + biomeFile.getName().replace(".json", "");
                        output.add(name, gson.toJsonTree(outBiome));
                        amount++;
                        System.out.println("Read biome data for biome " + name);
                    }
                }
            }
            System.out.println("Done reading biomes!");

            String shortDatapackName = pack;
            if (shortDatapackName.contains("|")) shortDatapackName = shortDatapackName.split("\\|")[0];
            else if (shortDatapackName.contains("-")) shortDatapackName = shortDatapackName.split("-")[0];
            else if (shortDatapackName.contains(".")) shortDatapackName = shortDatapackName.split("\\.")[0];

            shortDatapackName = shortDatapackName.trim();

            if (shortDatapackName.length() == 0) shortDatapackName = pack; //Something didn't go right so just forget making a short name
            else if (shortDatapackName.length() > 30) shortDatapackName = shortDatapackName.split(" ")[0]; //If the name is too long, just take the first word

            shortDatapackName = shortDatapackName.replaceAll(" ", "_");

            File zip = new File("BiomeColors_(" + shortDatapackName + ").zip");

            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip));
            ZipEntry e = new ZipEntry("assets/minecraft/bluemap/biomes.json");
            out.putNextEntry(e);

            byte[] data = gson.toJson(output).getBytes(StandardCharsets.UTF_8);
            out.write(data, 0, data.length);
            out.closeEntry();
            out.close();

            System.out.println("Written " + amount + " biomes to resource pack file " + zip.getName());

            JOptionPane.showMessageDialog(null, "Done! Generated " + amount + " biomes worth of data\nfor " + pack +
                            "\n\nCreated resource pack file: " + zip.getName(), "Bluemap Datapack Generator",
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
            JOptionPane.showConfirmDialog(null, e.getMessage(), "Bluemap Datapack Generator - Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        return "Unknown";
    }

    /**
     * Finds a zip file within the input folder
     * @param inputFolder The folder
     * @return The zip file
     */
    public static File findZip(File inputFolder) {
        try {
            for (File file : inputFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".zip"))) {
                ZipInputStream zipInput = new ZipInputStream(new FileInputStream(file));

                ZipEntry entry;

                while ((entry = zipInput.getNextEntry()) != null) {
                    if (entry.getName().equalsIgnoreCase("pack.mcmeta")) {
                        zipInput.closeEntry();
                        zipInput.close();
                        return file;
                    }
                }
                zipInput.closeEntry();
                zipInput.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showConfirmDialog(null, e.getMessage(), "Bluemap Datapack Generator - Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    /**
     * Auto extract a zip file in the same directory
     * @param zip The zip file
     * @return True if it succeeded
     */
    public static boolean autoExtract(File zip) {
        try (ZipInputStream zipInput = new ZipInputStream(new FileInputStream(zip))) {

            ZipEntry entry;
            byte[] buffer = new byte[1024];

            while ((entry = zipInput.getNextEntry()) != null) {
                File newFile = newFile(zip.getParentFile(), entry);
                if (entry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + newFile);
                    }
                } else {
                    // fix for Windows-created archives
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }

                    // write file content
                    FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = zipInput.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showConfirmDialog(null, e.getMessage(), "Bluemap Datapack Generator - Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        return true;
    }

    /**
     * Windows fix
     * @param destinationDir Destination directory
     * @param zipEntry The zip entry
     * @return The file
     * @throws IOException
     */
    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }
}
