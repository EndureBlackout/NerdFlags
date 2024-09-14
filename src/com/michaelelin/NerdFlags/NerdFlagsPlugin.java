package com.michaelelin.NerdFlags;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.EnumFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;

public class NerdFlagsPlugin extends JavaPlugin {

    public WorldGuardPlugin worldguard;

    public Material _navigationWand;

    private Player nextTP;
    private long timestamp;
    
    public HashMap<String, ItemStack[]> PLAYER_INVENTORY = new HashMap<String, ItemStack[]>();

    ProtocolManager protocolManager;

    public StateFlag ALLOW_DROPS;
    public StateFlag ALLOW_MOB_DROPS;

    public StateFlag PLAYER_MOB_DAMAGE;

    public StateFlag NETHER_PORTAL;
    public StateFlag END_PORTAL;

    public StateFlag SNOWBALL_FIREFIGHT;

    public StateFlag COMPASS;
    public StateFlag TELEPORT_ENTRY;

    public EnumFlag<GameMode> FORCE_GAMEMODE;

    public StateFlag WEATHER;

    public StateFlag NERD_KEEP_INVENTORY;
    
    public StateFlag REGION_SEPARATE_INVENTORY;

    public StringFlag ENTRY_COMMANDS;

    public StateFlag USE_DISPENSER;
    public StateFlag USE_NOTE_BLOCK;
    public StateFlag USE_WORKBENCH;
    public StateFlag USE_DOOR;
    public StateFlag USE_LEVER;
    public StateFlag USE_PRESSURE_PLATE;
    public StateFlag USE_BUTTON;
    public StateFlag USE_JUKEBOX;
    public StateFlag USE_REPEATER;
    public StateFlag USE_TRAP_DOOR;
    public StateFlag USE_FENCE_GATE;
    public StateFlag USE_BREWING_STAND;
    public StateFlag USE_CAULDRON;
    public StateFlag USE_ENCHANTMENT_TABLE;
    public StateFlag USE_ENDER_CHEST;
    public StateFlag USE_TRIPWIRE;
    public StateFlag USE_BEACON;
    public StateFlag USE_COMPARATOR;
    public StateFlag USE_HOPPER;
    public StateFlag USE_DROPPER;
    public StateFlag USE_DAYLIGHT_DETECTOR;
    public StateFlag TAKE_LECTERN_BOOK;
    public StateFlag USE_BOOK_SHELF;
    public StateFlag STOP_PATH_CHANGE;
    public StateFlag DENY_EGG_PLACE;
    public StateFlag DISABLE_COLLISION;
    
    public String CONNECTION_STRING;
    public String MYSQL_USER;
    public String MYSQL_PASS;

    @Override
    public void onEnable() {

        if (checkPlugin("ProtocolLib", false)) {
            protocolManager = ProtocolLibrary.getProtocolManager();
        }
        
        if(WorldGuard.getInstance().getPlatform().getSessionManager().registerHandler(Entry.FACTORY, null)) {
        	getServer().getPluginManager().registerEvents(new NerdFlagsRegionListener(this), this);
        }
        
        CONNECTION_STRING = getConfig().getString("mysql_connectionstring");
        MYSQL_USER = getConfig().getString("mysql_username");
        MYSQL_PASS = getConfig().getString("mysql_password");
        
        try {
        	Connection conn = DriverManager.getConnection(CONNECTION_STRING, MYSQL_USER, MYSQL_PASS);
        	
        	String query = "SELECT * FROM flag_inventory;";
        	
        	Statement stmt = conn.createStatement();
        	ResultSet results = stmt.executeQuery(query);
        	
        	while(results.next()) {
        		String key = results.getString(1);
        		ItemStack[] value = fromBase64(results.getString(2));
        		
        		PLAYER_INVENTORY.put(key, value);
        		
        		String deleteQuery = "DELETE FROM flag_inventory WHERE regionuuid=?";
        		
        		PreparedStatement pStmt = conn.prepareStatement(deleteQuery);
        		
        		pStmt.setString(1, key);
        		
        		pStmt.execute();
        	}
        } catch (SQLException e) {
        	Bukkit.getLogger().severe("Unable to retrieve inventories from database.");
        	Bukkit.getLogger().info(e.getMessage());
        } catch (IOException e) {
			Bukkit.getLogger().severe("Unable to decrypt from Base64.");
			Bukkit.getLogger().info(e.getMessage());
		}

        getServer().getPluginManager().registerEvents(new NerdFlagsListener(this), this);

        // pull WorldEdit navigation wand information now
        WorldEdit worldEdit = WorldEdit.getInstance();
        String navigationWandMaterialName = worldEdit.getConfiguration().navigationWand;
        _navigationWand = Material.getMaterial(navigationWandMaterialName);

        Plugin wgPlugin = getServer().getPluginManager().getPlugin("WorldGuard");
        if (wgPlugin instanceof WorldGuardPlugin) {
            worldguard = (WorldGuardPlugin) wgPlugin;
        }

    }
    
