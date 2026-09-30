package com.minecomedy.xtranims;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_342;
import net.minecraft.class_4185;
import net.minecraft.class_437;

public class RgbColorScreen extends class_437 {
   private static final int PANEL_W = 224;
   private static final int SLIDER_H = 14;
   private static final int PREVIEW = 44;
   private static final int BTN_H = 18;
   private static final int PAD = 8;
   private static final int LH = 9;
   private static final int ROW_GAP = 6;
   private List<String> keys;
   private int selectedIndex = 0;
   private int r = 255;
   private int g = 255;
   private int b = 255;
   private int sliderX;
   private int sliderW;
   private int sliderRY;
   private int sliderGY;
   private int sliderBY;
   private boolean draggingR;
   private boolean draggingG;
   private boolean draggingB;
   private class_342 hexField;
   private class_342 nameField;
   private boolean syncingHex = false;
   private String lastNameValue = "";
   private int px;
   private int py;
   private int panelH;
   private int yTitle;
   private int yNavRow;
   private int yNameLabel;
   private int yNameField;
   private int yRLabel;
   private int yGLabel;
   private int yBLabel;
   private int yHexLabel;
   private int yHexRow;
   private int yPreviewRow;
   private int ySave;
   private int yClose;
   private String cachedHex = "#FFFFFF";

   public RgbColorScreen() {
      super(class_2561.method_43470("XtraNimations — RGB Color"));
   }

   protected void method_25426() {
      this.keys = new ArrayList(RgbReflectionHelper.getDetectedLabels());
      Collections.sort(this.keys);
      if (this.keys.isEmpty()) {
         this.method_37063(class_4185.method_46430(class_2561.method_43470("Close"), (b) -> this.method_25419()).method_46434(this.field_22789 / 2 - 40, this.field_22790 / 2 + 10, 80, 18).method_46431());
      } else {
         if (this.selectedIndex >= this.keys.size()) {
            this.selectedIndex = 0;
         }

         this.loadSelected();
         this.cachedHex = toHex(this.r, this.g, this.b);
         int cx = 0;
         int cw = 208;
         int y = 8;
         this.yTitle = y;
         y += 15;
         this.yNavRow = y;
         y += 24;
         this.yNameLabel = y;
         y += 11;
         this.yNameField = y;
         y += 24;
         this.yRLabel = y;
         this.sliderRY = 0;
         y += 31;
         this.yGLabel = y;
         this.sliderGY = 0;
         y += 31;
         this.yBLabel = y;
         this.sliderBY = 0;
         y += 35;
         this.yHexLabel = y;
         y += 11;
         this.yHexRow = y;
         this.yPreviewRow = y;
         y += Math.max(14, 44) + 6 + 4;
         this.ySave = y;
         y += 22;
         this.yClose = y;
         y += 26;
         this.panelH = y;
         this.px = (this.field_22789 - 224) / 2;
         this.py = (this.field_22790 - this.panelH) / 2;
         this.sliderRY = this.py + this.yRLabel + 9 + 2;
         this.sliderGY = this.py + this.yGLabel + 9 + 2;
         this.sliderBY = this.py + this.yBLabel + 9 + 2;
         this.sliderX = this.px + 8;
         this.sliderW = cw;
         int ax = this.px + 8;
         this.method_37063(class_4185.method_46430(class_2561.method_43470("<"), (b) -> this.switchGroup(-1)).method_46434(ax, this.py + this.yNavRow, 20, 18).method_46431());
         this.method_37063(class_4185.method_46430(class_2561.method_43470(">"), (b) -> this.switchGroup(1)).method_46434(ax + cw - 20, this.py + this.yNavRow, 20, 18).method_46431());
         this.lastNameValue = RgbColorStore.getDisplayName((String)this.keys.get(this.selectedIndex));
         this.nameField = new class_342(this.field_22793, ax, this.py + this.yNameField, cw, 18, class_2561.method_43470("Name"));
         this.nameField.method_1880(40);
         this.nameField.method_1852(this.lastNameValue);
         this.nameField.method_1863((text) -> {
            if (!text.equals(this.lastNameValue)) {
               this.lastNameValue = text;
               RgbColorStore.setNameInMemory((String)this.keys.get(this.selectedIndex), text);
            }

         });
         this.method_37063(this.nameField);
         int hexW = cw - 44 - 8;
         this.hexField = new class_342(this.field_22793, ax, this.py + this.yHexRow, hexW, 14, class_2561.method_43470("HEX"));
         this.hexField.method_1880(7);
         this.hexField.method_1852(this.cachedHex);
         this.hexField.method_1863(this::onHexTyped);
         this.method_37063(this.hexField);
         this.method_37063(class_4185.method_46430(class_2561.method_43470("Save"), (b) -> this.saveAndApply()).method_46434(ax, this.py + this.ySave, cw, 18).method_46431());
         this.method_37063(class_4185.method_46430(class_2561.method_43470("Close"), (b) -> this.method_25419()).method_46434(ax, this.py + this.yClose, cw, 18).method_46431());
      }
   }

