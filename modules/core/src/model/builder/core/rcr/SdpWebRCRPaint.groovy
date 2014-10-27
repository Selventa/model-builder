package model.builder.core.rcr

import org.cytoscape.model.CyNetwork
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.vizmap.VisualStyle
import org.cytoscape.view.vizmap.mappings.DiscreteMapping

import java.awt.Color

import static model.builder.core.rcr.Constant.SDP_RCR_FILL_COLOR_COLUMN
import static model.builder.core.Activator.CY
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*

class SdpWebRCRPaint implements RCRPaint {

    @Override
    String paintColor(String dir, MechanismPaintField paintByField, Object value) {
        if (!paintByField) throw new NullPointerException('paintByField is null')

        if (paintByField == MechanismPaintField.DIRECTION) {
            // duck type on toString
            String str = value ? value.toString() : null;
            switch(str) {
                case 'Down':
                    return '#000666'
                case 'Up':
                    return '#FFA000'
                case 'None':
                    return '#FFFFFF'
                default:
                    return '#AAAAAA'
            }
        } else {
            // null when value is not a double
            if (!(value instanceof Double)) {
                return null;
            }

            switch(dir) {
                case 'Down':
                    def down = [(0.0..0.001):  '#000666', (0.001..0.005): '#333399',
                                (0.005..0.01): '#6666CC', (0.01..0.05):   '#9999FF',
                                (0.05..0.1):   '#CCCCFF', (0.1..1):       '#FFFFFF']
                    return down.find {
                        it.key.containsWithinBounds(value)
                    }?.value
                case 'Up':
                    def up = [(0.0..0.001):  '#FFA000', (0.001..0.005): '#FFC800',
                              (0.005..0.01): '#FFE800', (0.01..0.05):   '#FFF800',
                              (0.05..0.1):   '#FFFF99', (0.1..1):       '#FFFFFF']
                    return up.find {
                        it.key.containsWithinBounds(value)
                    }?.value
                case 'None':
                    return '#FFFFFF'
                default:
                    return '#AAAAAA'
            }
        }
    }

    /**
     * FIXME This should apply visualization to multiple CyNetwork!
     */
    @Override
    void paintMechanisms(MechanismPaintField paintByField, Collection<CyNetwork> networks) {
        def kamStyle = CY.visualMappingManager.allVisualStyles.find {
            it.title == 'KAM Visualization'
        }

        VisualStyle rcrStyle = CY.visualMappingManager.allVisualStyles.find {
            it.title == 'RCR'
        }
        if (!rcrStyle) {
            rcrStyle = CY.visualStyleFactory.createVisualStyle(kamStyle)
            rcrStyle.title = 'RCR'
            def lock = rcrStyle.allVisualPropertyDependencies.find {
                it.idString == 'nodeSizeLocked'
            }
            if (lock) lock.setDependency(false)

            // change node selection color (teal); yellow conflicts with Up color
            DiscreteMapping selected = CY.discreteMapping.createVisualMappingFunction(
                    'selected', Boolean.class, NODE_SELECTED_PAINT) as DiscreteMapping
            selected.putMapValue(Boolean.TRUE, new Color(0x00CCCC))
            rcrStyle.addVisualMappingFunction(selected)

            rcrStyle.addVisualMappingFunction(
                    CY.passthroughMapping.createVisualMappingFunction(
                            SDP_RCR_FILL_COLOR_COLUMN, String.class, NODE_FILL_COLOR
                    )
            )

            CY.visualMappingManager.addVisualStyle(rcrStyle);
            CY.cyEventHelper.flushPayloadEvents()
        }

        networks.each {
            CyNetwork cyN ->
                Collection<CyNetworkView> views = CY.cyNetworkViewManager.getNetworkViews(cyN)
                views.each {
                    CY.visualMappingManager.setVisualStyle(rcrStyle, it)
                    rcrStyle.apply(it)
                    it.updateView()
                }
        }
    }
}
