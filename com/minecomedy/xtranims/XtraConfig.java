package com.minecomedy.xtranims;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;

public class XtraConfig {
   public static volatile boolean DEBUG = false;
   private static final String CONFIG_FILE = "General_Config.txt";

   public static Path configDir() {
      return XtraPlatform.INSTANCE.gameConfigDir().resolve("xtranims");
   }

   public static Path animationsDir() {
      return configDir().resolve("animations");
   }

   public static Path colorsDir() {
      return configDir().resolve("colors");
   }

   public static Path effectsDir() {
      return configDir().resolve("effects");
   }

   public static Path biomesDir() {
      return configDir().resolve("biomes");
   }

   public static void load() {
      Path dir = configDir();
      Path file = dir.resolve("General_Config.txt");

      try {
         Files.createDirectories(dir);
      } catch (IOException e) {
         XtraNimations.LOGGER.error("[XtraNimations] Could not create config dir: {}", e.getMessage());
         return;
      }

      if (!Files.exists(file, new LinkOption[0])) {
         writeDefaultConfig(file);
      }

      parseConfig(file);
      XtraNimations.LOGGER.info("[XtraNimations] Config loaded — debug={}", DEBUG);
   }

   private static void parseConfig(Path file) {
      List<String> lines;
      try {
         lines = Files.readAllLines(file);
      } catch (IOException e) {
         XtraNimations.LOGGER.error("[XtraNimations] Could not read {}: {}", "General_Config.txt", e.getMessage());
         return;
      }

      for(String raw : lines) {
         String line = raw.strip();
         if (!line.isEmpty() && !line.startsWith("#")) {
            int eq = line.indexOf(61);
            if (eq >= 0) {
               String key = line.substring(0, eq).strip().toLowerCase();
               String val = line.substring(eq + 1).strip().toLowerCase();
               if (key.equals("debug")) {
                  DEBUG = val.equals("true") || val.equals("1") || val.equals("yes");
               }
            }
         }
      }

   }

   private static void writeDefaultConfig(Path file) {
      String content = "# ╔══════════════════════════════════════════════════════════════════╗\n# ║              XtraNimations — General Configuration              ║\n# ╚══════════════════════════════════════════════════════════════════╝\n#\n# This file controls global behaviour for the XtraNimations mod.\n# Edit the values below and restart Minecraft to apply changes.\n#\n# ─── OPTIONS ────────────────────────────────────────────────────────\n#\n#   debug = false  |  true\n#       Controls verbose logging in the client log file.\n#\n#       false (default) — only important warnings and errors are logged.\n#                         Best for normal gameplay; keeps the log clean.\n#\n#       true            — enables detailed internal state messages.\n#                         Use when reporting bugs or troubleshooting\n#                         unexpected animation behaviour.\n#\n# ────────────────────────────────────────────────────────────────────\n\ndebug = false\n";

      try {
         Files.writeString(file, content);
         XtraNimations.LOGGER.info("[XtraNimations] Created default General_Config.txt");
      } catch (IOException e) {
         XtraNimations.LOGGER.error("[XtraNimations] Could not write {}: {}", "General_Config.txt", e.getMessage());
      }

   }
}
