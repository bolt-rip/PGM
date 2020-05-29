package tc.oc.util.bukkit.item;

import java.util.Arrays;
import javax.annotation.Nullable;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Dye;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Wool;

/**
 * A nice way to build {@link ItemStack}s
 *
 * <p>TODO: attributes, canPlaceOn, canDestroy, color, potion effects, etc.
 */
public class ItemBuilder {

  private final ItemStack stack = new ItemStack(Material.AIR);
  private @Nullable ItemMeta meta;

  public ItemStack get() {
    if (meta != null) {
      stack.setItemMeta(meta);
      meta = null;
    }
    return stack;
  }

  public ItemStack copy() {
    return get().clone();
  }

  protected ItemMeta meta() {
    if (meta == null) {
      meta = stack.getItemMeta();
    }
    return meta;
  }

  public ItemBuilder material(Material material) {
    stack.setType(material);
    return this;
  }

  public ItemBuilder material(MaterialData material) {
    stack.setType(material.getItemType());
    stack.setData(material);
    return this;
  }

  public ItemBuilder amount(int amount) {
    stack.setAmount(amount);
    return this;
  }

  public ItemBuilder durability(int durability) {
    stack.setDurability((short) durability);
    return this;
  }

  public ItemBuilder name(String name) {
    meta().setDisplayName(name);
    return this;
  }

  public ItemBuilder lore(String... lore) {
    meta().setLore(Arrays.asList(lore));
    return this;
  }

  public ItemBuilder flags(ItemFlag... flags) {
    meta().addItemFlags(flags);
    return this;
  }

  public ItemBuilder unbreakable(boolean unbreakable) {
    meta().spigot().setUnbreakable(unbreakable);
    return this;
  }

  public ItemBuilder enchant(Enchantment enchantment, int level) {
    meta().addEnchant(enchantment, level, true);
    return this;
  }

  public ItemBuilder color(DyeColor color) {
    final Material type = stack.getType();
    switch (type) {
      case INK_SACK:
        stack.setData(new Dye(color));
        break;

      case WOOL:
        stack.setData(new Wool(color));
        break;

      default:
        // Assume a colored block
        // TODO verify this, support other things e.g. banners
        stack.setData(new MaterialData(type, color.getWoolData()));
        break;
    }

    // I'm not clear why Bukkit doesn't do this, but it needs to be done.
    // Of course, if we ever support non-block items with this method,
    // this gets a bit more complicated.
    stack.setDurability(stack.getData().getData());

    return this;
  }
}
