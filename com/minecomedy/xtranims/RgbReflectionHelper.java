package com.minecomedy.xtranims;

import com.tom.cpl.math.Rotation;
import com.tom.cpl.math.Vec3f;
import com.tom.cpm.shared.MinecraftClientAccess;
import com.tom.cpm.shared.animation.Animation;
import com.tom.cpm.shared.animation.AnimationRegistry;
import com.tom.cpm.shared.animation.AnimationTrigger;
import com.tom.cpm.shared.animation.IModelComponent;
import com.tom.cpm.shared.animation.IPose;
import com.tom.cpm.shared.animation.InterpolatorChannel;
import com.tom.cpm.shared.animation.VanillaPose;
import com.tom.cpm.shared.animation.interpolator.Interpolator;
import com.tom.cpm.shared.animation.interpolator.InterpolatorType;
import com.tom.cpm.shared.config.ModConfig;
import com.tom.cpm.shared.definition.ModelDefinition;
import com.tom.cpm.shared.definition.ModelDefinition.ModelLoadingState;
import com.tom.cpm.shared.io.ModelFile;
import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleUnaryOperator;
import net.minecraft.class_1657;
import net.minecraft.class_310;

public class RgbReflectionHelper {
   private static Field f_parts = null;
   private static Field f_resolved = null;
   private static Field f_otherParts = null;
   private static Field f_resolvedOtherParts = null;
   private static Field f_effect = null;
   private static Field f_effectId = null;
   private static Field f_effectColor = null;
   private static Field f_cubeRgb = null;
   private static Field f_recolor = null;
   private static Field f_renderedColor = null;
   private static Field f_psfs = null;
   private static Field f_iValues = null;
   private static Field f_trigAnimations = null;
   private static Class<?> animClass = null;
   private static MethodHandle mh_getCube = null;
   private static Field f_cubeId = null;
   private static boolean reflectionReady = false;
   private static final Map<String, List<Object>> groupToCubes = new LinkedHashMap();
   private static final Map<String, List<Integer>> groupToCubeId = new LinkedHashMap();
   private static final Map<Integer, Integer> cubeIdToColor = new HashMap();
   private static final Map<Integer, Integer> cubeIdToTagColor = new LinkedHashMap();
   private static final Map<Integer, String> cubeIdToOriginalGroup = new HashMap();
   private static final Map<Integer, Integer> cubeIdToOriginalTagColor = new HashMap();
   private static volatile boolean isPatchingInProgress = false;
   private static ModelDefinition lastDef = null;
   private static final Map<UUID, Map<String, Integer>> pendingPeerColors = new ConcurrentHashMap();
   private static final Map<String, InjectedColorAnim> injectedAnims = new LinkedHashMap();
   private static final Map<String, InjectedColorAnim> peerInjectedAnims = new ConcurrentHashMap();
   private static final Map<Class<?>, Field> interpolatorValuesFieldCache = new HashMap();

   public static Map<String, List<Integer>> getGroupToCubeId() {
      return Collections.unmodifiableMap(groupToCubeId);
   }

   private static boolean initReflection() {
      if (reflectionReady) {
         return true;
      } else {
         try {
            Class<?> defCls = Class.forName("com.tom.cpm.shared.definition.ModelDefinition");
            Class<?> partDefCls = Class.forName("com.tom.cpm.shared.parts.ModelPartDefinition");
            Class<?> partEffCls = Class.forName("com.tom.cpm.shared.parts.ModelPartRenderEffect");
            Class<?> effColorCls = Class.forName("com.tom.cpm.shared.effects.EffectColor");
            Class<?> rawCubeCls = Class.forName("com.tom.cpm.shared.model.Cube");
            Class<?> rendCubeCls = Class.forName("com.tom.cpm.shared.model.RenderedCube");
            f_parts = defCls.getDeclaredField("parts");
            f_parts.setAccessible(true);
            f_resolved = defCls.getDeclaredField("resolved");
            f_resolved.setAccessible(true);
            f_otherParts = partDefCls.getDeclaredField("otherParts");
            f_otherParts.setAccessible(true);
            f_resolvedOtherParts = partDefCls.getDeclaredField("resolvedOtherParts");
            f_resolvedOtherParts.setAccessible(true);
            f_effect = partEffCls.getDeclaredField("effect");
            f_effect.setAccessible(true);
            f_effectId = effColorCls.getDeclaredField("id");
            f_effectId.setAccessible(true);
            f_effectColor = effColorCls.getDeclaredField("color");
            f_effectColor.setAccessible(true);
            f_cubeRgb = rawCubeCls.getDeclaredField("rgb");
            f_cubeRgb.setAccessible(true);
            f_cubeId = rawCubeCls.getDeclaredField("id");
            f_cubeId.setAccessible(true);
            f_recolor = rendCubeCls.getDeclaredField("recolor");
            f_recolor.setAccessible(true);
            f_renderedColor = rendCubeCls.getDeclaredField("color");
            f_renderedColor.setAccessible(true);
            animClass = Class.forName("com.tom.cpm.shared.animation.Animation");
            f_psfs = animClass.getDeclaredField("psfs");
            f_psfs.setAccessible(true);
            f_trigAnimations = Class.forName("com.tom.cpm.shared.animation.AnimationTrigger").getDeclaredField("animations");
            f_trigAnimations.setAccessible(true);
            mh_getCube = MethodHandles.publicLookup().findVirtual(rendCubeCls, "getCube", MethodType.methodType(rawCubeCls));
            reflectionReady = true;
            return true;
         } catch (Exception e) {
            XtraNimations.LOGGER.error("[XtraNimations] RGB reflection init failed: {}", e.getMessage());
            return false;
         }
      }
   }

