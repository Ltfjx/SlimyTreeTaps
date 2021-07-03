package io.github.thebusybiscuit.slimytreetaps;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import io.github.thebusybiscuit.slimefun4.libraries.paperlib.PaperLib;
import io.github.thebusybiscuit.slimefun4.utils.ChatUtils;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.Category;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;

public class MagicalMirror extends SimpleSlimefunItem<ItemUseHandler> implements NotPlaceable {

    private final NamespacedKey mirrorLocation;

    public MagicalMirror(TreeTaps plugin, Category category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(category, item, recipeType, recipe);
        mirrorLocation = new NamespacedKey(plugin, "mirror_location");
    }

    @Override
    public ItemUseHandler getItemHandler() {
        return e -> {
            e.cancel();
            e.getPlayer().sendMessage(ChatColor.GREEN + "请给你的位置取个名字! 在聊天栏中输入.");
            ChatUtils.awaitInput(e.getPlayer(), name -> setLocation(e.getPlayer(), e.getItem(), name, e.getPlayer().getLocation()));
        };
    }

    public void teleport(Player p, ItemStack item) {
        if (!p.getInventory().containsAtLeast(new ItemStack(Material.ENDER_PEARL), 1)) {
            p.sendMessage(ChatColor.RED + "你必须至少有一颗末影珍珠来使用魔法镜子!");
            return;
        }

        Optional<Location> location = getLocation(item);

        if (location.isPresent()) {
            if (p.getInventory().removeItem(new ItemStack(Material.ENDER_PEARL)).isEmpty()) {
                PaperLib.teleportAsync(p, location.get()).thenAccept(hasTeleported -> {
                    if (hasTeleported.booleanValue()) {
                        p.sendTitle(item.getItemMeta().getDisplayName(), ChatColor.GRAY + "- 魔法镜子 -", 20, 60, 20);
                    } else {
                        p.getInventory().addItem(new ItemStack(Material.ENDER_PEARL));
                        p.sendMessage(ChatColor.RED + "传送已取消!");
                    }
                });
            } else {
                p.sendMessage(ChatColor.RED + "你至少要有一颗末影珍珠来进行传送!");
            }
        } else {
            p.sendMessage(ChatColor.RED + "此魔法镜子似乎没有有效的目的地!");
        }
    }

    private void setLocation(Player p, ItemStack item, String name, Location l) {
        ItemMeta meta = item.getItemMeta();

        JsonObject json = new JsonObject();
        json.addProperty("world", l.getWorld().getUID().toString());
        json.addProperty("x", l.getX());
        json.addProperty("y", l.getY());
        json.addProperty("z", l.getZ());
        json.addProperty("pitch", l.getPitch());
        json.addProperty("yaw", l.getYaw());

        meta.getPersistentDataContainer().set(mirrorLocation, PersistentDataType.STRING, json.toString());
        meta.setDisplayName(ChatColor.AQUA + ChatUtils.removeColorCodes(name));
        item.setItemMeta(meta);
        p.sendMessage(ChatColor.GREEN + "成功设置你的镜子位置!");
    }

    private Optional<Location> getLocation(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        String data = meta.getPersistentDataContainer().get(mirrorLocation, PersistentDataType.STRING);

        if (data != null) {
            JsonObject json = new JsonParser().parse(data).getAsJsonObject();
            UUID uuid = UUID.fromString(json.get("world").getAsString());
            World world = Bukkit.getWorld(uuid);

            if (world != null) {
                double x = json.get("x").getAsDouble();
                double y = json.get("y").getAsDouble();
                double z = json.get("z").getAsDouble();
                float pitch = json.get("pitch").getAsFloat();
                float yaw = json.get("yaw").getAsFloat();
                Location loc = new Location(world, x, y, z, yaw, pitch);

                return Optional.of(loc);
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

}
