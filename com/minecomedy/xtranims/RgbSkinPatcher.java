package com.minecomedy.xtranims;

import com.tom.cpl.nbt.NBTTagCompound;
import com.tom.cpm.shared.MinecraftClientAccess;
import com.tom.cpm.shared.config.ModConfig;
import com.tom.cpm.shared.io.ModelFile;
import com.tom.cpm.shared.network.packet.SetSkinC2S;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Map;

public class RgbSkinPatcher {
   public static void patchAndResend(Map<Integer, Integer> cubeIdToColor, Map<Integer, Integer> cubeIdToOriginal) {
      if (!cubeIdToColor.isEmpty()) {
         boolean hasServer = MinecraftClientAccess.get().getNetHandler().hasModClient();
         if (XtraConfig.DEBUG) {
            XtraNimations.LOGGER.info("[XtraNimations] RGB patch: hasModClient={}", hasServer);
         }

         if (hasServer) {
            try {
               String modelName = ModConfig.getCommonConfig().getString("selectedModel", (String)null);
               if (".temp.cpmmodel".equals(modelName)) {
                  String old = ModConfig.getCommonConfig().getString("selectedModelOld", (String)null);
                  if (old != null && !old.equals("~~VANILLA~~")) {
                     modelName = old;
                  }
               }

               if (XtraConfig.DEBUG) {
                  XtraNimations.LOGGER.info("[XtraNimations] RGB patch: modelName={}", modelName);
               }

               if (modelName == null) {
                  return;
               }

               File modelsDir = new File(MinecraftClientAccess.get().getGameDir(), "player_models");
               File modelFile = new File(modelsDir, modelName);
               if (!modelFile.exists()) {
                  XtraNimations.LOGGER.warn("[XtraNimations] RGB patch: model file not found: {}", modelFile);
                  return;
               }

               ModelFile file = ModelFile.load(modelFile);
               byte[] dataBlock = file.getDataBlock();

               try {
                  Field fOverflow = ModelFile.class.getDeclaredField("overflowLocal");
                  fOverflow.setAccessible(true);
                  byte[] overflow = (byte[])fOverflow.get(file);
                  if (overflow != null && overflow.length > 3) {
                     byte[] inner = new byte[overflow.length - 3];
                     System.arraycopy(overflow, 1, inner, 0, inner.length);
                     inner = patchRawBytes(inner, cubeIdToColor, cubeIdToOriginal);
                     if (inner == null) {
                        XtraNimations.LOGGER.warn("[XtraNimations] RGB patch: patchRawBytes found no matches in overflowLocal");
                        return;
                     }

                     ByteArrayOutputStream baos = new ByteArrayOutputStream();
                     baos.write(83);
                     baos.write(3);
                     writeVarIntTo(baos, inner.length);
                     baos.write(inner);
                     baos.write(0);
                     baos.write(0);
                     byte[] body = baos.toByteArray();
                     short sum = 0;

                     for(int i = 1; i < body.length; ++i) {
                        sum = (short)(sum + (body[i] & 255));
                     }

                     baos.write(sum >> 8 & 255);
                     baos.write(sum & 255);
                     dataBlock = baos.toByteArray();
                     if (XtraConfig.DEBUG) {
                        XtraNimations.LOGGER.info("[XtraNimations] RGB patch: gist model inlined as DEFINITION block ({} bytes)", dataBlock.length);
                     }
                  }
               } catch (Exception ex) {
                  XtraNimations.LOGGER.warn("[XtraNimations] RGB patch: overflowLocal wrap failed: {}", ex.getMessage());
               }

               if (dataBlock == null || dataBlock.length == 0) {
                  return;
               }

               byte[] patched = patchDataBlock(dataBlock, cubeIdToColor, cubeIdToOriginal);
               if (patched == null) {
                  return;
               }

               NBTTagCompound tag = new NBTTagCompound();
               tag.setByteArray("data", patched);
               MinecraftClientAccess.get().getNetHandler().sendPacketToServer(new SetSkinC2S(tag));
               if (XtraConfig.DEBUG) {
                  XtraNimations.LOGGER.info("[XtraNimations] RGB skin patch sent ({} cubes, {} bytes).", cubeIdToColor.size(), patched.length);
               }
            } catch (Exception e) {
               XtraNimations.LOGGER.warn("[XtraNimations] RGB skin patch failed: {}", e.getMessage(), e);
            }

         }
      }
   }

