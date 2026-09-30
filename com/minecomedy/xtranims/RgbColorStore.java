package com.minecomedy.xtranims;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RgbColorStore {
   private static final Map<String, int[]> colors = new ConcurrentHashMap();
   private static final Map<String, String> names = new ConcurrentHashMap();
   private static final Map<Integer, String> savedCubeIdToKey = new ConcurrentHashMap();
   private static volatile String currentModel = null;
   private static final int[] DEFAULT_WHITE = new int[]{255, 255, 255};

   private static Path colorDir() {
      return XtraConfig.colorsDir();
   }

   private static Path modelFile() {
      String model = currentModel;
      if (model == null || model.isEmpty()) {
         model = "default";
      }

      String safe = model.replace(":", "_").replace("/", "_").replace("\\", "_");
      return colorDir().resolve(safe + ".txt");
   }

   public static void switchModel(String modelName) {
      if (modelName != null) {
         if (!Objects.equals(modelName, currentModel)) {
            if (XtraConfig.DEBUG) {
               XtraNimations.LOGGER.info("[XtraNimations] RgbColorStore: switching model '{}' → '{}'", currentModel, modelName);
            }

            currentModel = modelName;
            colors.clear();
            names.clear();
            savedCubeIdToKey.clear();
            loadCurrentModel();
         }
      }
   }

   public static String getCurrentModel() {
      return currentModel;
   }

   public static String getSavedKeyForCubeId(int cubeId) {
      return (String)savedCubeIdToKey.get(cubeId);
   }

   public static int[] get(String key) {
      int[] v = (int[])colors.get(key);
      return v != null ? v : DEFAULT_WHITE;
   }

   public static boolean hasSaved(String key) {
      return colors.containsKey(key);
   }

   public static String getDisplayName(String key) {
      return (String)names.getOrDefault(key, key);
   }

   public static void setInMemory(String key, int r, int g, int b) {
      int cr = clamp(r);
      int cg = clamp(g);
      int cb = clamp(b);
      int[] existing = (int[])colors.get(key);
      if (existing == null || existing[0] != cr || existing[1] != cg || existing[2] != cb) {
         colors.put(key, new int[]{cr, cg, cb});
      }
   }

   public static void setNameInMemory(String key, String displayName) {
      names.put(key, displayName.isEmpty() ? key : displayName);
   }

   public static void save(String key, Map<String, List<Integer>> groupToCubeId) {
      saveCurrentModel(groupToCubeId);
   }

   public static void save(String key) {
      saveCurrentModel(Collections.emptyMap());
   }

   public static void revert(String key) {
      colors.clear();
      names.clear();
      savedCubeIdToKey.clear();
      loadCurrentModel();
   }

   public static void loadAll() {
   }

   public static Set<String> getLabels() {
      return colors.keySet();
   }

   public static void ensureLabels(Set<String> keys) {
      for(String key : keys) {
         colors.putIfAbsent(key, new int[]{255, 255, 255});
         names.putIfAbsent(key, key);
      }

   }

   private static void loadCurrentModel() {
      Path file = modelFile();
      if (Files.exists(file, new LinkOption[0])) {
         try {
            BufferedReader reader = Files.newBufferedReader(file);

            try {
               String pendingKey = null;
               String pendingLabel = null;
               List<Integer> pendingCubeIds = new ArrayList();
               int r = 255;
               int g = 255;
               int b = 255;
               boolean hasData = false;

               String line;
               while((line = reader.readLine()) != null) {
                  line = line.trim();
                  if (line.isEmpty()) {
                     if (hasData && pendingKey != null) {
                        commitBlock(pendingKey, pendingLabel, pendingCubeIds, r, g, b);
                     }

                     pendingKey = null;
                     pendingLabel = null;
                     pendingCubeIds = new ArrayList();
                     r = 255;
                     g = 255;
                     b = 255;
                     hasData = false;
                  } else {
                     int eq = line.indexOf(61);
                     if (eq >= 0) {
                        String k = line.substring(0, eq).trim();
                        String v = line.substring(eq + 1).trim();
                        switch (k) {
                           case "name":
                              pendingKey = v;
                              hasData = true;
                              break;
                           case "label":
                              pendingLabel = v;
                              break;
                           case "cubeId":
                              int id = parseInt(v, -1);
                              if (id >= 0) {
                                 pendingCubeIds.add(id);
                              }
                              break;
                           case "cubeIds":
                              for(String p : v.split(",")) {
                                 int id = parseInt(p.trim(), -1);
                                 if (id >= 0) {
                                    pendingCubeIds.add(id);
                                 }
                              }
                              break;
                           case "r":
                              r = parseInt(v, 255);
                              hasData = true;
                              break;
                           case "g":
                              g = parseInt(v, 255);
                              hasData = true;
                              break;
                           case "b":
                              b = parseInt(v, 255);
                              hasData = true;
                        }
                     }
                  }
               }

               if (hasData && pendingKey != null) {
                  commitBlock(pendingKey, pendingLabel, pendingCubeIds, r, g, b);
               }

               if (XtraConfig.DEBUG) {
                  XtraNimations.LOGGER.info("[XtraNimations] Loaded {} color groups from {}", colors.size(), file.getFileName());
               }
            } catch (Throwable var21) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var20) {
                     var21.addSuppressed(var20);
                  }
               }

               throw var21;
            }

            if (reader != null) {
               reader.close();
            }
         } catch (IOException e) {
            XtraNimations.LOGGER.error("[XtraNimations] Failed to read model color file {}", file, e);
         }

      }
   }

   private static void commitBlock(String key, String label, List<Integer> cubeIds, int r, int g, int b) {
      colors.put(key, new int[]{clamp(r), clamp(g), clamp(b)});
      names.put(key, label != null && !label.isEmpty() ? label : key);

      for(int cubeId : cubeIds) {
         savedCubeIdToKey.put(cubeId, key);
      }

   }

   private static void saveCurrentModel(Map<String, List<Integer>> groupToCubeId) {
      if (!colors.isEmpty()) {
         try {
            Files.createDirectories(colorDir());
            List<String> keys = new ArrayList(colors.keySet());
            Collections.sort(keys);
            BufferedWriter w = Files.newBufferedWriter(modelFile());

            try {
               boolean first = true;

               for(String key : keys) {
                  if (!first) {
                     w.newLine();
                  }

                  first = false;
                  int[] rgb = (int[])colors.getOrDefault(key, new int[]{255, 255, 255});
                  String label = (String)names.getOrDefault(key, key);
                  w.write("name = " + key);
                  w.newLine();
                  w.write("label = " + label);
                  w.newLine();
                  List<Integer> cubeIds = (List)groupToCubeId.get(key);
                  if (cubeIds != null && !cubeIds.isEmpty()) {
                     StringBuilder sb = new StringBuilder();

                     for(int i = 0; i < cubeIds.size(); ++i) {
                        if (i > 0) {
                           sb.append(',');
                        }

                        sb.append(cubeIds.get(i));
                     }

                     w.write("cubeIds = " + String.valueOf(sb));
                     w.newLine();
                  }

                  w.write("r = " + rgb[0]);
                  w.newLine();
                  w.write("g = " + rgb[1]);
                  w.newLine();
                  w.write("b = " + rgb[2]);
                  w.newLine();
               }
            } catch (Throwable var12) {
               if (w != null) {
                  try {
                     w.close();
                  } catch (Throwable var11) {
                     var12.addSuppressed(var11);
                  }
               }

               throw var12;
            }

            if (w != null) {
               w.close();
            }

            XtraNimations.LOGGER.info("[XtraNimations] Saved {} color groups to {}", colors.size(), modelFile().getFileName());
         } catch (IOException e) {
            XtraNimations.LOGGER.error("[XtraNimations] Failed to save model color file", e);
         }

      }
   }

   private static int parseInt(String s, int def) {
      try {
         return Integer.parseInt(s);
      } catch (NumberFormatException var3) {
         return def;
      }
   }

   private static int clamp(int v) {
      return Math.max(0, Math.min(255, v));
   }
}
