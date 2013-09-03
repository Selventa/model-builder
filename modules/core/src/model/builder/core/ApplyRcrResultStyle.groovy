package model.builder.core

import org.cytoscape.view.vizmap.VisualMappingFunctionFactory
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues
import org.cytoscape.view.vizmap.mappings.ContinuousMapping
import org.cytoscape.view.vizmap.mappings.DiscreteMapping

import java.awt.Color
import java.awt.Paint

import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*
import groovy.transform.TupleConstructor
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor

@TupleConstructor
class ApplyRcrResultStyle extends AbstractTask {

    Expando cyRef
    VisualMappingFunctionFactory cMapFac
    VisualMappingFunctionFactory dMapFac

    @Override
    void run(TaskMonitor monitor) throws Exception {
        def selected = cyRef.cyApplicationManager.selectedNetworkViews
        def kamVs = cyRef.visualMappingManager.allVisualStyles.find {
            it.title == 'KAM Visualization'
        }

        cyRef.visualMappingManager.allVisualStyles.findAll {
            it.title == 'RCR Mechanisms - By Concordance'
        }.each(cyRef.visualMappingManager.&removeVisualStyle)
        def rcrVs = cyRef.visualStyleFactory.createVisualStyle(kamVs)
        rcrVs.title = 'RCR Mechanisms - By Concordance'

        ContinuousMapping concordanceDown = cMapFac.createVisualMappingFunction(
                'concordance', Double.class, NODE_FILL_COLOR) as ContinuousMapping
        BoundaryRangeValues<Paint> down_0_001 = new BoundaryRangeValues<Paint>(
                new Color(0x000666), new Color(0x000666), new Color(0x333399));
        concordanceDown.addPoint(0.001, down_0_001)
        BoundaryRangeValues<Paint> down_001_005 = new BoundaryRangeValues<Paint>(
                new Color(0x333399), new Color(0x333399), new Color(0x6666CC));
        concordanceDown.addPoint(0.005, down_001_005)
        BoundaryRangeValues<Paint> down_005_01 = new BoundaryRangeValues<Paint>(
                new Color(0x6666CC), new Color(0x6666CC), new Color(0x9999FF));
        concordanceDown.addPoint(0.01, down_005_01)
        BoundaryRangeValues<Paint> down_01_05 = new BoundaryRangeValues<Paint>(
                new Color(0x9999FF), new Color(0x9999FF), new Color(0xCCCCFF));
        concordanceDown.addPoint(0.05, down_01_05)
        BoundaryRangeValues<Paint> down_05_1 = new BoundaryRangeValues<Paint>(
                new Color(0xCCCCFF), new Color(0xCCCCFF), new Color(0xCCCCFF));
        concordanceDown.addPoint(0.1, down_05_1)

        ContinuousMapping concordanceUp = cMapFac.createVisualMappingFunction(
                'concordance', Double.class, NODE_FILL_COLOR) as ContinuousMapping
        BoundaryRangeValues<Paint> up_0_001 = new BoundaryRangeValues<Paint>(
                new Color(0xFFA000), new Color(0xFFA000), new Color(0xFFC800));
        concordanceUp.addPoint(0.001, up_0_001)
        BoundaryRangeValues<Paint> up_001_005 = new BoundaryRangeValues<Paint>(
                new Color(0xFFC800), new Color(0xFFC800), new Color(0xFFE800));
        concordanceUp.addPoint(0.005, up_001_005)
        BoundaryRangeValues<Paint> up_005_01 = new BoundaryRangeValues<Paint>(
                new Color(0xFFE800), new Color(0xFFE800), new Color(0xFFF800));
        concordanceUp.addPoint(0.01, up_005_01)
        BoundaryRangeValues<Paint> up_01_05 = new BoundaryRangeValues<Paint>(
                new Color(0xFFF800), new Color(0xFFF800), new Color(0xFFFF99));
        concordanceUp.addPoint(0.05, up_01_05)
        BoundaryRangeValues<Paint> up_05_1 = new BoundaryRangeValues<Paint>(
                new Color(0xFFFF99), new Color(0xFFFF99), new Color(0xFFFF99));
        concordanceUp.addPoint(0.1, up_05_1)

        DiscreteMapping noDirectionMap = dMapFac.createVisualMappingFunction(
                'direction', String.class, NODE_FILL_COLOR) as DiscreteMapping
        noDirectionMap.putMapValue('None', Color.white)

        rcrVs.addVisualMappingFunction(concordanceUp)
        rcrVs.addVisualMappingFunction(concordanceDown)

        cyRef.visualMappingManager.addVisualStyle(rcrVs);
        selected.each { view ->
            rcrVs.apply(view)
            cyRef.visualMappingManager.setVisualStyle(rcrVs, view)
            view.updateView()
        }
    }
}
