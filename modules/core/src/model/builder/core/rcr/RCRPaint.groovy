package model.builder.core.rcr

import org.cytoscape.model.CyNetwork

public interface RCRPaint {

    public String paintColor(String direction, ScorePaintField paintByField, Object value)

    public String textColor(String direction, ScorePaintField paintByField, Object value)

    public void paintNetwork(ScorePaintField paintByField, CyNetwork cyN)
}