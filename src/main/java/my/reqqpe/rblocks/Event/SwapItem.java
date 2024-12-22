package my.reqqpe.rblocks.Event;

import my.reqqpe.rblocks.Main;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

public class SwapItem implements Listener {
    private final Main plugin;

    public SwapItem(Main plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent e) {
        Player player = e.getPlayer();
        ItemStack itemStack = player.getInventory().getItemInMainHand();

        if (player.hasPermission(plugin.getConfig().getString("permission.bypass", "rblock.bypass"))) return;
        for (String tool :  plugin.getConfig().getConfigurationSection("tools").getKeys(false)) {

            Material toolMaterial = Material.valueOf(plugin.getConfig().getString("tools." + tool + ".material"));
            String toolName = plugin.getConfig().getString("tools." + tool + ".name");

            if (plugin.isTool(itemStack, toolMaterial, toolName)) {
                plugin.removeEffect(player, tool);
            }
        }
    }
}