    @Override
    public void onDisable() {
    	try {
			Connection conn = DriverManager.getConnection(CONNECTION_STRING, MYSQL_USER, MYSQL_PASS);
			
	    	for(Map.Entry<String, ItemStack[]> inv : PLAYER_INVENTORY.entrySet()) {
	    		String encodedInv = toBase64(inv.getValue());
	    		
	    		String query = "INSERT INTO flag_inventory (regionuuid, inventoryString) VALUES (? , ?);";
	    		
	    		PreparedStatement pStmt = conn.prepareStatement(query);
	    		
	    		pStmt.setString(1, inv.getKey());
	    		pStmt.setString(2, encodedInv);
	    		
	    		pStmt.execute();
	    		
	    		pStmt.close();
	    	}
	    	
	    	conn.close();
		} catch (SQLException e) {
			Bukkit.getLogger().log(Level.SEVERE, "There was an error writing the inventory to database");
			Bukkit.getLogger().info(e.getMessage());
		};
    }
    
    public ItemStack[] fromBase64(String data) throws IOException {
    	try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack[] items = new ItemStack[dataInput.readInt()];
    
            // Read the serialized inventory
            for (int i = 0; i < items.length; i++) {
            	items[i] = (ItemStack) dataInput.readObject();
            }
            
            dataInput.close();
            return items;
        } catch (ClassNotFoundException e) {
            throw new IOException("Unable to decode class type.", e);
        }
    }
    
    public String toBase64(ItemStack[] inventory) throws IllegalStateException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            
            // Write the size of the inventory
            dataOutput.writeInt(inventory.length);
            
            // Save every element in the list
            for (int i = 0; i < inventory.length; i++) {
                dataOutput.writeObject(inventory[i]);
            }
            
            // Serialize that array
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    private <T extends Plugin> boolean checkPlugin(String name, boolean required) {
        Plugin plugin = getServer().getPluginManager().getPlugin(name);
        if (plugin == null) {
            if (required) {
                getLogger().warning("[" + getName() + "] " + name + " is required for this plugin to work; disabling.");
                getServer().getPluginManager().disablePlugin(this);
            }
            return false;
        }
        return true;
    }

    public void expectTeleport(Player player) {
        this.nextTP = player;
        this.timestamp = player.getPlayerTime();
    }

    public boolean hasCompassed(Player player) {
        return this.nextTP == player && this.timestamp == player.getPlayerTime();
    }

    /**
     * Register Flags.
     */
    @Override
    public void onLoad() {
    	
    	saveDefaultConfig();

        WorldGuard worldGuard = WorldGuard.getInstance();
        FlagRegistry flagRegistry = worldGuard.getFlagRegistry();

        flagRegistry.register(ALLOW_DROPS = new StateFlag("allow-drops", true));
        flagRegistry.register(ALLOW_MOB_DROPS = new StateFlag("allow-mob-drops", true));
        flagRegistry.register(PLAYER_MOB_DAMAGE = new StateFlag("player-mob-damage", true));
        flagRegistry.register(NETHER_PORTAL = new StateFlag("nether-portal", true));
        flagRegistry.register(END_PORTAL = new StateFlag("end-portal", true));
        flagRegistry.register(SNOWBALL_FIREFIGHT = new StateFlag("snowball-firefight", false));
        flagRegistry.register(COMPASS = new StateFlag("compass", true));
        flagRegistry.register(TELEPORT_ENTRY = new StateFlag("teleport-entry", true));
        flagRegistry.register(FORCE_GAMEMODE = new EnumFlag<>("force-gamemode", GameMode.class));
        flagRegistry.register(WEATHER = new StateFlag("weather", false));
        flagRegistry.register(NERD_KEEP_INVENTORY = new StateFlag("nerd-keep-inventory", false));
        flagRegistry.register(new StringFlag("date"));
        flagRegistry.register(new StringFlag("created-by"));
        flagRegistry.register(new StringFlag("first-owner"));
        flagRegistry.register(ENTRY_COMMANDS = new StringFlag("entry-commands"));
        flagRegistry.register(REGION_SEPARATE_INVENTORY = new StateFlag("separate-region-inventory", false));
        flagRegistry.register(DENY_EGG_PLACE = new StateFlag("deny-egg-place", false));
        flagRegistry.register(DISABLE_COLLISION = new StateFlag("disable-collision", false));

        flagRegistry.register(STOP_PATH_CHANGE = new StateFlag("stop-path-change", getConfig().getBoolean("default-stop-path-change")));
        flagRegistry.register(TAKE_LECTERN_BOOK = new StateFlag("take-lectern-book", getConfig().getBoolean("default-lectern")));
        flagRegistry.register(USE_BOOK_SHELF = new StateFlag("use-book-shelf", getConfig().getBoolean("default-book-shelf")));;
        flagRegistry.register(USE_DISPENSER = new StateFlag("use-dispenser", getConfig().getBoolean("default-dispenser")));
        flagRegistry.register(USE_NOTE_BLOCK = new StateFlag("use-note-block", getConfig().getBoolean("default-note-block")));
        flagRegistry.register(USE_WORKBENCH = new StateFlag("use-workbench", getConfig().getBoolean("default-workbench")));
        flagRegistry.register(USE_DOOR = new StateFlag("use-door", getConfig().getBoolean("default-door")));
        flagRegistry.register(USE_LEVER = new StateFlag("use-lever", getConfig().getBoolean("default-lever")));
        flagRegistry.register(USE_PRESSURE_PLATE = new StateFlag("use-pressure-plate", getConfig().getBoolean("default-pressure-plate")));
        flagRegistry.register(USE_BUTTON = new StateFlag("use-button", getConfig().getBoolean("default-button")));
        flagRegistry.register(USE_JUKEBOX = new StateFlag("use-jukebox", getConfig().getBoolean("default-jukebox")));
        flagRegistry.register(USE_REPEATER = new StateFlag("use-repeater", getConfig().getBoolean("default-repeater")));
        flagRegistry.register(USE_TRAP_DOOR = new StateFlag("use-trap-door", getConfig().getBoolean("default-trap-door")));
        flagRegistry.register(USE_FENCE_GATE = new StateFlag("use-fence-gate", getConfig().getBoolean("default-fence-gate")));
        flagRegistry.register(USE_BREWING_STAND = new StateFlag("use-brewing-stand", getConfig().getBoolean("default-brewing-stand")));
        flagRegistry.register(USE_CAULDRON = new StateFlag("use-cauldron", getConfig().getBoolean("default-cauldron")));
        flagRegistry.register(USE_ENCHANTMENT_TABLE = new StateFlag("use-enchantment-table", getConfig().getBoolean("default-enchantment-table")));
        flagRegistry.register(USE_ENDER_CHEST = new StateFlag("use-ender-chest", getConfig().getBoolean("default-ender-chest")));
        flagRegistry.register(USE_TRIPWIRE = new StateFlag("use-tripwire", getConfig().getBoolean("default-tripwire")));
        flagRegistry.register(USE_BEACON = new StateFlag("use-beacon", getConfig().getBoolean("default-beacon")));
        flagRegistry.register(USE_COMPARATOR = new StateFlag("use-comparator", getConfig().getBoolean("default-comparator")));
        flagRegistry.register(USE_HOPPER = new StateFlag("use-hopper", getConfig().getBoolean("default-hopper")));
        flagRegistry.register(USE_DROPPER = new StateFlag("use-dropper", getConfig().getBoolean("default-dropper")));
        flagRegistry.register(USE_DAYLIGHT_DETECTOR = new StateFlag("use-daylight-detector", getConfig().getBoolean("default-daylight-detector")));

        getLogger().log(Level.INFO, "Loaded all flags");
    }

}
