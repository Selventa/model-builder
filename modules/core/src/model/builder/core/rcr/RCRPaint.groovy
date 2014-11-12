package model.builder.core.rcr

import org.cytoscape.model.CyNetwork

public interface RCRPaint {

    public String paintColor(String direction, MechanismPaintField paintByField, Object value)

    public String textColor(String direction, MechanismPaintField paintByField, Object value)

    public void paintMechanisms(MechanismPaintField paintByField, Collection<CyNetwork> networks)
}