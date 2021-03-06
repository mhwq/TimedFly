package com.minestom.Utilities;

import com.minestom.CMDs.Completitions.FlyTabCompletion;
import com.minestom.CMDs.Completitions.MainTabCompletion;
import com.minestom.CMDs.CustomFlyCMD;
import com.minestom.CMDs.FlyCMD;
import com.minestom.CMDs.MainCMD;
import com.minestom.ConfigurationFiles.ItemsConfig;
import com.minestom.ConfigurationFiles.LangFiles;
import com.minestom.Hooks.PlaceholderAPI;
import com.minestom.Hooks.aSkyblock;
import com.minestom.Managers.MySQLManager;
import com.minestom.TimedFly;
import com.minestom.Updater.SpigotUpdater;
import com.minestom.Utilities.GUI.GUIListener;
import com.minestom.Utilities.Others.GeneralListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;

public class Setup {

    public void checkForUpdate(TimedFly plugin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin.getConfig().getBoolean("Check-For-Updates")) {
                    try {
                        new SpigotUpdater(plugin, 48668);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 15 * 34500L);
    }

    /* not used any more
    public void renameConfig(TimedFly plugin) {
        Random random = new Random();
        int i = random.nextInt(99999) + 11111;
        File oldName = new File(plugin.getDataFolder(), "config.yml");
        File newName = new File(plugin.getDataFolder(), "config" + i + ".yml");
        if (oldName.renameTo(newName)) {
            Bukkit.getConsoleSender().sendMessage("§cTimedFly >> Backup made for config.yml");
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.isOp()) {
                    player.sendMessage("§cTimedFly >> §aYour config.yml was outdated so I have created a backup called config" + i + ".yml");
                    player.sendMessage("§cTimedFly >> §aNow creating a new config.yml for you...");
                }
            }
        } else {
            Bukkit.getConsoleSender().sendMessage("§cTimedFly >> Could not backup the config.yml");
        }
    }*/

    public void registerCMD(TimedFly plugin, Utility utility) {
        plugin.getCommand("timedfly").setExecutor(new MainCMD());
        plugin.getCommand("tfly").setExecutor(new FlyCMD(utility));
        plugin.getCommand("timedfly").setTabCompleter(new MainTabCompletion());
        plugin.getCommand("tfly").setTabCompleter(new FlyTabCompletion());
    }

    public void registerListener(TimedFly plugin, MySQLManager sqlManager, Utility utility) {
        PluginManager pm = Bukkit.getServer().getPluginManager();
        pm.registerEvents(new GUIListener(plugin, utility), plugin);
        pm.registerEvents(new GeneralListener(plugin,sqlManager, utility), plugin);
        pm.registerEvents(new CustomFlyCMD(), plugin);

        if (Bukkit.getPluginManager().isPluginEnabled("ASkyBlock") && plugin.getConfig().getBoolean("ASkyblockIntegration")) {
            pm.registerEvents(new aSkyblock(), plugin);
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&cTimedFly >> &7ASkyBlock found, enabling the hook."));
        }
    }

    public void createConfigFiles(TimedFly plugin) {
        LangFiles lang = LangFiles.getInstance();
        ItemsConfig items = ItemsConfig.getInstance();

        lang.createFiles(plugin);
        items.createFiles(plugin);
    }

    public void registerDependencies(TimedFly plugin) {
        if (Bukkit.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderAPI(plugin).hook();
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&cTimedFly >> &7PlaceholderAPI found, using it for item's lore and name."));
        } else {
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&cTimedFly >> &7PlaceholderAPI not found, you should install it"));
        }
    }

}
