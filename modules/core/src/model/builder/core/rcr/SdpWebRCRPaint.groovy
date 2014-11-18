package model.builder.core.rcr

import org.cytoscape.model.CyNetwork
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.vizmap.VisualStyle
import org.cytoscape.view.vizmap.mappings.DiscreteMapping
import org.cytoscape.view.vizmap.mappings.PassthroughMapping

import java.awt.*

import static model.builder.core.Activator.CY
import static model.builder.core.rcr.Constant.SDP_RCR_FILL_COLOR_COLUMN
import static model.builder.core.rcr.Constant.SDP_RCR_SIGNIFICANT_COLUMN
import static model.builder.core.rcr.Constant.SDP_RCR_TEXT_COLOR_COLUMN
import static ScorePaintField.*
import static model.builder.core.rcr.Constant.SDP_RCR_TOOLTIP_COLUMN
import static org.cytoscape.model.CyNetwork.NAME;
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.NODE_BORDER_PAINT
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.NODE_BORDER_WIDTH
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.NODE_FILL_COLOR
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.NODE_LABEL_COLOR
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.NODE_SELECTED_PAINT
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.NODE_TOOLTIP

class SdpWebRCRPaint implements RCRPaint {

    @Override
    String paintColor(String dir, ScorePaintField paintByField, Object value) {
        if (!paintByField) throw new NullPointerException('paintByField is null')
        if (paintByField == DIRECTION) {
            // duck type on toString
            String str = value ? value.toString() : null
            switch (str) {
                case 'Down':
                    return '#6666CC'
                case 'Up':
                    return '#FFE800'
            }
        } else if (paintByField == CONCORDANCE || paintByField == RICHNESS) {
            def down = [(0.0..0.001):  '#FF00FF', (0.001..0.005): '#FF2FFF',
                        (0.005..0.01): '#FF5FFF', (0.01..0.05):   '#FF8FFF',
                        (0.05..0.1):   '#FFBFFF', (0.1..1):       '#FFFFFF']
            return down.find {
                it.key.containsWithinBounds(value)
            }?.value
        } else {
            // null when value is not a double
            if (!(value instanceof Double)) return null
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
            }
        }
    }

    @Override
    void paintNetwork(ScorePaintField paintByField, CyNetwork cyN) {
        // 1. Find views for network. If none then return.
        // 2. Iterate views
        //    (a) retrieve visual style
        //    (b) create derivative style, named "RCR style for [NETWORK NAME]"
        //    (c) customize derivative style
        //    (d) apply to view

        def views = CY.cyNetworkViewManager.getNetworkViews(cyN)
        if (views.empty) return;

        views.each {
            CyNetworkView view ->
                def rcrStyle = getStyle(view)
                CY.visualMappingManager.setVisualStyle(rcrStyle, view)
        }

        views.each {
            CyNetworkView view ->
                def style = CY.visualMappingManager.getVisualStyle(view)

                try {
                    applyStyle(style, view)
                } catch (ConcurrentModificationException e) {
                    // cytoscape stacktrace
                }
        }
        CY.cyEventHelper.flushPayloadEvents()
    }

    protected static VisualStyle getStyle(CyNetworkView view) {
        CyNetwork cyN = view.model
        String title = cyN.getRow(cyN).get(NAME, String.class)
        String rcrStyleName = "RCR style - $title"

        def viewStyle = CY.visualMappingManager.getVisualStyle(view)
        def rcrStyle = CY.visualMappingManager.allVisualStyles.find {
            it.title == rcrStyleName
        }

        if (rcrStyle) {
            // View style changed, reapply RCR style
            if (viewStyle.title != rcrStyle.title) {
                CY.visualMappingManager.removeVisualStyle(rcrStyle)
                rcrStyle = deriveStyle(viewStyle, rcrStyleName)
            }
        } else {
            // create new RCR style
            rcrStyle = deriveStyle(viewStyle, rcrStyleName)
        }
        CY.cyEventHelper.flushPayloadEvents()
        rcrStyle
    }

    protected static VisualStyle deriveStyle(VisualStyle style, String newStyleTitle) {
        def derivedStyle = CY.visualStyleFactory.createVisualStyle(style)
        derivedStyle.title = newStyleTitle
        addVisualFunctions(derivedStyle)
        CY.visualMappingManager.addVisualStyle(derivedStyle)
        derivedStyle
    }

    protected static void addVisualFunctions(VisualStyle rcrStyle) {
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
        rcrStyle.addVisualMappingFunction(
                CY.passthroughMapping.createVisualMappingFunction(
                        SDP_RCR_TEXT_COLOR_COLUMN, String.class, NODE_LABEL_COLOR
                )
        )
        DiscreteMapping significanceBorder = CY.discreteMapping.createVisualMappingFunction(
                SDP_RCR_SIGNIFICANT_COLUMN, Boolean.class, NODE_BORDER_PAINT) as DiscreteMapping
        significanceBorder.putMapValue(Boolean.TRUE,  Color.black)
        significanceBorder.putMapValue(Boolean.FALSE, Color.red)
        rcrStyle.addVisualMappingFunction(significanceBorder)
        DiscreteMapping significanceWidth = CY.discreteMapping.createVisualMappingFunction(
                SDP_RCR_SIGNIFICANT_COLUMN, Boolean.class, NODE_BORDER_WIDTH) as DiscreteMapping
        significanceWidth.putMapValue(Boolean.FALSE, 6.0)
        rcrStyle.addVisualMappingFunction(significanceWidth)

        PassthroughMapping tooltip = CY.passthroughMapping.createVisualMappingFunction(
                SDP_RCR_TOOLTIP_COLUMN, String.class, NODE_TOOLTIP) as PassthroughMapping
        rcrStyle.addVisualMappingFunction(tooltip)

        rcrStyle
    }

    protected static void applyStyle(VisualStyle style, CyNetworkView view) {
        style.apply(view)
        view.updateView()
        Thread.start {
            sleep(500)
            view.updateView()
        }
    }
}
