package org.saintqd.vineriumcore.suffix;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class CommunitySuffix {

    private final String suffixName;
    private final String permission;
    private int suffixLimit;
    private final List<String> users = new ArrayList<>();

    public CommunitySuffix(String suffixName, String communitySuffixPermission, ConfigurationSection config) {
        this.suffixName = suffixName;
        this.permission = communitySuffixPermission;
        if (config != null) {
            this.suffixLimit = config.getInt("Limit", 10);
            users.addAll(config.getStringList("Users"));
        }
        else
            this.suffixLimit = 10;
    }

    public String getSuffixName() {
        return suffixName;
    }

    public String getPermission() {
        return permission;
    }

    public int getSuffixLimit() {
        return suffixLimit;
    }

    public void setSuffixLimit(int suffixLimit) {
        this.suffixLimit = suffixLimit;
    }

    public List<String> getUsers() {
        return users;
    }

    public boolean isPossibleToAdd() {
        return users.size() < suffixLimit;
    }
}
