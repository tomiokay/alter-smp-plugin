package com.alterSMP.combatlogger;

import org.bukkit.plugin.java.JavaPlugin;

public class CombatLoggerPlugin extends JavaPlugin {

    private static CombatLoggerPlugin instance;
    private CombatManager combatManager;

    @Override
    public void onEnable() {
        instance = this;
        combatManager = new CombatManager(this);

        getServer().getPluginManager().registerEvents(combatManager, this);

        getLogger().info("CombatLogger enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("CombatLogger disabled!");
    }

    public static CombatLoggerPlugin getInstance() {
        return instance;
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }
}
