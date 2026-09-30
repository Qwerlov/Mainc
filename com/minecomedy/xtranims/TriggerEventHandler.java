package com.minecomedy.xtranims;

import com.tom.cpm.shared.MinecraftClientAccess;
import com.tom.cpm.shared.animation.AnimationRegistry;
import com.tom.cpm.shared.config.ModConfig;
import com.tom.cpm.shared.config.Player;
import com.tom.cpm.shared.definition.ModelDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.class_1291;
import net.minecraft.class_1293;
import net.minecraft.class_1294;
import net.minecraft.class_1702;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1937;
import net.minecraft.class_1959;
import net.minecraft.class_2338;
import net.minecraft.class_2481;
import net.minecraft.class_2487;
import net.minecraft.class_2489;
import net.minecraft.class_2494;
import net.minecraft.class_2497;
import net.minecraft.class_2499;
import net.minecraft.class_2503;
import net.minecraft.class_2516;
import net.minecraft.class_2520;
import net.minecraft.class_310;
import net.minecraft.class_6880;
import net.minecraft.class_746;

public class TriggerEventHandler {
   private static final Map<String, Integer> lastValues = new HashMap(256);
   private static Map<String, List<String>> colonIndex = Collections.emptyMap();
   private static final String[] MOON_KEYS = new String[8];
   private static String lastProfileId;
   private static ModelDefinition lastModelRef;
   private static volatile boolean pendingPropagateOnJoin;
   private static int modelCheckCounter;
   private static final int MODEL_CHECK_INTERVAL = 10;
   private static int lastHotbarSlot;
   private static class_1799 lastMainItem;
   private static class_1799 lastOffItem;
   private static boolean itemStateDirty;
   private static final Set<class_1291> lastActiveEffects;
   private static boolean potionsDirty;
   private static int lastHealthPct;
   private static int lastFoodPct;
   private static int lastAirPct;
   private static int lastXpLevel;
   private static int lastArmorVal;
   private static int lastPlayerFlags;
   private static final int F_SPRINTING = 1;
   private static final int F_SNEAKING = 2;
   private static final int F_SWIMMING = 4;
   private static final int F_UNDERWATER = 8;
   private static final int F_ELYTRA = 16;
   private static final int F_IN_WATER = 32;
   private static final int F_IN_LAVA = 64;
   private static final int F_ON_FIRE = 128;
   private static final int F_INVISIBLE = 256;
   private static int lastBiomeX;
   private static int lastBiomeZ;
   private static int biomeSlowCounter;
   private static final int BIOME_FORCE_INTERVAL = 80;
   private static boolean lastWasNight;
   private static int lastMoonPhase;
   private static int lastTimeOfDay;
   private static int moonSlowCounter;
   private static final int TIME_FORCE_INTERVAL = 100;
   private static int lastRaining;
   private static int lastThundering;

