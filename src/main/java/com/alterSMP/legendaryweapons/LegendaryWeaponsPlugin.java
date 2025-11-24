package com.alterSMP.legendaryweapons;

import com.alterSMP.legendaryweapons.altar.AltarManager;
import com.alterSMP.legendaryweapons.altar.AltarPlaceListener;
import com.alterSMP.legendaryweapons.altar.AltarInteractListener;
import com.alterSMP.legendaryweapons.altar.AltarCraftingListener;
import com.alterSMP.legendaryweapons.commands.GiveAltarCommand;
import com.alterSMP.legendaryweapons.commands.AbilityCommand;
import com.alterSMP.legendaryweapons.commands.KResetCommand;
import com.alterSMP.legendaryweapons.data.DataManager;
import com.alterSMP.legendaryweapons.data.CooldownManager;
import com.alterSMP.legendaryweapons.items.LegendaryItemFactory;
import com.alterSMP.legendaryweapons.passives.PassiveEffectManager;
import com.alterSMP.legendaryweapons.abilities.AbilityManager;
import org.bukkit.plugin.java.JavaPlugin;

public class LegendaryWeaponsPlugin extends JavaPlugin {

    private static LegendaryWeaponsPlugin instance;

    private DataManager dataManager;
    private CooldownManager cooldownManager;
    private AltarManager altarManager;
    private LegendaryItemFactory itemFactory;
    private PassiveEffectManager passiveManager;
    private AbilityManager abilityManager;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize managers
        this.dataManager = new DataManager(this);
        this.cooldownManager = new CooldownManager(this);
        this.altarManager = new AltarManager(this);
        this.itemFactory = new LegendaryItemFactory();
        this.abilityManager = new AbilityManager(this);
        this.passiveManager = new PassiveEffectManager(this);

        // Load data
        dataManager.loadAll();
        altarManager.loadAltars();

        // Register commands
        getCommand("givealtar").setExecutor(new GiveAltarCommand(this));
        getCommand("ability").setExecutor(new AbilityCommand(this));
        getCommand("kreset").setExecutor(new KResetCommand(this));

        // Register listeners
        getServer().getPluginManager().registerEvents(new AltarPlaceListener(this), this);
        getServer().getPluginManager().registerEvents(new AltarInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new AltarCraftingListener(this), this);

        // Start passive effect task (runs every 10 ticks = 0.5 seconds)
        passiveManager.startPassiveTask();

        getLogger().info("Legendary Weapons SMP plugin enabled!");
    }

    @Override
    public void onDisable() {
        // Save all data
        if (dataManager != null) {
            dataManager.saveAll();
        }
        if (altarManager != null) {
            altarManager.saveAltars();
        }

        getLogger().info("Legendary Weapons SMP plugin disabled!");
    }

    public static LegendaryWeaponsPlugin getInstance() {
        return instance;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public AltarManager getAltarManager() {
        return altarManager;
    }

    public LegendaryItemFactory getItemFactory() {
        return itemFactory;
    }

    public PassiveEffectManager getPassiveManager() {
        return passiveManager;
    }

    public AbilityManager getAbilityManager() {
        return abilityManager;
    }
}