   private static boolean isGistModel() {
      try {
         String modelName = ModConfig.getCommonConfig().getString("selectedModel", (String)null);
         if (modelName != null && !modelName.equals("~~VANILLA~~")) {
            if (".temp.cpmmodel".equals(modelName)) {
               String old = ModConfig.getCommonConfig().getString("selectedModelOld", (String)null);
               if (old == null || old.equals("~~VANILLA~~")) {
                  return false;
               }

               modelName = old;
            }

            File modelsDir = new File(MinecraftClientAccess.get().getGameDir(), "player_models");
            File modelFile = new File(modelsDir, modelName);
            if (!modelFile.exists()) {
               return false;
            } else {
               return ModelFile.load(modelFile).convertable();
            }
         } else {
            return false;
         }
      } catch (Exception e) {
         if (XtraConfig.DEBUG) {
            XtraNimations.LOGGER.debug("[XtraNimations] RGB isGistModel check failed: {}", e.getMessage());
         }

         return false;
      }
   }

   public static void scanModel(ModelDefinition def, AnimationRegistry registry, String activeModelName) {
      groupToCubes.clear();
      groupToCubeId.clear();
      cubeIdToColor.clear();
      cubeIdToTagColor.clear();
      if (initReflection() && def != null) {
         lastDef = def;
         RgbColorStore.switchModel(activeModelName);
         List<Object> parts = null;

         try {
            parts = (List)f_parts.get(def);
         } catch (Throwable e) {
            XtraNimations.LOGGER.error("[XtraNimations] RGB: can't read parts: {}", e.getMessage());
         }

         if (parts != null) {
            for(Object part : parts) {
               String cls = part.getClass().getName();
               if (cls.equals("com.tom.cpm.shared.parts.ModelPartRenderEffect")) {
                  checkRenderEffect(part, def);
               } else if (cls.equals("com.tom.cpm.shared.parts.ModelPartDefinition")) {
                  scanPartDefinitionOtherParts(part, def);
               }
            }
         }

         List<Object> resolved = null;

         try {
            resolved = (List)f_resolved.get(def);
         } catch (Throwable var10) {
         }

         if (resolved != null) {
            for(Object rpart : resolved) {
               String cls = rpart.getClass().getName();
               if (cls.equals("com.tom.cpm.shared.parts.ModelPartDefinition")) {
                  scanPartDefinitionResolvedOtherParts(rpart, def);
               }
            }
         }

         RgbColorStore.ensureLabels(groupToCubes.keySet());
         if (!groupToCubes.isEmpty()) {
            if (XtraConfig.DEBUG) {
               XtraNimations.LOGGER.info("[XtraNimations] RGB scan done. Groups: {}", groupToCubes.keySet());
            }

            injectedAnims.clear();

            for(Map.Entry<String, List<Object>> e : groupToCubes.entrySet()) {
               int[] rgb = RgbColorStore.get((String)e.getKey());
               int packed = rgb[0] << 16 | rgb[1] << 8 | rgb[2];
               InjectedColorAnim ica = injectColorAnimation(def, (List)e.getValue(), packed);
               if (ica != null) {
                  injectedAnims.put((String)e.getKey(), ica);
               }
            }

            applyColorsLocal();
         }

      }
   }

   private static void scanPartDefinitionOtherParts(Object partDef, ModelDefinition def) {
      List<Object> otherParts;
      try {
         otherParts = (List)f_otherParts.get(partDef);
      } catch (Throwable var5) {
         return;
      }

      if (otherParts != null) {
         for(Object other : otherParts) {
            if (other.getClass().getName().equals("com.tom.cpm.shared.parts.ModelPartRenderEffect")) {
               checkRenderEffect(other, def);
            }
         }

      }
   }

