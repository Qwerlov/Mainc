package com.minecomedy.xtranims;

import com.tom.cpl.nbt.NBTTagCompound;
import com.tom.cpm.api.ICPMPlugin;
import com.tom.cpm.api.IClientAPI;
import com.tom.cpm.api.ICommonAPI;
import java.util.HashMap;
import java.util.Map;

public class CPMPlugin implements ICPMPlugin {
   public static IClientAPI clientApi = null;
   public static IClientAPI.MessageSender rgbMessageSender = null;

   public String getOwnerModId() {
      return "xtranims";
   }

   public void initClient(IClientAPI api) {
      clientApi = api;

      try {
         api.registerEditorGenerator("xtranims_reference", "XtraNimations Triggers", (gui) -> {
         });
      } catch (Exception e) {
         XtraNimations.LOGGER.warn("[XtraNimations] Could not register CPM editor panel: {}", e.getMessage());
      }

      try {
         rgbMessageSender = api.registerPluginMessage("rgb", (senderUuid, tag) -> {
            int count = tag.getInteger("n");
            Map<String, Integer> groups = new HashMap(count);

            for(int i = 0; i < count; ++i) {
               String key = tag.getString("k" + i);
               int color = tag.getInteger("v" + i);
               if (key != null && !key.isEmpty()) {
                  groups.put(key, color);
               }
            }

            if (!groups.isEmpty()) {
               if (XtraConfig.DEBUG) {
                  XtraNimations.LOGGER.info("[XtraNimations] CPM RGB_PEER from {}: {} groups", senderUuid, groups.keySet());
               }

               RgbReflectionHelper.applyColorsToOtherPlayer(senderUuid, groups);
            }

         }, true);
         XtraNimations.LOGGER.info("[XtraNimations] CPM RGB plugin message registered.");
      } catch (Exception e) {
         XtraNimations.LOGGER.warn("[XtraNimations] Could not register CPM RGB plugin message: {}", e.getMessage());
      }

   }

   public void initCommon(ICommonAPI api) {
   }

   public static void sendRgbViaCpm(Map<String, Integer> groupColors) {
      if (rgbMessageSender != null && groupColors != null && !groupColors.isEmpty()) {
         try {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("n", groupColors.size());
            int i = 0;

            for(Map.Entry<String, Integer> e : groupColors.entrySet()) {
               tag.setString("k" + i, (String)e.getKey());
               tag.setInteger("v" + i, (Integer)e.getValue());
               ++i;
            }

            rgbMessageSender.sendMessage(tag);
            if (XtraConfig.DEBUG) {
               XtraNimations.LOGGER.info("[XtraNimations] CPM RGB sent: {} groups via CPM channel.", groupColors.size());
            }
         } catch (Exception e) {
            XtraNimations.LOGGER.warn("[XtraNimations] CPM RGB send failed: {}", e.getMessage());
         }

      }
   }
}
