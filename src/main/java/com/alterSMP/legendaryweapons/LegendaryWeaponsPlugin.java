package com.alterSMP.legendaryweapons;

import com.alterSMP.legendaryweapons.altar.AltarManager;
import com.alterSMP.legendaryweapons.altar.AltarPlaceListener;
import com.alterSMP.legendaryweapons.altar.AltarInteractListener;
import com.alterSMP.legendaryweapons.altar.AltarCraftingListener;
import com.alterSMP.legendaryweapons.commands.GiveAltarCommand;
import com.alterSMP.legendaryweapons.commands.AbilityCommand;
import com.alterSMP.legendaryweapons.commands.KResetCommand;
import com.alterSMP.legendaryweapons.commands.KResetPlayerCommand;
import com.alterSMP.legendaryweapons.commands.LReloadCommand;
import com.alterSMP.legendaryweapons.commands.CooldownCommand;
import com.alterSMP.legendaryweapons.commands.TrustCommand;
import com.alterSMP.legendaryweapons.commands.GiveLegendaryCommand;
import com.alterSMP.legendaryweapons.config.ConfigManager;
import com.alterSMP.legendaryweapons.data.DataManager;
import com.alterSMP.legendaryweapons.data.CooldownManager;
import com.alterSMP.legendaryweapons.data.TrustManager;
import com.alterSMP.legendaryweapons.items.LegendaryItemFactory;
import com.alterSMP.legendaryweapons.passives.PassiveEffectManager;
import com.alterSMP.legendaryweapons.passives.ArmorPassivesListener;
import com.alterSMP.legendaryweapons.abilities.AbilityManager;
import com.alterSMP.legendaryweapons.abilities.CopperPickaxeListener;
import com.alterSMP.legendaryweapons.listeners.AnvilProtectionListener;
import com.alterSMP.legendaryweapons.listeners.RestrictedItemsListener;
import org.bukkit.plugin.java.JavaPlugin;

public class LegendaryWeaponsPlugin extends JavaPlugin {

    private static LegendaryWeaponsPlugin instance;

    private ConfigManager configManager;
    private DataManager dataManager;
    private CooldownManager cooldownManager;
    private TrustManager trustManager;
    private AltarManager altarManager;
    private LegendaryItemFactory itemFactory;
    private PassiveEffectManager passiveManager;
    private AbilityManager abilityManager;
    private ArmorPassivesListener armorPassivesListener;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize config first
        this.configManager = new ConfigManager(this);

        // Initialize managers
        this.dataManager = new DataManager(this);
        this.cooldownManager = new CooldownManager(this);
        this.trustManager = new TrustManager(this);
        this.altarManager = new AltarManager(this);
        this.itemFactory = new LegendaryItemFactory();
        this.abilityManager = new AbilityManager(this);
        this.passiveManager = new PassiveEffectManager(this);

        // Load data
        dataManager.loadAll();
        altarManager.loadAltars();

        // Register commands
        getCommand("giveforge").setExecutor(new GiveAltarCommand(this));
        getCommand("ability").setExecutor(new AbilityCommand(this));
        getCommand("kreset").setExecutor(new KResetCommand(this));
        KResetPlayerCommand kresetPlayerCmd = new KResetPlayerCommand(this);
        getCommand("kresetplayer").setExecutor(kresetPlayerCmd);
        getCommand("kresetplayer").setTabCompleter(kresetPlayerCmd);
        getCommand("lreload").setExecutor(new LReloadCommand(this));
        getCommand("cooldown").setExecutor(new CooldownCommand(this));
        TrustCommand trustCmd = new TrustCommand(this);
        getCommand("trust").setExecutor(trustCmd);
        getCommand("trust").setTabCompleter(trustCmd);
        GiveLegendaryCommand giveLegendaryCmd = new GiveLegendaryCommand(this);
        getCommand("givelegendary").setExecutor(giveLegendaryCmd);
        getCommand("givelegendary").setTabCompleter(giveLegendaryCmd);

        // Register listeners
        getServer().getPluginManager().registerEvents(new AltarPlaceListener(this), this);
        getServer().getPluginManager().registerEvents(new AltarInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new AltarCraftingListener(this), this);
        getServer().getPluginManager().registerEvents(new CopperPickaxeListener(this), this);
        this.armorPassivesListener = new ArmorPassivesListener(this);
        getServer().getPluginManager().registerEvents(armorPassivesListener, this);
        getServer().getPluginManager().registerEvents(new AnvilProtectionListener(), this);
        getServer().getPluginManager().registerEvents(new RestrictedItemsListener(), this);

        // Start passive effect task (uses config interval)
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
        if (trustManager != null) {
            trustManager.saveTrustData();
        }

        getLogger().info("Legendary Weapons SMP plugin disabled!");
    }

    public static LegendaryWeaponsPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
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

    public TrustManager getTrustManager() {
        return trustManager;
    }

    public ArmorPassivesListener getArmorPassivesListener() {
        return armorPassivesListener;
    }
}
