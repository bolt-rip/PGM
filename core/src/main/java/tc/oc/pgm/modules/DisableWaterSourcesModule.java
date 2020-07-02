package tc.oc.pgm.modules;

import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.util.xml.InvalidXMLException;

import java.util.logging.Logger;

public class DisableWaterSourcesModule implements MapModule {

  @Override
  public MatchModule createMatchModule(Match match) {
    return new DisableWaterSourcesMatchModule(match);
  }

  public static class Factory implements MapModuleFactory<DisableWaterSourcesModule> {
    @Override
    public DisableWaterSourcesModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      Element el = doc.getRootElement().getChild("disable-water-sources");
      if (el != null) {
        return new DisableWaterSourcesModule();
      }

      return null;
    }
  }
}
