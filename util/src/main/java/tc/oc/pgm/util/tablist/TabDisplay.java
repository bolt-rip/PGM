package tc.oc.pgm.util.tablist;

import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.SpigotTextAdapter;
import net.kyori.text.format.TextColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.bukkit.OnlinePlayerMapAdapter;
import tc.oc.pgm.util.nms.NMSHacks;

/**
 * Render arbitrary strings to the TAB list AKA player list. Before this is used with a player,
 * their list should be initialized with {@link #addViewer}. This will disable default updates for
 * the list, and it is assumed that nothing else will make changes to the player's list while
 * TabDisplay is working with it.
 *
 * <p>This class works by filling the list with unique invisible player names consisting of a single
 * formatting code. Each of those players is added to a unique team, and the team prefix and suffix
 * are used to show text in the player's slot in the list.
 */
public class TabDisplay {
  // Used as the ping value for all slots
  private static final int PING = 9999;

  // Number of rows in the player list, which is always 20 AFAIK
  public static final int HEIGHT = 20;

  // Maximum number of columns that the player list will show
  public static final int MAX_WIDTH = 10;

  // Width and total size of the list
  private final int width, slots;

  // A no-space character for 1.7 clients
  private final String NO_SPACE = "\u1FFF";

  // Cached packets used to setup and tear down the player list
  private final Packet[] teamCreatePackets;
  private final Packet[] teamRemovePackets;
  private final UUID[] uuids;
  private final PacketPlayOutPlayerInfo listAddPacket;
  private final PacketPlayOutPlayerInfo listRemovePacket;

  // Current contents of each viewer's list
  private final OnlinePlayerMapAdapter<String[]> viewers;

  /**
   * @param plugin The display will immediately register to receive events on behalf of this plugin
   */
  public TabDisplay(Plugin plugin, int width) {
    // Number of columns is maxPlayers/rows rounded up
    this.width = width;
    this.slots = this.width * HEIGHT;

    this.teamCreatePackets = new Packet[this.slots];
    this.teamRemovePackets = new Packet[this.slots];
    this.uuids = new UUID[this.slots];

    this.listAddPacket = new PacketPlayOutPlayerInfo();
    this.listRemovePacket = new PacketPlayOutPlayerInfo();
    listAddPacket.a = PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER;
    listRemovePacket.a = PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER;

    for (int slot = 0; slot < this.slots; ++slot) {
      BaseComponent[] playerName = this.slotName(slot);
      String name = playerName[0].toLegacyText();
      String teamName = this.slotTeamName(slot);
      this.teamCreatePackets[slot] =
          NMSHacks.teamCreatePacket(
              teamName, teamName, "", "", false, false, Collections.singleton(name));
      this.teamRemovePackets[slot] = NMSHacks.teamRemovePacket(teamName);
      uuids[slot] = UUID.randomUUID();

      listAddPacket.b.add(
          NMSHacks.playerListPacketData(
              listAddPacket,
              uuids[slot],
              playerName[0].toLegacyText(),
              GameMode.SURVIVAL,
              PING,
              null,
              playerName));
      listRemovePacket.b.add(
          NMSHacks.playerListPacketData(listRemovePacket, uuids[slot], playerName));
    }

    this.viewers = new OnlinePlayerMapAdapter<>(new HashMap<Player, String[]>(), plugin);
  }

  /** Register event listeners and allow the display to be used */
  public void enable() {
    this.viewers.enable();
  }

  /** Unregister event listeners and permanently disable this display */
  public void disable() {
    this.clearViewers();
    this.viewers.disable();
  }

  public int getWidth() {
    return width;
  }

  private int slotIndex(int x, int y) {
    return y * this.width + x;
  }

  private static final int MAX_COLOR = TextColor.values().length;

  private BaseComponent[] slotName(int slot) {
    // This needs to avoid collision with the sidebar, which uses chars 0-15. Eventually we will add
    // a scoreboard API to Commons and this class can cooperate with it in a less hacky way.
    TextComponent.Builder builder = TextComponent.builder();
    builder.append(NO_SPACE, TextColor.BLACK); // Avoid collision by adding a 0 on front

    do {
      builder.append(NO_SPACE, TextColor.values()[slot % MAX_COLOR]);
      slot /= MAX_COLOR;
    } while (slot > 0);
    return SpigotTextAdapter.toBungeeCord(builder.build());
  }

  private String slotTeamName(int slot) {
    return "TabDisplay" + slot;
  }

  private void set(Player viewer, int slot, String text) {
    String[] names = this.viewers.get(viewer);
    if (names != null && !Objects.equals(text, names[slot])) {
      names[slot] = text;

      String[] split = StringUtils.splitIntoTeamPrefixAndSuffix(text);
      String name = this.slotTeamName(slot);
      NMSHacks.sendPacket(
          viewer, NMSHacks.teamUpdatePacket(name, name, split[0], split[1], false, false));
    }
  }

  /** Set the text for the slot at the given position for the given viewer */
  public void set(Player viewer, int x, int y, String text) {
    this.set(viewer, this.slotIndex(x, y), text);
  }

  public void addViewer(Player viewer) {
    if (viewer.isOnline() && !this.viewers.containsKey(viewer)) {

      for (int slot = 0; slot < this.slots; ++slot) {
        NMSHacks.sendPacket(viewer, this.teamCreatePackets[slot]);
      }
      NMSHacks.sendPacket(viewer, this.listAddPacket);

      String[] names = new String[this.slots];
      Arrays.fill(names, "");
      this.viewers.put(viewer, names);
    }
  }

  public void removeViewer(Player viewer) {
    if (this.viewers.containsKey(viewer)) {
      this.viewers.remove(viewer);

      NMSHacks.sendPacket(viewer, this.listRemovePacket);
      for (int slot = 0; slot < this.slots; ++slot) {
        NMSHacks.sendPacket(viewer, this.teamRemovePackets[slot]);
      }
    }
  }

  public void clearViewers() {
    for (Player viewer : this.viewers.keySet()) {
      NMSHacks.sendPacket(viewer, this.listRemovePacket);
      for (int slot = 0; slot < this.slots; ++slot) {
        NMSHacks.sendPacket(viewer, this.teamRemovePackets[slot]);
      }
    }
    this.viewers.clear();
  }
}
