package model.builder.core.event

import groovy.transform.TupleConstructor
import org.cytoscape.session.events.SessionLoadedEvent
import org.cytoscape.session.events.SessionLoadedListener

import static model.builder.core.Activator.CY
import static org.openbel.kamnav.core.Util.contributeVisualStyles

@TupleConstructor
class SessionLoadListener implements SessionLoadedListener {

    @Override
    void handleEvent(SessionLoadedEvent ev) {
        contributeVisualStyles(CY.visualMappingManager, CY.loadVizmapFileTaskFactory)
    }
}