   private static byte[] patchDataBlock(byte[] data, Map<Integer, Integer> cubeIdToColor, Map<Integer, Integer> cubeIdToOriginal) {
      byte[] out = (byte[])(([B)data).clone();
      int patchCount = 0;
      int checksumDelta = 0;
      int streamEnd = out.length - 2;

      for(Map.Entry<Integer, Integer> entry : cubeIdToColor.entrySet()) {
         int cubeId = (Integer)entry.getKey();
         int newColor = (Integer)entry.getValue();
         byte[] varint = encodeVarInt(cubeId);
         byte newR = (byte)(newColor >> 16 & 255);
         byte newG = (byte)(newColor >> 8 & 255);
         byte newB = (byte)(newColor & 255);
         int matchPos = -1;

         for(int i = 3; i < streamEnd - varint.length - 2; ++i) {
            int pre1 = out[i - 2] & 255;
            int pre2 = out[i - 1] & 255;
            if ((pre1 == 5 || pre1 == 6) && pre2 == 3) {
               boolean varMatch = true;

               for(int v = 0; v < varint.length; ++v) {
                  if (out[i + v] != varint[v]) {
                     varMatch = false;
                     break;
                  }
               }

               if (varMatch) {
                  int rgbPos = i + varint.length;
                  if (rgbPos + 2 < streamEnd) {
                     matchPos = i;
                     break;
                  }
               }
            }
         }

         if (matchPos == -1) {
            XtraNimations.LOGGER.warn("[XtraNimations] RGB: no match found for cubeId={}", cubeId);
         } else {
            int rgbPos = matchPos + varint.length;
            checksumDelta += (newR & 255) - (out[rgbPos] & 255);
            checksumDelta += (newG & 255) - (out[rgbPos + 1] & 255);
            checksumDelta += (newB & 255) - (out[rgbPos + 2] & 255);
            out[rgbPos] = newR;
            out[rgbPos + 1] = newG;
            out[rgbPos + 2] = newB;
            ++patchCount;
         }
      }

      if (patchCount == 0) {
         XtraNimations.LOGGER.warn("[XtraNimations] RGB patch: no cubes matched in dataBlock (cubesWanted={})", cubeIdToColor.keySet());
         return null;
      } else {
         int origChecksum = (out[out.length - 2] & 255) << 8 | out[out.length - 1] & 255;
         int newChecksum = origChecksum + checksumDelta & '\uffff';
         out[out.length - 2] = (byte)(newChecksum >> 8 & 255);
         out[out.length - 1] = (byte)(newChecksum >> 0 & 255);
         return out;
      }
   }

   public static byte[] encodeVarInt(int value) {
      ByteArrayOutputStream baos;
      for(baos = new ByteArrayOutputStream(5); (value & -128) != 0; value >>>= 7) {
         baos.write(value & 127 | 128);
      }

      baos.write(value);
      return baos.toByteArray();
   }

   private static byte[] patchRawBytes(byte[] data, Map<Integer, Integer> cubeIdToColor, Map<Integer, Integer> cubeIdToOriginal) {
      byte[] out = (byte[])(([B)data).clone();
      int patchCount = 0;
      int streamEnd = out.length;

      for(Map.Entry<Integer, Integer> entry : cubeIdToColor.entrySet()) {
         int cubeId = (Integer)entry.getKey();
         int newColor = (Integer)entry.getValue();
         byte[] varint = encodeVarInt(cubeId);
         byte newR = (byte)(newColor >> 16 & 255);
         byte newG = (byte)(newColor >> 8 & 255);
         byte newB = (byte)(newColor & 255);
         int matchPos = -1;

         for(int i = 3; i < streamEnd - varint.length - 2; ++i) {
            int pre1 = out[i - 2] & 255;
            int pre2 = out[i - 1] & 255;
            if ((pre1 == 5 || pre1 == 6) && pre2 == 3) {
               boolean varMatch = true;

               for(int v = 0; v < varint.length; ++v) {
                  if (out[i + v] != varint[v]) {
                     varMatch = false;
                     break;
                  }
               }

               if (varMatch) {
                  int rgbPos = i + varint.length;
                  if (rgbPos + 2 < streamEnd) {
                     matchPos = i;
                     break;
                  }
               }
            }
         }

         if (matchPos == -1) {
            XtraNimations.LOGGER.warn("[XtraNimations] RGB raw patch: no match for cubeId={}", cubeId);
         } else {
            int rgbPos = matchPos + varint.length;
            out[rgbPos] = newR;
            out[rgbPos + 1] = newG;
            out[rgbPos + 2] = newB;
            ++patchCount;
         }
      }

      return patchCount == 0 ? null : out;
   }

   private static void writeVarIntTo(ByteArrayOutputStream baos, int value) {
      while((value & -128) != 0) {
         baos.write(value & 127 | 128);
         value >>>= 7;
      }

      baos.write(value);
   }
}