   private static void scanPartDefinitionResolvedOtherParts(Object partDef, ModelDefinition def) {
      List<Object> resolvedOtherParts;
      try {
         resolvedOtherParts = (List)f_resolvedOtherParts.get(partDef);
      } catch (Throwable var5) {
         return;
      }

      if (resolvedOtherParts != null) {
         for(Object rpart : resolvedOtherParts) {
            if (rpart.getClass().getName().equals("com.tom.cpm.shared.parts.ModelPartRenderEffect")) {
               checkRenderEffect(rpart, def);
            }
         }

      }
   }

   private static void checkRenderEffect(Object renderEffectPart, ModelDefinition def) {
      Object effect;
      try {
         effect = f_effect.get(renderEffectPart);
      } catch (Throwable var14) {
         return;
      }

      if (effect != null) {
         if (effect.getClass().getName().equals("com.tom.cpm.shared.effects.EffectColor")) {
            int cubeId;
            int tagColor;
            try {
               cubeId = (Integer)f_effectId.get(effect);
               tagColor = (Integer)f_effectColor.get(effect);
            } catch (Throwable var13) {
               return;
            }

            int colorVal = tagColor & 16777215;
            String groupKey;
            if (cubeIdToOriginalGroup.containsKey(cubeId)) {
               groupKey = (String)cubeIdToOriginalGroup.get(cubeId);
               colorVal = (Integer)cubeIdToOriginalTagColor.getOrDefault(cubeId, colorVal);
            } else {
               if (colorVal == 16777215 || colorVal == 0) {
                  return;
               }

               groupKey = String.format("rgb:%06X", colorVal);
               cubeIdToOriginalGroup.put(cubeId, groupKey);
               cubeIdToOriginalTagColor.put(cubeId, colorVal);
            }

            Object cube = def.getElementById(cubeId);
            if (cube == null) {
               XtraNimations.LOGGER.warn("[XtraNimations] RGB: getElementById({}) returned null for group '{}'", cubeId, groupKey);
            } else {
               String savedKey = RgbColorStore.getSavedKeyForCubeId(cubeId);
               if (savedKey != null && !savedKey.equals(groupKey)) {
                  if (XtraConfig.DEBUG) {
                     XtraNimations.LOGGER.debug("[XtraNimations] RGB gist anchor: cubeId={} hex-key='{}' → saved-key='{}'", cubeId, groupKey, savedKey);
                  }

                  cubeIdToOriginalGroup.put(cubeId, savedKey);
                  groupKey = savedKey;
               }

               List<Integer> cubeIdList = (List)groupToCubeId.computeIfAbsent(groupKey, (k) -> new ArrayList());
               if (cubeIdList.contains(cubeId)) {
                  if (XtraConfig.DEBUG) {
                     XtraNimations.LOGGER.debug("[XtraNimations] RGB skip duplicate cubeId={} for group '{}'", cubeId, groupKey);
                  }

               } else {
                  cubeIdList.add(cubeId);
                  ((List)groupToCubes.computeIfAbsent(groupKey, (k) -> new ArrayList())).add(cube);
                  cubeIdToTagColor.put(cubeId, colorVal);
                  if (XtraConfig.DEBUG) {
                     XtraNimations.LOGGER.debug("[XtraNimations] RGB group '{}' -> cubeId={}", groupKey, cubeId);
                  }

                  try {
                     Object underlying = mh_getCube.invoke(cube);
                     int before = f_cubeRgb.getInt(underlying);
                     f_cubeRgb.setInt(underlying, 16777215);
                     if (XtraConfig.DEBUG) {
                        XtraNimations.LOGGER.info("[XtraNimations] RGB init cubeId={} rgb #{} -> #FFFFFF", cubeId, Integer.toHexString(before).toUpperCase());
                     }
                  } catch (Throwable ex) {
                     XtraNimations.LOGGER.warn("[XtraNimations] RGB: init cube.rgb failed cubeId={}: {}", cubeId, ex.getMessage());
                  }

               }
            }
         }
      }
   }

   public static void applyColorsLocal() {
      if (!groupToCubes.isEmpty() && f_cubeRgb != null) {
         for(Map.Entry<String, List<Object>> e : groupToCubes.entrySet()) {
            int[] rgb = RgbColorStore.get((String)e.getKey());
            int packed = rgb[0] << 16 | rgb[1] << 8 | rgb[2];

            for(Object cube : (List)e.getValue()) {
               try {
                  Object underlying = mh_getCube.invoke(cube);
                  f_cubeRgb.setInt(underlying, packed);
                  int id = f_cubeId.getInt(underlying);
                  cubeIdToColor.put(id, packed);
                  if (f_recolor != null) {
                     f_recolor.set(cube, true);
                  }
               } catch (Throwable var8) {
               }
            }

            InjectedColorAnim ica = (InjectedColorAnim)injectedAnims.get(e.getKey());
            if (ica != null) {
               ica.setColor(packed);
            }
         }

      }
   }