   public void method_25394(class_332 gfx, int mouseX, int mouseY, float partialTick) {
      if (this.keys != null && !this.keys.isEmpty()) {
         gfx.method_25294(this.px, this.py, this.px + 224, this.py + this.panelH, -434891752);
         gfx.method_25294(this.px + 2, this.py + 2, this.px + 224 - 2, this.py + 2 + 9 + 6, -14803418);
         gfx.method_49601(this.px, this.py, 224, this.panelH, -10066313);
         gfx.method_49601(this.px + 1, this.py + 1, 222, this.panelH - 2, -14013898);
         int mid = this.px + 112;
         int ax = this.px + 8;
         int cw = 208;
         gfx.method_25300(this.field_22793, "§lRGB Color Picker", mid, this.py + this.yTitle, -1);
         gfx.method_25300(this.field_22793, "§7" + (String)this.keys.get(this.selectedIndex), mid, this.py + this.yNavRow + 4, -5592406);
         gfx.method_51433(this.field_22793, "§7Group name:", ax, this.py + this.yNameLabel, -4473925, false);
         gfx.method_51433(this.field_22793, "§cRed: §f" + this.r, ax, this.py + this.yRLabel, -1, false);
         this.drawSlider(gfx, ax, this.sliderRY, cw, 14, this.r, -3394765, -2009923584);
         gfx.method_51433(this.field_22793, "§aGreen: §f" + this.g, ax, this.py + this.yGLabel, -1, false);
         this.drawSlider(gfx, ax, this.sliderGY, cw, 14, this.g, -13382605, -2013252864);
         gfx.method_51433(this.field_22793, "§9Blue: §f" + this.b, ax, this.py + this.yBLabel, -1, false);
         this.drawSlider(gfx, ax, this.sliderBY, cw, 14, this.b, -13421620, -2013265869);
         gfx.method_51433(this.field_22793, "§7Hex:", ax, this.py + this.yHexLabel, -4473925, false);
         int previewX = ax + cw - 44;
         gfx.method_25294(previewX, this.py + this.yPreviewRow, previewX + 44, this.py + this.yPreviewRow + 44, -16777216 | this.r << 16 | this.g << 8 | this.b);
         gfx.method_49601(previewX, this.py + this.yPreviewRow, 44, 44, -7829368);
         super.method_25394(gfx, mouseX, mouseY, partialTick);
      } else {
         gfx.method_25294(0, 0, this.field_22789, this.field_22790, -2013265920);
         gfx.method_25300(this.field_22793, "No RGB groups found in model.", this.field_22789 / 2, this.field_22790 / 2 - 12, -22016);
         gfx.method_25300(this.field_22793, "Add a Color Filter render effect to any cube.", this.field_22789 / 2, this.field_22790 / 2 + 2, -3355444);
         super.method_25394(gfx, mouseX, mouseY, partialTick);
      }
   }

   private void drawSlider(class_332 gfx, int x, int y, int w, int h, int value, int fill, int bg) {
      gfx.method_25294(x, y, x + w, y + h, bg);
      int fw = (int)((float)value / 255.0F * (float)w);
      if (fw > 0) {
         gfx.method_25294(x, y, x + fw, y + h, fill);
      }

      gfx.method_49601(x, y, w, h, -11184794);
      int kx = x + fw;
      gfx.method_25294(Math.max(x, kx - 1), y, Math.min(x + w, kx + 2), y + h, -1);
   }

