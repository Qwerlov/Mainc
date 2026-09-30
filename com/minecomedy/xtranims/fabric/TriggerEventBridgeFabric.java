package com.minecomedy.xtranims.fabric;

import com.minecomedy.xtranims.TriggerEventHandler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.class_1657;

public class TriggerEventBridgeFabric {
   public static void register() {
      ClientTickEvents.END_CLIENT_TICK.register((ClientTickEvents.EndTick)(client) -> TriggerEventHandler.tick());
      ClientPlayConnectionEvents.JOIN.register((ClientPlayConnectionEvents.Join)(handler, sender, client) -> TriggerEventHandler.onJoinServer());
      ClientPlayConnectionEvents.DISCONNECT.register((ClientPlayConnectionEvents.Disconnect)(handler, client) -> TriggerEventHandler.onLeaveServer());
      ClientEntityEvents.ENTITY_UNLOAD.register((ClientEntityEvents.Unload)(entity, world) -> {
         if (entity instanceof class_1657) {
            TriggerEventHandler.onClientEntityUnload(entity.method_5667());
         }

      });
   }
}