   public static void tick() {
      if (CPMPlugin.clientApi != null) {
         class_310 mc = class_310.method_1551();
         class_746 player = mc.field_1724;
         if (player != null && mc.field_1687 != null) {
            RgbReflectionHelper.tickPeerRetry();
            if (++modelCheckCounter >= 10) {
               modelCheckCounter = 0;
               checkForModelChange();
            }

            class_1937 level = mc.field_1687;
            boolean screenOpen = mc.field_1755 != null;
            float maxHealth = player.method_6063();
            int healthPct = maxHealth > 0.0F ? (int)(player.method_6032() / maxHealth * 100.0F) : 0;
            if (healthPct != lastHealthPct) {
               lastHealthPct = healthPct;
               send("health_pct", healthPct);
            }

            class_1702 food = player.method_7344();
            int foodPct = food.method_7586() * 5;
            if (foodPct != lastFoodPct) {
               lastFoodPct = foodPct;
               send("food_pct", foodPct);
            }

            int maxAir = player.method_5748();
            int airPct = maxAir > 0 ? (int)((float)Math.max(0, player.method_5669()) / (float)maxAir * 100.0F) : 100;
            if (airPct != lastAirPct) {
               lastAirPct = airPct;
               send("air_pct", airPct);
            }

            int xpLevel = Math.min(player.field_7520, 100);
            if (xpLevel != lastXpLevel) {
               lastXpLevel = xpLevel;
               send("xp_level", xpLevel);
            }

            int armorVal = player.method_6096();
            if (armorVal != lastArmorVal) {
               lastArmorVal = armorVal;
               send("armor_value", armorVal);
            }

            Set<class_1291> currentEffects = new HashSet();

            for(class_1293 instance : player.method_6026()) {
               currentEffects.add(instance.method_5579());
            }

            if (!currentEffects.equals(lastActiveEffects)) {
               lastActiveEffects.clear();
               lastActiveEffects.addAll(currentEffects);
               potionsDirty = true;
            }

            if (potionsDirty) {
               potionsDirty = false;
               send("has_nausea", hasEffect(player, class_1294.field_5916));
               send("has_night_vision", hasEffect(player, class_1294.field_5925));
               send("has_blindness", hasEffect(player, class_1294.field_5919));
               send("has_speed", hasEffect(player, class_1294.field_5904));
               send("has_slowness", hasEffect(player, class_1294.field_5909));
               send("has_strength", hasEffect(player, class_1294.field_5910));
               send("has_weakness", hasEffect(player, class_1294.field_5911));
               send("has_regeneration", hasEffect(player, class_1294.field_5924));
               send("has_poison", hasEffect(player, class_1294.field_5899));
               send("has_wither", hasEffect(player, class_1294.field_5920));
               send("has_fire_resistance", hasEffect(player, class_1294.field_5918));
               send("has_water_breathing", hasEffect(player, class_1294.field_5923));
               send("has_invisibility", hasEffect(player, class_1294.field_5905));
               send("has_levitation", hasEffect(player, class_1294.field_5902));
               send("has_slow_falling", hasEffect(player, class_1294.field_5906));
               send("has_haste", hasEffect(player, class_1294.field_5917));
               send("has_mining_fatigue", hasEffect(player, class_1294.field_5901));
               send("has_absorption", hasEffect(player, class_1294.field_5898));
               send("has_glowing", hasEffect(player, class_1294.field_5912));
               send("has_hunger", hasEffect(player, class_1294.field_5903));
               send("has_saturation", hasEffect(player, class_1294.field_5922));
               send("has_luck", hasEffect(player, class_1294.field_5926));
               send("has_bad_luck", hasEffect(player, class_1294.field_5908));
               send("has_dolphins_grace", hasEffect(player, class_1294.field_5900));
               send("has_conduit_power", hasEffect(player, class_1294.field_5927));
               send("has_hero_of_village", hasEffect(player, class_1294.field_18980));
               send("has_bad_omen", hasEffect(player, class_1294.field_16595));

               for(EffectTriggerLoader.EffectAnimation ea : EffectTriggerLoader.getAnimations()) {
                  send(ea.animationName, player.method_6059(ea.effect) ? 1 : 0);
               }
            }

            send("is_on_fire", player.method_5809() ? 1 : 0);
            int raining = level.method_8419() ? 1 : 0;
            int thundering = level.method_8546() ? 1 : 0;
            if (raining != lastRaining) {
               lastRaining = raining;
               send("is_raining", raining);
            }

            if (thundering != lastThundering) {
               lastThundering = thundering;
               send("is_thundering", thundering);
            }

            if (!screenOpen) {
               int flags = 0;
               if (player.method_5624()) {
                  flags |= 1;
               }

               if (player.method_18276()) {
                  flags |= 2;
               }

               if (player.method_5681()) {
                  flags |= 4;
               }

               if (player.method_5869()) {
                  flags |= 8;
               }

               if (player.method_6128()) {
                  flags |= 16;
               }

               if (player.method_5799()) {
                  flags |= 32;
               }

               if (player.method_5771()) {
                  flags |= 64;
               }

               if (player.method_5809()) {
                  flags |= 128;
               }

               if (player.method_5767()) {
                  flags |= 256;
               }

               if (flags != lastPlayerFlags) {
                  lastPlayerFlags = flags;
                  send("is_sprinting", (flags & 1) != 0 ? 1 : 0);
                  send("is_sneaking", (flags & 2) != 0 ? 1 : 0);
                  send("is_swimming", (flags & 4) != 0 ? 1 : 0);
                  send("is_underwater", (flags & 8) != 0 ? 1 : 0);
                  send("is_elytra_fly", (flags & 16) != 0 ? 1 : 0);
                  send("is_in_water", (flags & 32) != 0 ? 1 : 0);
                  send("is_in_lava", (flags & 64) != 0 ? 1 : 0);
                  send("is_on_fire", (flags & 128) != 0 ? 1 : 0);
                  send("is_invisible", (flags & 256) != 0 ? 1 : 0);
               }

               int hotbarSlot = player.method_31548().field_7545;
               if (hotbarSlot != lastHotbarSlot) {
                  lastHotbarSlot = hotbarSlot;
                  send("hotbar_slot", hotbarSlot);
                  itemStateDirty = true;
               }

               class_1799 main = player.method_6047();
               class_1799 off = player.method_6079();
               boolean mainChanged = !class_1799.method_31577(main, lastMainItem) || main.method_7947() != lastMainItem.method_7947();
               boolean offChanged = !class_1799.method_31577(off, lastOffItem) || off.method_7947() != lastOffItem.method_7947();
               if (mainChanged || offChanged || itemStateDirty) {
                  itemStateDirty = false;
                  lastMainItem = main.method_7972();
                  lastOffItem = off.method_7972();
                  String mainId = main.method_7909().toString();
                  send("holding_sword", mainId.contains("sword") ? 1 : 0);
                  send("holding_bow", main.method_31574(class_1802.field_8102) ? 1 : 0);
                  send("holding_crossbow", main.method_31574(class_1802.field_8399) ? 1 : 0);
                  send("holding_trident", main.method_31574(class_1802.field_8547) ? 1 : 0);
                  send("holding_shield", !main.method_31574(class_1802.field_8255) && !off.method_31574(class_1802.field_8255) ? 0 : 1);
                  send("holding_axe", mainId.contains("_axe") ? 1 : 0);
                  send("holding_pickaxe", mainId.contains("pickaxe") ? 1 : 0);
                  send("holding_shovel", mainId.contains("shovel") ? 1 : 0);
                  send("holding_hoe", mainId.contains("_hoe") ? 1 : 0);
                  send("holding_staff", main.method_31574(class_1802.field_8600) ? 1 : 0);
                  boolean isBook = main.method_31574(class_1802.field_8529) || main.method_31574(class_1802.field_8674) || main.method_31574(class_1802.field_8360) || main.method_31574(class_1802.field_8598);
                  send("holding_book", isBook ? 1 : 0);
                  boolean isTool = mainId.contains("pickaxe") || mainId.contains("_axe") || mainId.contains("shovel") || mainId.contains("_hoe");
                  send("holding_tool", isTool ? 1 : 0);
                  boolean isFood = !main.method_7960() && main.method_7909().method_19264() != null;
                  send("holding_food", isFood ? 1 : 0);
                  send("main_hand_empty", main.method_7960() ? 1 : 0);
                  send("off_hand_empty", off.method_7960() ? 1 : 0);

                  for(NbtTriggerLoader.ItemAnimation anim : NbtTriggerLoader.getAnimations()) {
                     class_1799 var10000;
                     switch (anim.hand) {
                        case MAIN -> var10000 = main;
                        case OFF -> var10000 = off;
                        case BOTH -> var10000 = anim.itemMatches(main) ? main : (anim.itemMatches(off) ? off : class_1799.field_8037);
                        default -> throw new IncompatibleClassChangeError();
                     }

                     class_1799 target = var10000;
                     boolean active = anim.itemMatches(target) && allConditionsPass(anim.conditions, target);
                     send(anim.animationName, active ? 1 : 0);
                  }
               }

               int bx = (int)Math.floor(player.method_23317());
               int bz = (int)Math.floor(player.method_23321());
               ++biomeSlowCounter;
               boolean biomeChanged = bx != lastBiomeX || bz != lastBiomeZ;
               if (biomeChanged || biomeSlowCounter >= 80) {
                  biomeSlowCounter = 0;
                  lastBiomeX = bx;
                  lastBiomeZ = bz;
                  class_2338 pos = player.method_24515();
                  class_6880<class_1959> biomeHolder = level.method_23753(pos);
                  float temp = ((class_1959)biomeHolder.comp_349()).method_8712();
                  String biomeName = (String)biomeHolder.method_40230().map((k) -> k.method_29177().method_12832()).orElse("");
                  send("biome_temperature", Math.max(0, Math.min(100, (int)((temp + 0.5F) / 2.5F * 100.0F))));
                  send("biome_is_snowy", temp < 0.15F ? 1 : 0);
                  send("biome_is_dry", temp > 0.9F ? 1 : 0);
                  send("biome_is_ocean", biomeName.contains("ocean") ? 1 : 0);
                  send("biome_is_desert", biomeName.contains("desert") ? 1 : 0);
                  send("biome_is_forest", biomeName.contains("forest") ? 1 : 0);
                  send("biome_is_swamp", biomeName.contains("swamp") ? 1 : 0);
                  send("biome_is_jungle", biomeName.contains("jungle") ? 1 : 0);
                  send("biome_is_savanna", biomeName.contains("savanna") ? 1 : 0);
                  send("biome_is_nether", level.method_27983() == class_1937.field_25180 ? 1 : 0);
                  send("biome_is_the_end", level.method_27983() == class_1937.field_25181 ? 1 : 0);
                  send("biome_is_underground", pos.method_10264() < 0 ? 1 : 0);
                  send("y_level", Math.max(0, Math.min(100, (int)(((float)pos.method_10264() + 64.0F) / 384.0F * 100.0F))));
                  String fullBiomeId = (String)biomeHolder.method_40230().map((k) -> k.method_29177().toString()).orElse("");

                  for(BiomeTriggerLoader.BiomeAnimation ba : BiomeTriggerLoader.getAnimations()) {
                     send(ba.animationName, ba.matches(fullBiomeId) ? 1 : 0);
                  }
               }
            }

            ++moonSlowCounter;
            long rawTime = level.method_8532() % 24000L;
            int timeOfDay = (int)(rawTime / 1000L);
            boolean isNight = rawTime >= 13000L;
            if (timeOfDay != lastTimeOfDay || moonSlowCounter >= 100) {
               moonSlowCounter = 0;
               lastTimeOfDay = timeOfDay;
               send("time_of_day", timeOfDay);
            }

            if (isNight != lastWasNight || isNight && level.method_30273() != lastMoonPhase) {
               lastWasNight = isNight;
               lastMoonPhase = level.method_30273();

               for(int i = 0; i < 8; ++i) {
                  send(MOON_KEYS[i], isNight && lastMoonPhase == i ? 1 : 0);
               }
            }

            if (RgbKeybind.KEY_OPEN_RGB.method_1436() && mc.field_1755 == null) {
               mc.method_1507(new RgbColorScreen());
            }

         }
      }
   }