   public static void applyColorsForced() {
      isPatchingInProgress = false;
      applyColors();
   }

   public static void applyColors() {
      if (!groupToCubes.isEmpty() && f_cubeRgb != null) {
         boolean anyColorSaved = false;

         for(Map.Entry<String, List<Object>> e : groupToCubes.entrySet()) {
            if (RgbColorStore.hasSaved((String)e.getKey())) {
               anyColorSaved = true;
               break;
            }
         }

         for(Map.Entry<String, List<Object>> e : groupToCubes.entrySet()) {
            int[] rgb = RgbColorStore.get((String)e.getKey());
            int packed = rgb[0] << 16 | rgb[1] << 8 | rgb[2];
            if (XtraConfig.DEBUG) {
               XtraNimations.LOGGER.info("[XtraNimations] RGB applyColors: group='{}' packed=#{} cubes={}", e.getKey(), Integer.toHexString(packed).toUpperCase(), ((List)e.getValue()).size());
            }

            for(Object cube : (List)e.getValue()) {
               try {
                  Object underlying = mh_getCube.invoke(cube);
                  int before = f_cubeRgb.getInt(underlying);
                  f_cubeRgb.setInt(underlying, packed);
                  int after = f_cubeRgb.getInt(underlying);
                  int id = f_cubeId.getInt(underlying);
                  cubeIdToColor.put(id, packed);
                  boolean rc = f_recolor != null && (Boolean)f_recolor.get(cube);
                  if (XtraConfig.DEBUG) {
                     XtraNimations.LOGGER.info("[XtraNimations] RGB cube id={} rgb #{} -> #{} (readback=#{} recolor={})", id, Integer.toHexString(before).toUpperCase(), Integer.toHexString(packed).toUpperCase(), Integer.toHexString(after).toUpperCase(), rc);
                  }

                  if (f_recolor != null) {
                     f_recolor.set(cube, true);
                  }
               } catch (Throwable t) {
                  XtraNimations.LOGGER.warn("[XtraNimations] RGB write failed: {}", t.getMessage());
               }
            }
         }

         if (anyColorSaved) {
            boolean gist = isGistModel();
            if (XtraConfig.DEBUG) {
               XtraNimations.LOGGER.debug("[XtraNimations] RGB propagate: isGistModel={}", gist);
            }

            if (gist) {
               Map<String, Integer> groupColors = new LinkedHashMap();

               for(Map.Entry<String, List<Object>> e : groupToCubes.entrySet()) {
                  int[] rgb = RgbColorStore.get((String)e.getKey());
                  int packed = rgb[0] << 16 | rgb[1] << 8 | rgb[2];
                  groupColors.put((String)e.getKey(), packed);
                  InjectedColorAnim ica = (InjectedColorAnim)injectedAnims.get(e.getKey());
                  if (ica != null) {
                     ica.setColor(packed);
                  } else {
                     ica = injectColorAnimation(lastDef, (List)e.getValue(), packed);
                     if (ica != null) {
                        injectedAnims.put((String)e.getKey(), ica);
                     }
                  }
               }

               if (!isPatchingInProgress) {
                  isPatchingInProgress = true;

                  try {
                     RgbSkinPatcher.patchAndResend(cubeIdToColor, cubeIdToTagColor);
                  } finally {
                     scheduleGuardReset();
                  }
               }
            } else if (!isPatchingInProgress) {
               isPatchingInProgress = true;

               try {
                  RgbSkinPatcher.patchAndResend(cubeIdToColor, cubeIdToTagColor);
               } finally {
                  scheduleGuardReset();
               }
            }

         }
      }
   }

