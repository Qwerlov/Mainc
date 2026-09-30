package com.minecomedy.xtranims.fabric;

import com.minecomedy.xtranims.XtraPlatform;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public class XtraPlatformFabric implements XtraPlatform {
   public Path gameConfigDir() {
      return FabricLoader.getInstance().getConfigDir();
   }

   public String loaderName() {
      return "fabric";
   }
}