   private static int hasEffect(class_746 player, class_1291 effect) {
      return player.method_6059(effect) ? 1 : 0;
   }

   public static void onJoinServer() {
      if (RgbState.sendHelloHook != null) {
         RgbState.sendHelloHook.run();
      }

      pendingPropagateOnJoin = true;
   }

   public static void onLeaveServer() {
      RgbReflectionHelper.reset();
      pendingPropagateOnJoin = false;
   }

   public static void onClientEntityUnload(UUID entityUuid) {
      RgbReflectionHelper.clearPeerPlayer(entityUuid);
   }

   private static void send(String trigger, int value) {
      sendOne(trigger, value);
      List<String> targets = (List)colonIndex.get(trigger);
      if (targets != null) {
         for(String name : targets) {
            sendOne(name, value);
         }
      }

   }

   private static void sendOne(String name, int value) {
      int clamped = value == 0 ? 0 : 1;
      Integer prev = (Integer)lastValues.get(name);
      if (prev == null || prev != clamped) {
         lastValues.put(name, clamped);

         try {
            CPMPlugin.clientApi.playAnimation(name, clamped);
         } catch (Exception var5) {
         }

      }
   }

   private static boolean allConditionsPass(List<NbtTriggerLoader.NbtCondition> conditions, class_1799 stack) {
      if (stack.method_7960()) {
         return false;
      } else {
         class_2487 root = stack.method_7969();
         if (root == null) {
            return false;
         } else {
            for(NbtTriggerLoader.NbtCondition cond : conditions) {
               boolean pass;
               if (cond.op == NbtTriggerLoader.Op.HAS_ENCHANT) {
                  pass = hasEnchantment(root, cond.compareValue);
               } else {
                  class_2520 resolved = walkNbtPath(root, cond.nbtPath);
                  if (resolved == null) {
                     return false;
                  }

                  boolean var10000;
                  switch (cond.op) {
                     case EXISTS -> var10000 = true;
                     case NOT_EMPTY -> var10000 = isNbtNonEmpty(resolved);
                     case EQUALS -> var10000 = nbtAsString(resolved).equalsIgnoreCase(cond.compareValue);
                     case NOT_EQUALS -> var10000 = !nbtAsString(resolved).equalsIgnoreCase(cond.compareValue);
                     case GT -> var10000 = nbtAsInt(resolved) > cond.compareInt;
                     case GTE -> var10000 = nbtAsInt(resolved) >= cond.compareInt;
                     case LT -> var10000 = nbtAsInt(resolved) < cond.compareInt;
                     case LTE -> var10000 = nbtAsInt(resolved) <= cond.compareInt;
                     default -> var10000 = false;
                  }

                  pass = var10000;
               }

               if (!pass) {
                  return false;
               }
            }

            return true;
         }
      }
   }

