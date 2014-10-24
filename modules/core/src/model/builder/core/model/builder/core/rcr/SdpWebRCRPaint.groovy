package model.builder.core.model.builder.core.rcr

import org.cytoscape.model.CyNetwork
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.vizmap.VisualStyle
import org.cytoscape.view.vizmap.mappings.DiscreteMapping

import java.awt.Color

import static model.builder.core.Activator.CY
import static model.builder.core.model.builder.core.rcr.MechanismPaintField.createConcordanceFunction
import static model.builder.core.model.builder.core.rcr.MechanismPaintField.createDirectionFunction
import static model.builder.core.model.builder.core.rcr.MechanismPaintField.createRichnessFunction
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*

class SdpWebRCRPaint implements RCRPaint {

    @Override
    void paintMechanisms(MechanismPaintField paintByField, CyNetwork network) {
        Collection<CyNetworkView> views = CY.cyNetworkViewManager.getNetworkViews(network)
        views.each {
            VisualStyle style = CY.visualMappingManager.getVisualStyle(it)
            def lock = style.allVisualPropertyDependencies.find {
                it.idString == 'nodeSizeLocked'
            }
            if (lock) {
                lock.setDependency(false)
            }

            DiscreteMapping selected = CY.discreteMapping.createVisualMappingFunction(
                    'selected', Boolean.class, NODE_SELECTED_PAINT) as DiscreteMapping
            selected.putMapValue(Boolean.TRUE, new Color(0x00CCCC))
            style.addVisualMappingFunction(selected)

            if (paintByField == MechanismPaintField.DIRECTION) {
                style.addVisualMappingFunction(createDirectionFunction())
            } else if (paintByField == MechanismPaintField.CONCORDANCE) {
                style.addVisualMappingFunction(createConcordanceFunction())
            } else if (paintByField == MechanismPaintField.RICHNESS) {
                style.addVisualMappingFunction(createRichnessFunction())
            }

            CY.cyEventHelper.flushPayloadEvents()
            style.apply(it)
            it.updateView()
        }
    }
}
