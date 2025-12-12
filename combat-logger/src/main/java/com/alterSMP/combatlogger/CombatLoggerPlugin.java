package com.alterSMP.combatlogger;

import org.bukkit.plugin.java.JavaPlugin;

public class CombatLoggerPlugin extends JavaPlugin {

    private static CombatLoggerPlugin instance;
    private CombatManager combatManager;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        saveDefaultConfig();

        combatManager = new CombatManager(this);

        getServer().getPluginManager().registerEvents(combatManager, this);

        // Register commands
        getCommand("togglecombat").setExecutor(new ToggleCombatTimerCommand(this));
        getCommand("togglepvp").setExecutor(new TogglePvPCommand(this));
        CombatTagCommand combatTagCmd = new CombatTagCommand(this);
        getCommand("combattag").setExecutor(combatTagCmd);
        getCommand("combattag").setTabCompleter(combatTagCmd);

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
