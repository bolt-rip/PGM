package tc.oc.pgm.modules;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.WaterSourceFormEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.events.ListenerScope;

@ListenerScope(MatchScope.RUNNING)
public class DisableWaterSourcesMatchModule implements MatchModule, Listener {

  public DisableWaterSourcesMatchModule(Match match) {}

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onWaterSourceForm(final WaterSourceFormEvent event) {
    event.setCancelled(true);
  }
}
