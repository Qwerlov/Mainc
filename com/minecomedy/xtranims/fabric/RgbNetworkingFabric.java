package com.minecomedy.xtranims.fabric;

import com.minecomedy.xtranims.RgbReflectionHelper;
import com.minecomedy.xtranims.RgbState;
import com.minecomedy.xtranims.XtraConfig;
import com.minecomedy.xtranims.XtraNimations;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.class_2540;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3222;

public class RgbNetworkingFabric {
   private static final class_2960 HELLO_ID = new class_2960("xtranims", "rgb_hello");
   private static final class_2960 HELLO_ACK_ID = new class_2960("xtranims", "rgb_hello_ack");
   private static final class_2960 RGB_UPDATE_ID = new class_2960("xtranims", "rgb_update");
   private static final class_2960 RGB_PEER_ID = new class_2960("xtranims", "rgb_peer");

   public static void registerCommon() {
      ServerPlayNetworking.registerGlobalReceiver(HELLO_ID, (server, player, handler, buf, responseSender) -> server.execute(() -> {
            if (XtraConfig.DEBUG) {
               XtraNimations.LOGGER.info("[Xtranims] HELLO from {}.", player.method_5477().getString());
            }

            class_2540 ack = PacketByteBufs.create();
            ServerPlayNetworking.send(player, HELLO_ACK_ID, ack);
            pushAllTo(player);
         }));
      ServerPlayNetworking.registerGlobalReceiver(RGB_UPDATE_ID, (server, player, handler, buf, responseSender) -> {
         Map<String, Integer> groups = RgbState.decodeColorMap(buf);
         server.execute(() -> {
            UUID uuid = player.method_5667();
            RgbState.recordUpdate(uuid, groups);
            if (XtraConfig.DEBUG) {
               XtraNimations.LOGGER.info("[Xtranims] RGB_UPDATE from {}: {} groups.", player.method_5477().getString(), groups.size());
            }

            broadcastPeerToNearby(player, uuid, groups);
         });
      });
      ServerPlayConnectionEvents.JOIN.register((ServerPlayConnectionEvents.Join)(handler, sender, server) -> pushAllTo(handler.field_14140));
      ServerPlayConnectionEvents.DISCONNECT.register((ServerPlayConnectionEvents.Disconnect)(handler, server) -> {
         class_3222 p = handler.field_14140;
         RgbState.forgetPlayer(p.method_5667());
         if (XtraConfig.DEBUG) {
            XtraNimations.LOGGER.info("[Xtranims] Cleared RGB state for {}", p.method_5477().getString());
         }

      });
      XtraNimations.LOGGER.info("[Xtranims] RgbNetworkingFabric server-side handlers registered (xtranims:rgb v{}).", "2");
   }

   public static void registerClient() {
      RgbState.sendHelloHook = RgbNetworkingFabric::sendHello;
      ClientPlayNetworking.registerGlobalReceiver(HELLO_ACK_ID, (client, handler, buf, responseSender) -> client.execute(() -> {
            RgbState.serverHasCapability = true;
            if (XtraConfig.DEBUG) {
               XtraNimations.LOGGER.info("[Xtranims] HELLO_ACK - server-assisted RGB sync active.");
            }

         }));
      ClientPlayNetworking.registerGlobalReceiver(RGB_PEER_ID, (client, handler, buf, responseSender) -> {
         UUID uuid = buf.method_10790();
         Map<String, Integer> groups = RgbState.decodeColorMap(buf);
         client.execute(() -> {
            if (XtraConfig.DEBUG) {
               XtraNimations.LOGGER.info("[Xtranims] RGB_PEER for {}: {} groups.", uuid, groups.keySet());
            }

            RgbReflectionHelper.applyColorsToOtherPlayer(uuid, groups);
         });
      });
   }

   public static void sendHello() {
      RgbState.serverHasCapability = false;
      if (class_310.method_1551().method_1496()) {
         RgbState.serverHasCapability = true;
         if (XtraConfig.DEBUG) {
            XtraNimations.LOGGER.info("[Xtranims] Integrated server detected — RGB sync active (LAN host).");
         }

      } else if (!ClientPlayNetworking.canSend(HELLO_ID)) {
         if (XtraConfig.DEBUG) {
            XtraNimations.LOGGER.debug("[Xtranims] Server has no xtranims:rgb_hello channel registered — skipping HELLO.");
         }

      } else {
         try {
            ClientPlayNetworking.send(HELLO_ID, PacketByteBufs.create());
            if (XtraConfig.DEBUG) {
               XtraNimations.LOGGER.info("[Xtranims] Sent HELLO (probing server capability).");
            }
         } catch (Exception e) {
            if (XtraConfig.DEBUG) {
               XtraNimations.LOGGER.debug("[Xtranims] HELLO send failed: {}", e.getMessage());
            }
         }

      }
   }

   public static void sendRgbUpdate(Map<String, Integer> groupColors) {
      if (RgbState.serverHasCapability && groupColors != null && !groupColors.isEmpty()) {
         try {
            class_2540 buf = PacketByteBufs.create();
            RgbState.encodeColorMap(buf, groupColors);
            ClientPlayNetworking.send(RGB_UPDATE_ID, buf);
            if (XtraConfig.DEBUG) {
               XtraNimations.LOGGER.info("[Xtranims] RGB_UPDATE sent: {} groups.", groupColors.size());
            }
         } catch (Exception e) {
            XtraNimations.LOGGER.warn("[Xtranims] RGB_UPDATE send failed: {}", e.getMessage());
         }

      }
   }

   private static void pushAllTo(class_3222 target) {
      for(Map.Entry<UUID, Map<String, Integer>> e : RgbState.allEntries()) {
         sendPeerTo(target, (UUID)e.getKey(), (Map)e.getValue());
      }

   }

   private static void sendPeerTo(class_3222 target, UUID uuid, Map<String, Integer> groups) {
      try {
         class_2540 buf = PacketByteBufs.create();
         buf.method_10797(uuid);
         RgbState.encodeColorMap(buf, groups);
         ServerPlayNetworking.send(target, RGB_PEER_ID, buf);
      } catch (Exception var4) {
      }

   }

   private static void broadcastPeerToNearby(class_3222 sender, UUID uuid, Map<String, Integer> groups) {
      for(class_3222 p : RgbState.nearbyTargets(sender)) {
         sendPeerTo(p, uuid, groups);
      }

   }
}