   private static boolean hasEnchantment(class_2487 root, String enchantId) {
      if (!enchantId.contains(":")) {
         enchantId = "minecraft:" + enchantId;
      }

      for(String listKey : new String[]{"Enchantments", "StoredEnchantments"}) {
         if (root.method_10545(listKey)) {
            class_2499 list = root.method_10554(listKey, 10);

            for(int i = 0; i < list.size(); ++i) {
               if (list.method_10602(i).method_10558("id").equalsIgnoreCase(enchantId)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private static class_2520 walkNbtPath(class_2487 root, String[] path) {
      class_2520 current = root;

      for(String key : path) {
         if (!(current instanceof class_2487)) {
            return null;
         }

         class_2487 c = (class_2487)current;
         if (!c.method_10545(key)) {
            return null;
         }

         current = c.method_10580(key);
      }

      return current;
   }

   private static boolean isNbtNonEmpty(class_2520 tag) {
      boolean var10000;
      switch (tag.method_10711()) {
         case 8 -> var10000 = !tag.method_10714().isEmpty();
         case 9 -> var10000 = ((class_2499)tag).size() > 0;
         case 10 -> var10000 = !((class_2487)tag).method_33133();
         default -> var10000 = nbtAsInt(tag) != 0;
      }

      return var10000;
   }

   private static String nbtAsString(class_2520 tag) {
      String s = tag.method_10714();
      return s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2 ? s.substring(1, s.length() - 1) : s;
   }

   private static int nbtAsInt(class_2520 tag) {
      int var10000;
      switch (tag.method_10711()) {
         case 1:
            var10000 = ((class_2481)tag).method_10701();
            break;
         case 2:
            var10000 = ((class_2516)tag).method_10701();
            break;
         case 3:
            var10000 = ((class_2497)tag).method_10701();
            break;
         case 4:
            var10000 = (int)((class_2503)tag).method_10699();
            break;
         case 5:
            var10000 = (int)((class_2494)tag).method_10700();
            break;
         case 6:
            var10000 = (int)((class_2489)tag).method_10697();
            break;
         case 7:
         case 8:
         default:
            var10000 = 0;
            break;
         case 9:
            var10000 = ((class_2499)tag).size();
      }

      return var10000;
   }

   private static void checkForModelChange() {
      try {
         Player<?> cpmPlayer = MinecraftClientAccess.get().getCurrentClientPlayer();
         if (cpmPlayer == null) {
            if (lastProfileId != null) {
               resetAll();
            }

            return;
         }

         ModelDefinition def = cpmPlayer.getModelDefinition();
         if (def == null) {
            if (lastProfileId != null) {
               resetAll();
            }

            return;
         }

         AnimationRegistry registry = def.getAnimations();
         if (registry == null) {
            return;
         }

         String profileId = registry.getProfileId();
         if (Objects.equals(profileId, lastProfileId) && def == lastModelRef) {
            return;
         }

         lastProfileId = profileId;
         lastModelRef = def;
         resetStateFlags();
         Map<String, List<String>> index = new HashMap();

         for(String name : registry.getCommandActionsMap().keySet()) {
            if (name.contains(":") && !name.startsWith("rgb:")) {
               int lastColon = name.lastIndexOf(58);
               String afterLast = name.substring(lastColon + 1);
               String fullName = !afterLast.isEmpty() && afterLast.chars().allMatch(Character::isDigit) ? name.substring(0, lastColon) : name;
               String base = fullName.substring(0, fullName.indexOf(58));
               ((List)index.computeIfAbsent(base, (k) -> new ArrayList())).add(fullName);
            }
         }

         index.replaceAll((k, v) -> Collections.unmodifiableList(v));
         colonIndex = Collections.unmodifiableMap(index);
         String activeModel = null;

         try {
            activeModel = ModConfig.getCommonConfig().getString("selectedModel", (String)null);
            if (".temp.cpmmodel".equals(activeModel)) {
               String old2 = ModConfig.getCommonConfig().getString("selectedModelOld", (String)null);
               if (old2 != null && !old2.equals("~~VANILLA~~")) {
                  activeModel = old2;
               }
            }
         } catch (Exception var11) {
         }

         RgbReflectionHelper.scanModel(def, registry, activeModel);
         NbtTriggerLoader.load();
         EffectTriggerLoader.load();
         BiomeTriggerLoader.load();
         if (pendingPropagateOnJoin) {
            pendingPropagateOnJoin = false;
            RgbReflectionHelper.applyColors();
         }

         XtraNimations.LOGGER.info("[XtraNimations] Model loaded (profile: {}). Colon anims: {}", profileId, colonIndex);
      } catch (Exception var12) {
      }

   }

   private static void resetAll() {
      colonIndex = Collections.emptyMap();
      lastProfileId = null;
      lastModelRef = null;
      lastValues.clear();
      resetStateFlags();
      RgbReflectionHelper.reset();
   }

   private static void resetStateFlags() {
      modelCheckCounter = 10;
      lastValues.clear();
      lastPlayerFlags = -1;
      lastArmorVal = -1;
      lastXpLevel = -1;
      lastAirPct = -1;
      lastFoodPct = -1;
      lastHealthPct = -1;
      lastHotbarSlot = -1;
      lastMainItem = class_1799.field_8037;
      lastOffItem = class_1799.field_8037;
      itemStateDirty = true;
      lastActiveEffects.clear();
      potionsDirty = true;
      lastBiomeZ = Integer.MIN_VALUE;
      lastBiomeX = Integer.MIN_VALUE;
      biomeSlowCounter = 80;
      lastWasNight = false;
      lastMoonPhase = -1;
      lastTimeOfDay = -1;
      moonSlowCounter = 100;
      lastThundering = -1;
      lastRaining = -1;
   }

   static {
      for(int i = 0; i < 8; ++i) {
         MOON_KEYS[i] = "moon_phase_" + i;
      }

      lastProfileId = null;
      lastModelRef = null;
      pendingPropagateOnJoin = false;
      modelCheckCounter = 0;
      lastHotbarSlot = -1;
      lastMainItem = class_1799.field_8037;
      lastOffItem = class_1799.field_8037;
      itemStateDirty = true;
      lastActiveEffects = new HashSet();
      potionsDirty = true;
      lastHealthPct = -1;
      lastFoodPct = -1;
      lastAirPct = -1;
      lastXpLevel = -1;
      lastArmorVal = -1;
      lastPlayerFlags = -1;
      lastBiomeX = Integer.MIN_VALUE;
      lastBiomeZ = Integer.MIN_VALUE;
      biomeSlowCounter = 0;
      lastWasNight = false;
      lastMoonPhase = -1;
      lastTimeOfDay = -1;
      moonSlowCounter = 0;
      lastRaining = -1;
      lastThundering = -1;
   }
}
