package com.minecomedy.xtranims;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.class_1937;
import net.minecraft.class_2540;
import net.minecraft.class_2960;
import net.minecraft.class_3218;
import net.minecraft.class_3222;

public final class RgbState {
   public static final class_2960 CHANNEL_ID = new class_2960("xtranims", "rgb");
   public static final String VERSION_STR = "2";
   public static volatile boolean serverHasCapability = false;
   public static volatile Runnable sendHelloHook = null;
   public static final ConcurrentHashMap<UUID, Map<String, Integer>> rgbStore = new ConcurrentHashMap();

   private RgbState() {
   }

   public static void encodeColorMap(class_2540 buf, Map<String, Integer> groups) {
      buf.writeByte(Math.min(groups.size(), 255));
      int n = 0;

      for(Map.Entry<String, Integer> e : groups.entrySet()) {
         if (n++ >= 255) {
            break;
         }

         buf.method_10788((String)e.getKey(), 64);
         buf.writeInt((Integer)e.getValue());
      }

   }

   public static Map<String, Integer> decodeColorMap(class_2540 buf) {
      int count = buf.readUnsignedByte();
      Map<String, Integer> g = new HashMap(count);

      for(int i = 0; i < count; ++i) {
         g.put(buf.method_10800(64), buf.readInt());
      }

      return g;
   }

   public static List<Map.Entry<UUID, Map<String, Integer>>> allEntries() {
      return new ArrayList(rgbStore.entrySet());
   }

   public static void recordUpdate(UUID uuid, Map<String, Integer> groups) {
      rgbStore.put(uuid, groups);
   }

   public static void forgetPlayer(UUID uuid) {
      rgbStore.remove(uuid);
   }

   public static List<class_3222> nearbyTargets(class_3222 sender) {
      List<class_3222> targets = new ArrayList();
      class_1937 var3 = sender.method_37908();
      if (var3 instanceof class_3218 sl) {
         for(class_3222 p : sl.method_18456()) {
            if (p != sender && !(sender.method_5739(p) > 128.0F)) {
               targets.add(p);
            }
         }

         return targets;
      } else {
         return targets;
      }
   }
}
