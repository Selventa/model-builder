package model.builder.web.api.event

import org.cytoscape.event.CyListener

public interface SetDefaultAccessInformationListener extends CyListener {

    void handleEvent(SetDefaultAccessInformationEvent event)
}
