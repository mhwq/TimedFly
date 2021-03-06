package com.minestom;

import com.minestom.ConfigurationFiles.UpdateConfig;
import com.minestom.Managers.MySQLManager;
import com.minestom.NMS.NMS;
import com.minestom.NMS.v_1_10.v_1_10_R1;
import com.minestom.NMS.v_1_11.v_1_11_R1;
import com.minestom.NMS.v_1_12.v_1_12_R1;
import com.minestom.NMS.v_1_8.v_1_8_R1;
import com.minestom.NMS.v_1_8.v_1_8_R2;
import com.minestom.NMS.v_1_8.v_1_8_R3;
import com.minestom.NMS.v_1_9.v_1_9_R1;
import com.minestom.NMS.v_1_9.v_1_9_R2;
import com.minestom.Utilities.BossBarManager;
import com.minestom.Utilities.GUI.GUIListener;
import com.minestom.Utilities.Metrics;
import com.minestom.Utilities.Others.PlayerCache;
import com.minestom.Utilities.Setup;
import com.minestom.Utilities.Utility;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class TimedFly extends JavaPlugin {

    private ConsoleCommandSender log = Bukkit.getServer().getConsoleSender();
    private static TimedFly instance;
    private BossBarManager bossBarManager;

    public static TimedFly getInstance() {
        return instance;
    }

    private Connection connection;
    private Economy economy = null;
    private Setup setup = new Setup();
    private NMS nms;
    private Utility utility;
    private UpdateConfig updateConfig = new UpdateConfig();
    private MySQLManager sqlManager;

    public Connection getConnection() {
        return connection;
    }

    private void setConnection(Connection connection) {
        this.connection = connection;
    }

    public NMS getNMS() {
        return nms;
    }

    public Economy getEconomy() {
        return economy;
    }

    public BossBarManager getBossBarManager() {
        return bossBarManager;
    }

    public Utility getUtility() {
        return utility;
    }

    @Override
    public void onEnable() {
        sqlManager = new MySQLManager(this);
        instance = this;
        if (getConfig().getBoolean("BossBarTimer.Enabled")) {
            bossBarManager = new BossBarManager(this);
        }
        utility = new Utility(this);
        if (!new File(this.getDataFolder(), "config.yml").exists()) {
            this.saveDefaultConfig();
        }

        setup.createConfigFiles(this);
        setup.registerCMD(this, utility);
        setup.registerListener(this, sqlManager, utility);
        setup.registerDependencies(this);
        setupBStats();

        if (!setupEconomy()) {
            utility.message(log, ("&cDisabled due to no Vault dependency found!"));
            getServer().getPluginManager().disablePlugin(this);
            return;
        } else {
            utility.message(log, ("&7Hooked to Vault."));
        }

        if (utility.isTokenManagerEnabled()) {
            utility.message(log, ("&7Hooked to TokenManager."));
        } else {
            utility.message(log, ("&7TokenManager not found disabling tokens currency."));
        }

        if (setupNMS()) {
            utility.message(log, ("&7Actionbar, Titles, Subtitles support was successfully enabled!"));
        } else {
            utility.message(log, ("&cFailed to setup Actionbar, Titles, Subtitles and Particles support!"));
            utility.message(log, ("&cYour server version is not compatible with this instance!"));
            utility.message(log, ("&cThe instance will not function correctly!"));
        }

        if (!mysqlSetup()) {
            utility.message(log, ("&cDisabled due to MySQL error!"));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        setup.checkForUpdate(this);
        updateConfig.updateConfig(this);

        Bukkit.getOnlinePlayers().forEach(player -> {
            utility.getPlayerCacheMap().put(player, new PlayerCache(sqlManager, player));
            PlayerCache playerCache = utility.getPlayerCacheMap().get(player);

            int time = playerCache.getTimeLeft();
            if (!(time <= 0)) GUIListener.flytime.put(player.getUniqueId(), time);

            if (GUIListener.flytime.containsKey(player.getUniqueId())) player.setAllowFlight(true);
        });
        utility.message(log, ("&7The Plugin has been enabled and its ready to use."));
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerCache playerCache = utility.getPlayerCacheMap().get(player);
            if (GUIListener.flytime.containsKey(player.getUniqueId()) && (utility.isWorldEnabled(player, player.getWorld()) && player.getAllowFlight())) {
                player.setAllowFlight(false);
                player.setFlying(false);
            }
            sqlManager.setTimeLeft(player, playerCache.getTimeLeft());
            if (player.getOpenInventory().getTitle().equals(ChatColor.translateAlternateColorCodes('&',
                    getConfig().getString("Gui.DisplayName")))) player.closeInventory();
        }
        try {
            if (getConnection() != null && !getConnection().isClosed()) getConnection().close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        utility.message(log, "&7The Plugin has been disabled with success");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    private void setupBStats() {
        Metrics metrics = new Metrics(this);
        metrics.addCustomChart(new Metrics.MultiLineChart("players_and_servers", new Callable<Map<String, Integer>>() {
            @Override
            public Map<String, Integer> call() {
                Map<String, Integer> valueMap = new HashMap<>();
                valueMap.put("servers", 1);
                valueMap.put("players", Bukkit.getOnlinePlayers().size());
                return valueMap;
            }
        }));
    }

    private boolean setupNMS() {
        String version;
        try {
            version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
        } catch (ArrayIndexOutOfBoundsException whatVersionAreYouUsingException) {
            return false;
        }

        utility.message(log, "&7Your server is running version " + version);

        if (version.equals("v1_8_R1")) {
            nms = new v_1_8_R1();
        } else if (version.equals("v1_8_R2")) {
            nms = new v_1_8_R2();
        } else if (version.equals("v1_8_R3")) {
            nms = new v_1_8_R3();
        } else if (version.equals("v1_9_R1")) {
            nms = new v_1_9_R1();
        } else if (version.equals("v1_9_R2")) {
            nms = new v_1_9_R2();
        } else if (version.equals("v1_10_R1")) {
            nms = new v_1_10_R1();
        } else if (version.equals("v1_11_R1")) {
            nms = new v_1_11_R1();
        } else if (version.equals("v1_12_R1")) {
            nms = new v_1_12_R1();
        }
        return nms != null;
    }

    private boolean mysqlSetup() {
        String host = getConfig().getString("MySQL.Host");
        int port = getConfig().getInt("MySQL.Port");
        String database = getConfig().getString("MySQL.Database");
        String username = getConfig().getString("MySQL.Username");
        String password = getConfig().getString("MySQL.Password");

        try {
            synchronized (this) {
                if (getConnection() != null && !getConnection().isClosed()) {
                    return false;
                }
                if (getConfig().getString("Type").equalsIgnoreCase("sqlite")) {
                    Class.forName("org.sqlite.JDBC");
                    String url = "jdbc:sqlite:" + getDataFolder() + "/SQLite.db";
                    setConnection(DriverManager.getConnection(url));
                    utility.message(log, "&7Successfully connected to &cSQLite &7database.");
                    sqlManager.createTable();
                    return true;
                } else {
                    if (getConfig().getString("Type").equalsIgnoreCase("mysql")) {
                        Class.forName("com.mysql.jdbc.Driver");
                        setConnection(DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database + "?autoReconnect=true", username, password));
                        utility.message(log, "&7Successfully connected to &cMySQL &7database.");
                        sqlManager.createTable();
                        return true;
                    } else {
                        utility.message(log, "&4Could not connect to any database. Please use sqlite or mysql in your MySQL file.");
                        return false;
                    }
                }
            }
        } catch (SQLException e) {
            utility.message(log, "&cCould not connect to MySQL database, check yor credentials.");
            utility.message(log, e.getMessage());
            return false;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }
}