   public static InjectedColorAnim injectColorAnimation(ModelDefinition def, List<Object> cubes, int packed) {
      try {
         AnimationRegistry registry = def.getAnimations();
         if (registry == null) {
            return null;
         } else {
            int numComponents = cubes.size();
            int numChannels = InterpolatorChannel.VALUES.length;
            float[][][] data = new float[numComponents][numChannels][1];
            float r = (float)(packed >> 16 & 255);
            float g = (float)(packed >> 8 & 255);
            float b = (float)(packed & 255);

            for(int ci = 0; ci < numComponents; ++ci) {
               for(InterpolatorChannel ch : InterpolatorChannel.VALUES) {
                  data[ci][ch.channelID()][0] = (float)ch.defaultValue;
               }

               data[ci][InterpolatorChannel.COLOR_R.channelID()][0] = r;
               data[ci][InterpolatorChannel.COLOR_G.channelID()][0] = g;
               data[ci][InterpolatorChannel.COLOR_B.channelID()][0] = b;
            }

            Boolean[][] show = new Boolean[numComponents][1];

            for(int ci = 0; ci < numComponents; ++ci) {
               show[ci][0] = true;
            }

            IModelComponent[] components = new IModelComponent[numComponents];

            for(int ci = 0; ci < numComponents; ++ci) {
               final IModelComponent realCube = (IModelComponent)cubes.get(ci);
               components[ci] = new IModelComponent() {
                  public void setPosition(boolean add, float x, float y, float z) {
                  }

                  public void setRotation(boolean add, float x, float y, float z) {
                  }

                  public void setVisible(boolean v) {
                  }

                  public void setRenderScale(boolean add, float x, float y, float z) {
                  }

                  public void reset() {
                  }

                  public Vec3f getPosition() {
                     return realCube.getPosition();
                  }

                  public Rotation getRotation() {
                     return realCube.getRotation();
                  }

                  public Vec3f getRenderScale() {
                     return realCube.getRenderScale();
                  }

                  public boolean isVisible() {
                     return realCube.isVisible();
                  }

                  public int getRGB() {
                     return realCube.getRGB();
                  }

                  public void setColor(float r2, float g2, float b2) {
                     realCube.setColor(r2, g2, b2);
                  }
               };
            }

            Animation anim = new Animation(components, data, show, 1, 1000, false, InterpolatorType.NO_INTERPOLATE);
            Set<IPose> onPoses = new HashSet();
            onPoses.add(VanillaPose.GLOBAL);
            AnimationTrigger trigger = new AnimationTrigger(registry, onPoses, (VanillaPose)null, Collections.singletonList(anim), true, false);
            registry.register(trigger);
            Field fPsfs = Animation.class.getDeclaredField("psfs");
            fPsfs.setAccessible(true);
            Object[][] psfs = fPsfs.get(anim);
            Class<?> noInterpCls = Class.forName("com.tom.cpm.shared.animation.interpolator.NoInterpolate");
            Field fValues = noInterpCls.getDeclaredField("values");
            fValues.setAccessible(true);
            int rIdx = InterpolatorChannel.COLOR_R.channelID();
            int gIdx = InterpolatorChannel.COLOR_G.channelID();
            int bIdx = InterpolatorChannel.COLOR_B.channelID();
            float[][][] allColorValues = new float[numComponents][3][];

            for(int ci = 0; ci < numComponents; ++ci) {
               allColorValues[ci][0] = (float[])fValues.get(psfs[ci][rIdx]);
               allColorValues[ci][1] = (float[])fValues.get(psfs[ci][gIdx]);
               allColorValues[ci][2] = (float[])fValues.get(psfs[ci][bIdx]);
            }

            if (XtraConfig.DEBUG) {
               XtraNimations.LOGGER.info("[XtraNimations] Injected color anim: {} cubes, #{}", numComponents, String.format("%06X", packed));
            }

            return new InjectedColorAnim(allColorValues);
         }
      } catch (Exception e) {
         XtraNimations.LOGGER.warn("[XtraNimations] injectColorAnimation failed: {}", e.getMessage());
         return null;
      }
   }

   public static void patchAnimationInterpolators(ModelDefinition def) {
      if (!groupToCubes.isEmpty() && def != null && f_trigAnimations != null) {
         if (initReflection()) {
            Map<Integer, Integer> cubeColors = new HashMap();

            for(Map.Entry<String, List<Object>> e : groupToCubes.entrySet()) {
               int[] rgb = RgbColorStore.get((String)e.getKey());
               int packed = rgb[0] << 16 | rgb[1] << 8 | rgb[2];

               for(Object rendCube : (List)e.getValue()) {
                  try {
                     Object underlying = mh_getCube.invoke(rendCube);
                     int id = f_cubeId.getInt(underlying);
                     cubeColors.put(id, packed);
                  } catch (Throwable var13) {
                  }
               }
            }

            if (!cubeColors.isEmpty()) {
               AnimationRegistry registry = def.getAnimations();
               if (registry != null) {
                  int patched = 0;
                  Set<AnimationTrigger> triggers = registry.getAnimations();
                  if (XtraConfig.DEBUG) {
                     XtraNimations.LOGGER.info("[XtraNimations] RGB patch: {} triggers for {} cubes", triggers.size(), cubeColors.size());
                  }

                  label82:
                  for(AnimationTrigger trigger : triggers) {
                     List<Object> anims;
                     try {
                        anims = (List)f_trigAnimations.get(trigger);
                     } catch (Throwable e) {
                        XtraNimations.LOGGER.warn("[XtraNimations] RGB: f_trigAnimations failed: {}", e.getMessage());
                        continue;
                     }

                     Iterator e = anims.iterator();

                     while(true) {
                        Object animToPatch;
                        while(true) {
                           if (!e.hasNext()) {
                              continue label82;
                           }

                           Object anim = e.next();
                           animToPatch = anim;
                           if (animClass.isInstance(anim)) {
                              break;
                           }

                           try {
                              Field fParent = anim.getClass().getDeclaredField("parent");
                              fParent.setAccessible(true);
                              Object parent = fParent.get(anim);
                              if (parent != null && animClass.isInstance(parent)) {
                                 animToPatch = parent;
                                 break;
                              }
                           } catch (Throwable var15) {
                           }
                        }

                        patched += patchAnimation(animToPatch, def, cubeColors);
                     }
                  }

                  if (XtraConfig.DEBUG) {
                     XtraNimations.LOGGER.info("[XtraNimations] RGB patched {} interpolator channel(s) in animations.", patched);
                  }

               }
            }
         }
      }
   }

