package model.builder.web.api.event

import org.cytoscape.event.CyListener

public interface AddedAccessInformationListener extends CyListener {

    void handleEvent(AddedAccessInformationEvent event)
}
