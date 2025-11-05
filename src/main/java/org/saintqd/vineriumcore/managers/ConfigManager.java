package org.saintqd.vineriumcore.managers;

import org.bukkit.configuration.file.YamlConfiguration;
import org.saintqd.vineriumcore.VineriumCore;
import org.saintqd.vineriumlib.utils.VinUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ConfigManager {

    public void checkConfigs() {
        File suffixFile = new File(VineriumCore.inst().getMainDirectory()+"Suffixes.yml");
        File parent = suffixFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            VinUtils.sendDebugMessage(0,"<red>Couldn't create suffix config!");
            return;
        }
        YamlConfiguration suffixYaml = YamlConfiguration.loadConfiguration(suffixFile);
        suffixYaml.addDefault("HideWithoutPermission",true);
        suffixYaml.addDefault("Suffixes.Test.Display","<gray>Тестовый суффикс");
        suffixYaml.addDefault("Suffixes.Test.Desc", List.of("<white>Описание тестового суффикса.","<white>Может быть многострочным."));
        suffixYaml.addDefault("Suffixes.Test.Model","test");
        suffixYaml.addDefault("Suffixes.Test.Permission","vineriumcore.suffix.test");
        suffixYaml.addDefault("Suffixes.Test.Symbol","+");
        suffixYaml.options().copyDefaults(true);
        try {
            suffixYaml.save(suffixFile);
        } catch (IOException e) {
            VinUtils.sendDebugMessage(0,"<red>Couldn't save suffix config to "+ suffixFile +"!");
        }
    }
}