   private static int patchAnimation(Object anim, ModelDefinition def, Map<Integer, Integer> cubeColors) {
      Object[][] psfs;
      Object[] componentIDs;
      try {
         psfs = f_psfs.get(anim);
         Field fComps = animClass.getDeclaredField("componentIDs");
         fComps.setAccessible(true);
         componentIDs = fComps.get(anim);
      } catch (Throwable var22) {
         return 0;
      }

      if (psfs != null && componentIDs != null) {
         int patched = 0;

         for(int ci = 0; ci < componentIDs.length; ++ci) {
            Object rendCube = componentIDs[ci];

            int cubeId;
            try {
               Object underlying = mh_getCube.invoke(rendCube);
               cubeId = f_cubeId.getInt(underlying);
            } catch (Throwable var23) {
               continue;
            }

            if (XtraConfig.DEBUG && cubeColors.containsKey(cubeId)) {
               XtraNimations.LOGGER.info("[XtraNimations] RGB patchAnim: comp[{}] cubeId={} inMap=true (WILL PATCH)", ci, cubeId);
            }

            Integer packed = (Integer)cubeColors.get(cubeId);
            if (packed != null) {
               float r = (float)(packed >> 16 & 255);
               float g = (float)(packed >> 8 & 255);
               float b = (float)(packed & 255);
               int[] colorChannels = new int[]{6, 7, 8};
               float[] colorValues = new float[]{r, g, b};

               for(int ch = 0; ch < 3; ++ch) {
                  final int channelIdx = colorChannels[ch];
                  if (channelIdx < psfs[ci].length) {
                     Object interpolator = psfs[ci][channelIdx];
                     if (interpolator != null) {
                        try {
                           Field fValues = getValuesField(interpolator);
                           if (fValues != null) {
                              float[] vals = (float[])fValues.get(interpolator);
                              if (vals != null) {
                                 Arrays.fill(vals, colorValues[ch]);
                                 ++patched;
                              }
                           } else {
                              final float constVal = colorValues[ch];
                              Interpolator constInterp = new Interpolator() {
                                 final int[] callCount = new int[]{0};

                                 public double applyAsDouble(double op) {
                                    if (XtraConfig.DEBUG && this.callCount[0]++ < 3) {
                                       XtraNimations.LOGGER.info("[XtraNimations] constInterp ch={} called, returning {}", channelIdx, constVal);
                                    }

                                    return (double)constVal;
                                 }

                                 public void init(float[] v, DoubleUnaryOperator s) {
                                 }
                              };
                              psfs[ci][channelIdx] = constInterp;
                              ++patched;
                           }
                        } catch (Throwable var21) {
                        }
                     }
                  }
               }
            }
         }

         return patched;
      } else {
         return 0;
      }
   }

   private static Field getValuesField(Object interpolator) {
      Class<?> cls = interpolator.getClass();
      return (Field)interpolatorValuesFieldCache.computeIfAbsent(cls, (c) -> {
         try {
            Field f = c.getDeclaredField("values");
            f.setAccessible(true);
            return f;
         } catch (NoSuchFieldException var2) {
            return null;
         }
      });
   }

   public static void tickColors() {
   }

   public static void tickPeerRetry() {
      if (!pendingPeerColors.isEmpty()) {
         for(UUID uuid : new ArrayList(pendingPeerColors.keySet())) {
            Map<String, Integer> colors = (Map)pendingPeerColors.get(uuid);
            if (colors != null) {
               applyColorsToOtherPlayer(uuid, colors);
            }
         }

      }
   }

