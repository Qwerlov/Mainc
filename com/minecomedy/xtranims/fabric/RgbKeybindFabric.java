package com.minecomedy.xtranims.fabric;

import com.minecomedy.xtranims.RgbKeybind;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

public class RgbKeybindFabric {
   public static void register() {
      KeyBindingHelper.registerKeyBinding(RgbKeybind.KEY_OPEN_RGB);
   }
}