   public boolean method_25402(double mx, double my, int btn) {
      if (btn == 0) {
         if (this.onSlider(mx, my, this.sliderRY)) {
            this.draggingR = true;
            this.applySlider('R', mx);
            return true;
         }

         if (this.onSlider(mx, my, this.sliderGY)) {
            this.draggingG = true;
            this.applySlider('G', mx);
            return true;
         }

         if (this.onSlider(mx, my, this.sliderBY)) {
            this.draggingB = true;
            this.applySlider('B', mx);
            return true;
         }
      }

      return super.method_25402(mx, my, btn);
   }

   public boolean method_25403(double mx, double my, int btn, double dx, double dy) {
      if (this.draggingR) {
         this.applySlider('R', mx);
         return true;
      } else if (this.draggingG) {
         this.applySlider('G', mx);
         return true;
      } else if (this.draggingB) {
         this.applySlider('B', mx);
         return true;
      } else {
         return super.method_25403(mx, my, btn, dx, dy);
      }
   }

   public boolean method_25406(double mx, double my, int btn) {
      this.draggingR = this.draggingG = this.draggingB = false;
      return super.method_25406(mx, my, btn);
   }

   private boolean onSlider(double mx, double my, int sy) {
      return mx >= (double)this.sliderX && mx <= (double)(this.sliderX + this.sliderW) && my >= (double)sy && my <= (double)(sy + 14);
   }

   private void applySlider(char ch, double mx) {
      int val = Math.max(0, Math.min(255, (int)((mx - (double)this.sliderX) / (double)this.sliderW * (double)255.0F)));
      switch (ch) {
         case 'B' -> this.b = val;
         case 'G' -> this.g = val;
         case 'R' -> this.r = val;
      }

      this.updateHex();
   }

   private void onHexTyped(String text) {
      if (!this.syncingHex) {
         String s = text.startsWith("#") ? text.substring(1) : text;
         if (s.length() == 6) {
            try {
               int packed = Integer.parseInt(s, 16);
               int nr = packed >> 16 & 255;
               int ng = packed >> 8 & 255;
               int nb = packed & 255;
               if (nr == this.r && ng == this.g && nb == this.b) {
                  return;
               }

               this.r = nr;
               this.g = ng;
               this.b = nb;
            } catch (NumberFormatException var7) {
            }
         }

      }
   }

   private void updateHex() {
      String newHex = toHex(this.r, this.g, this.b);
      if (!newHex.equals(this.cachedHex)) {
         this.cachedHex = newHex;
         if (this.hexField != null) {
            this.syncingHex = true;
            this.hexField.method_1852(this.cachedHex);
            this.syncingHex = false;
         }
      }
   }

   private static String toHex(int r, int g, int b) {
      return String.format("#%02X%02X%02X", r, g, b);
   }

   private void saveAndApply() {
      if (this.keys != null && !this.keys.isEmpty()) {
         String key = (String)this.keys.get(this.selectedIndex);
         RgbColorStore.setInMemory(key, this.r, this.g, this.b);
         RgbColorStore.save(key, RgbReflectionHelper.getGroupToCubeId());
         RgbReflectionHelper.applyColorsForced();
      }
   }

   private void revertCurrent() {
      if (this.keys != null && !this.keys.isEmpty()) {
         RgbColorStore.revert((String)this.keys.get(this.selectedIndex));
         RgbReflectionHelper.applyColors();
      }
   }

   private void switchGroup(int dir) {
      this.selectedIndex = (this.selectedIndex + dir + this.keys.size()) % this.keys.size();
      this.reloadScreen();
   }

   private void loadSelected() {
      if (this.keys != null && !this.keys.isEmpty()) {
         int[] rgb = RgbColorStore.get((String)this.keys.get(this.selectedIndex));
         this.r = rgb[0];
         this.g = rgb[1];
         this.b = rgb[2];
      }
   }

   private void reloadScreen() {
      this.loadSelected();
      this.method_37067();
      this.method_25426();
   }

   public void method_25419() {
      if (this.keys != null && !this.keys.isEmpty()) {
         RgbColorStore.revert((String)this.keys.get(this.selectedIndex));
      }

      super.method_25419();
   }

   public boolean method_25421() {
      return true;
   }
}