   public static void applyColorsToOtherPlayer(UUID playerUuid, Map<String, Integer> groupColors) {
      if (groupColors != null && !groupColors.isEmpty()) {
         if (initReflection()) {
            try {
               class_310 mc = class_310.method_1551();
               if (mc.field_1687 == null) {
                  return;
               }

               class_1657 mcPlayer = mc.field_1687.method_18470(playerUuid);
               if (mcPlayer == null) {
                  if (XtraConfig.DEBUG) {
                     XtraNimations.LOGGER.info("[XtraNimations] RGB peer: player {} not in world", playerUuid);
                  }

                  return;
               }

               Object clientAccess = MinecraftClientAccess.get();
               if (clientAccess == null) {
                  return;
               }

               ModelDefinition def = resolveModelDef(clientAccess, mcPlayer);
               if (def == null) {
                  pendingPeerColors.put(playerUuid, groupColors);
                  if (XtraConfig.DEBUG) {
                     XtraNimations.LOGGER.info("[XtraNimations] RGB peer: no CPM model for {}, queued", playerUuid);
                  }

                  return;
               }

               Map<String, List<Object>> peerGroups = new LinkedHashMap();
               List<Object> parts = null;

               try {
                  parts = (List)f_parts.get(def);
               } catch (Throwable var19) {
               }

               if (parts != null) {
                  for(Object part : parts) {
                     String cls = part.getClass().getName();
                     if (cls.equals("com.tom.cpm.shared.parts.ModelPartRenderEffect")) {
                        checkRenderEffectInto(part, def, peerGroups);
                     } else if (cls.equals("com.tom.cpm.shared.parts.ModelPartDefinition")) {
                        scanPartDefinitionOtherPartsInto(part, def, peerGroups);
                     }
                  }
               }

               List<Object> resolved = null;

               try {
                  resolved = (List)f_resolved.get(def);
               } catch (Throwable var18) {
               }

               if (resolved != null) {
                  for(Object rpart : resolved) {
                     if (rpart.getClass().getName().equals("com.tom.cpm.shared.parts.ModelPartDefinition")) {
                        scanPartDefinitionResolvedOtherPartsInto(rpart, def, peerGroups);
                     }
                  }
               }

               if (peerGroups.isEmpty()) {
                  pendingPeerColors.put(playerUuid, groupColors);
                  if (XtraConfig.DEBUG) {
                     XtraNimations.LOGGER.info("[XtraNimations] RGB peer: model not ready for {}, queued for retry", playerUuid);
                  }

                  return;
               }

               ModelDefinition.ModelLoadingState state = def.getResolveState();
               if (state != ModelLoadingState.LOADED) {
                  pendingPeerColors.put(playerUuid, groupColors);
               } else {
                  pendingPeerColors.remove(playerUuid);
               }

               int applied = 0;

               for(Map.Entry<String, Integer> incoming : groupColors.entrySet()) {
                  List<Object> cubes = (List)peerGroups.get(incoming.getKey());
                  if (cubes != null) {
                     int packed = (Integer)incoming.getValue();
                     String var10000 = String.valueOf(playerUuid);
                     String animKey = var10000 + ":" + (String)incoming.getKey();
                     InjectedColorAnim existing = (InjectedColorAnim)peerInjectedAnims.get(animKey);
                     if (existing != null) {
                        existing.setColor(packed);
                        applied += cubes.size();
                     } else {
                        InjectedColorAnim ica = injectColorAnimation(def, cubes, packed);
                        if (ica != null) {
                           peerInjectedAnims.put(animKey, ica);
                           applied += cubes.size();
                        }
                     }
                  }
               }

               if (XtraConfig.DEBUG) {
                  XtraNimations.LOGGER.info("[XtraNimations] RGB peer {} -> injected/updated {} cubes", playerUuid, applied);
               }
            } catch (Exception e) {
               XtraNimations.LOGGER.warn("[XtraNimations] RGB applyColorsToOtherPlayer: {}", e.getMessage());
            }

         }
      }
   }

   private static void scanPartDefinitionOtherPartsInto(Object partDef, ModelDefinition def, Map<String, List<Object>> out) {
      List<Object> otherParts;
      try {
         otherParts = (List)f_otherParts.get(partDef);
      } catch (Throwable var6) {
         return;
      }

      if (otherParts != null) {
         for(Object other : otherParts) {
            if (other.getClass().getName().equals("com.tom.cpm.shared.parts.ModelPartRenderEffect")) {
               checkRenderEffectInto(other, def, out);
            }
         }

      }
   }

