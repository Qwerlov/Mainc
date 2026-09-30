package com.minecomedy.xtranims.fabric;

import com.minecomedy.xtranims.XtraNimations;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;

public class XtraNimationsFabric implements ModInitializer {
   public void onInitialize() {
      RgbNetworkingFabric.registerCommon();
      XtraNimations.LOGGER.info("[XtraNimations] Mod loaded (Fabric, common).");
   }

   public static class ClientEntry implements ClientModInitializer {
      public void onInitializeClient() {
         RgbKeybindFabric.register();
         RgbNetworkingFabric.registerClient();
         TriggerEventBridgeFabric.register();
         XtraNimations.initClient();
         XtraNimations.LOGGER.info("[XtraNimations] Client init complete (Fabric).");
      }
   }

   public static class ServerEntry implements DedicatedServerModInitializer {
      public void onInitializeServer() {
         XtraNimations.LOGGER.info("[XtraNimations] Dedicated server init complete (Fabric).");
      }
   }
}
