package com.minecomedy.xtranims;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class XtraNimations {
   public static final String MODID = "xtranims";
   public static final String MOD_ID = "xtranims";
   public static final Logger LOGGER = LogManager.getLogger("xtranims");

   public static void initClient() {
      XtraConfig.load();
      NbtTriggerLoader.load();
      EffectTriggerLoader.load();
      BiomeTriggerLoader.load();
      RgbColorStore.loadAll();
      LOGGER.info("[XtraNimations] Common client init complete ({}).", XtraPlatform.INSTANCE.loaderName());
   }
}