   private static void scanPartDefinitionResolvedOtherPartsInto(Object partDef, ModelDefinition def, Map<String, List<Object>> out) {
      List<Object> resolvedOtherParts;
      try {
         resolvedOtherParts = (List)f_resolvedOtherParts.get(partDef);
      } catch (Throwable var6) {
         return;
      }

      if (resolvedOtherParts != null) {
         for(Object rpart : resolvedOtherParts) {
            if (rpart.getClass().getName().equals("com.tom.cpm.shared.parts.ModelPartRenderEffect")) {
               checkRenderEffectInto(rpart, def, out);
            }
         }

      }
   }

   private static void checkRenderEffectInto(Object renderEffectPart, ModelDefinition def, Map<String, List<Object>> out) {
      Object effect;
      try {
         effect = f_effect.get(renderEffectPart);
      } catch (Throwable var10) {
         return;
      }

      if (effect != null) {
         if (effect.getClass().getName().equals("com.tom.cpm.shared.effects.EffectColor")) {
            int cubeId;
            int tagColor;
            try {
               cubeId = (Integer)f_effectId.get(effect);
               tagColor = (Integer)f_effectColor.get(effect);
            } catch (Throwable var9) {
               return;
            }

            int colorVal = tagColor & 16777215;
            if (colorVal != 16777215 && colorVal != 0) {
               String groupKey = String.format("rgb:%06X", colorVal);
               Object cube = def.getElementById(cubeId);
               if (cube != null) {
                  ((List)out.computeIfAbsent(groupKey, (k) -> new ArrayList())).add(cube);
               }
            }
         }
      }
   }

   private static ModelDefinition resolveModelDef(Object clientAccess, class_1657 mcPlayer) {
      try {
         Object loader = clientAccess.getClass().getMethod("getDefinitionLoader").invoke(clientAccess);
         if (loader == null) {
            return null;
         }

         List<?> players = (List)clientAccess.getClass().getMethod("getPlayers").invoke(clientAccess);
         if (players == null) {
            return null;
         }

         UUID targetUuid = mcPlayer.method_5667();
         Object matchedProfile = null;

         for(Object profile : players) {
            try {
               Object profileId = profile.getClass().getMethod("getId").invoke(profile);
               if (targetUuid.equals(profileId)) {
                  matchedProfile = profile;
                  break;
               }
            } catch (Exception var9) {
            }
         }

         if (matchedProfile == null) {
            if (XtraConfig.DEBUG) {
               XtraNimations.LOGGER.debug("[XtraNimations] RGB peer: GameProfile not found for {}", targetUuid);
            }

            return null;
         }

         Object cpmPlayer = loader.getClass().getMethod("loadPlayer", Object.class, String.class).invoke(loader, matchedProfile, "player");
         if (cpmPlayer == null) {
            return null;
         }

         Object d = cpmPlayer.getClass().getMethod("getModelDefinition").invoke(cpmPlayer);
         if (d instanceof ModelDefinition) {
            return (ModelDefinition)d;
         }
      } catch (Exception e) {
         if (XtraConfig.DEBUG) {
            XtraNimations.LOGGER.debug("[XtraNimations] RGB peer resolveModelDef failed: {}", e.getMessage());
         }
      }

      return null;
   }

   public static Set<String> getDetectedLabels() {
      return groupToCubes.keySet();
   }

   private static void scheduleGuardReset() {
      Thread t = new Thread(() -> {
         try {
            Thread.sleep(2000L);
         } catch (InterruptedException var1) {
         }

         isPatchingInProgress = false;
      }, "xtranims-patch-guard-reset");
      t.setDaemon(true);
      t.start();
   }

   public static void reset() {
      groupToCubes.clear();
      cubeIdToColor.clear();
      cubeIdToTagColor.clear();
      cubeIdToOriginalGroup.clear();
      cubeIdToOriginalTagColor.clear();
      isPatchingInProgress = false;
      lastDef = null;
      interpolatorValuesFieldCache.clear();
      injectedAnims.clear();
      peerInjectedAnims.clear();
      pendingPeerColors.clear();
   }

   public static void clearPeerPlayer(UUID uuid) {
      pendingPeerColors.remove(uuid);
      peerInjectedAnims.entrySet().removeIf((e) -> ((String)e.getKey()).startsWith(uuid.toString()));
   }

   public static class InjectedColorAnim {
      private final float[][][] allColorValues;

      InjectedColorAnim(float[][][] allColorValues) {
         this.allColorValues = allColorValues;
      }

      public void setColor(int packed) {
         float r = (float)(packed >> 16 & 255);
         float g = (float)(packed >> 8 & 255);
         float b = (float)(packed & 255);

         for(float[][] cv : this.allColorValues) {
            cv[0][0] = r;
            cv[1][0] = g;
            cv[2][0] = b;
         }

      }
   }
}
