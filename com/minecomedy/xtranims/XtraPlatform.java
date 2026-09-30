package com.minecomedy.xtranims;

import java.nio.file.Path;
import java.util.ServiceLoader;

public interface XtraPlatform {
   XtraPlatform INSTANCE = (XtraPlatform)ServiceLoader.load(XtraPlatform.class).findFirst().orElseThrow(() -> new IllegalStateException("[XtraNimations] No XtraPlatform implementation found on the classpath — missing META-INF/services entry in the forge/fabric module."));

   Path gameConfigDir();

   String loaderName();
}
