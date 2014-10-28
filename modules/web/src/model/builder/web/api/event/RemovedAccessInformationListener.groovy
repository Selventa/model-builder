package model.builder.web.api.event

import org.cytoscape.event.CyListener

public interface RemovedAccessInformationListener extends CyListener {

    void handleEvent(RemovedAccessInformationEvent event)
}
