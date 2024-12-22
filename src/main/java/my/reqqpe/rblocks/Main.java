package my.reqqpe.rblocks;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import me.clip.placeholderapi.PlaceholderAPI;
import my.reqqpe.rblocks.Command.rblock;
import my.reqqpe.rblocks.Event.BlockBreak;
import my.reqqpe.rblocks.Event.BlockInteract;
import my.reqqpe.rblocks.Event.SwapItem;
import my.reqqpe.rblocks.Util.BackPack;
import my.reqqpe.rblocks.Util.DataBaseManager;
import my.reqqpe.rblocks.Util.PlaceHolderApiExpansion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public final class Main extends JavaPlugin {
    private DataBaseManager dataBaseManager;
    private BackPack backPack;

    @Override
    public void onEnable() {
        checkPlugin("PlaceholderAPI");
        checkPlugin("WorldGuard");
        backPack = new BackPack();
        dataBaseManager = new DataBaseManager();
        new PlaceHolderApiExpansion(this, backPack, dataBaseManager).register();

        try {
            dataBaseManager.connect();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new BlockBreak(this, backPack, dataBaseManager), this);
        getServer().getPluginManager().registerEvents(new BlockInteract(this), this);
        getServer().getPluginManager().registerEvents(new SwapItem(this), this);
        getCommand("rblock").setExecutor(new rblock(this, dataBaseManager, backPack));

    }

    @Override
    public void onDisable() {
        try {
            dataBaseManager.disconnect();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }



    private void checkPlugin(String pluginName) {
        if (Bukkit.getPluginManager().getPlugin(pluginName) != null) {
            getLogger().info(" ");
            getLogger().info("Плагин " + pluginName + " был найден");
            getLogger().info(" ");
        } else {
            getLogger().severe(" ");
            getLogger().severe("Плагин " + pluginName + " не найден");
            getLogger().severe(" ");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    public boolean isTool(ItemStack itemStack, Material material, String name) {
        if (itemStack == null || itemStack.getType() != material) {
            return false;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }

        if (!ChatColor.stripColor(meta.getDisplayName()).equals(name)) {
            return false;
        }
        return true;

    }

    public void removeEffect(Player player, String tool) {
        ConfigurationSection effectsSection = getConfig().getConfigurationSection("tools." + tool + ".effects");
        if (effectsSection !=null) {
            for (String effectName : effectsSection.getKeys(false)) {
                PotionEffectType effectType = PotionEffectType.getByName(effectName.toUpperCase());
                assert effectType != null;
                if (player.hasPotionEffect(effectType)) {
                    player.removePotionEffect(effectType);
                }
            }
        }
    }
    public void addEffect(Player player, String tool) {
        ConfigurationSection effectsSection = getConfig().getConfigurationSection("tools." + tool + ".effects");
        if (effectsSection != null) {
            for (String effectName : effectsSection.getKeys(false)) {
                int level = effectsSection.getInt(effectName);
                PotionEffectType effectType = PotionEffectType.getByName(effectName.toUpperCase());
                if (effectType != null) {
                    PotionEffect effect = new PotionEffect(effectType, Integer.MAX_VALUE, level-1, false, false);
                    player.addPotionEffect(effect);
                }
            }
        }
    }

    public boolean isBlockTool(String tool, Material block) {
        Map<String, Set<Material>> pickaxeBlocks = new HashMap<>();
        List<String> blockList = getConfig().getStringList("tools." + tool + ".blocks");

        Set<Material> blockSet = blockList.stream()
                .map(Material::matchMaterial)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        pickaxeBlocks.put(tool, blockSet);

        Set<Material> blocks = pickaxeBlocks.get(tool);
        return blocks != null && blocks.contains(block);
    }
    public boolean isBlockInRegion(Block block, String regionName) {

        // Получаем региональный контейнер
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        // Получаем менеджер регионов для мира, в котором находится блок
        RegionManager regionManager = container.get(BukkitAdapter.adapt(block.getWorld()));

        if (regionManager == null) {
            getLogger().warning("Нет менеджера регионов этого мира");
            return false; // Нет менеджера регионов для данного мира
        }

        // Получаем регион с именем regionName
        ProtectedRegion region = regionManager.getRegion(regionName);

        if (region == null) {
            getLogger().warning("Регион не найден");
            return false; // Регион с таким именем не найден
        }

        // Проверяем, находится ли блок в указанном регионе
        return region.contains(block.getX(), block.getY(), block.getZ());
    }
}
