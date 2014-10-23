package model.builder.core

import org.cytoscape.view.vizmap.VisualMappingFunctionFactory
import org.cytoscape.view.vizmap.mappings.DiscreteMapping
import org.cytoscape.view.vizmap.mappings.PassthroughMapping

import java.awt.Color

import static model.builder.core.Util.concordanceColor
import static model.builder.core.Util.createColumn
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*
import groovy.transform.TupleConstructor
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor

@TupleConstructor
class ApplyRcrResultStyle extends AbstractTask {

    Expando cyRef
    VisualMappingFunctionFactory dMapFac
    VisualMappingFunctionFactory pMapFac

    @Override
    void run(TaskMonitor monitor) throws Exception {
        def networks = cyRef.cyApplicationManager.selectedNetworks
        networks.collect {
            def table = it.defaultNodeTable
            createColumn(table, 'rcr.concordance.fill', String.class, false, null)
            table
        }.collect {it.allRows}.flatten().each { row ->
            def dir = row.get('direction', String.class)
            def concordance = row.get('concordance', Double.class)
            row.set('rcr.concordance.fill', concordanceColor(dir, concordance))
        }

        def selected = cyRef.cyApplicationManager.selectedNetworkViews
        def kamVs = cyRef.visualMappingManager.allVisualStyles.find {
            it.title == 'KAM Visualization'
        }

        cyRef.visualMappingManager.allVisualStyles.findAll {
            it.title == 'RCR Mechanisms - By Concordance'
        }.each(cyRef.visualMappingManager.&removeVisualStyle)

        def rcrVs = cyRef.visualStyleFactory.createVisualStyle(kamVs)
        rcrVs.title = 'RCR Mechanisms - By Concordance'

        def lock = rcrVs.allVisualPropertyDependencies.find { it.idString == 'nodeSizeLocked' }
        if (lock) lock.setDependency(false)

        DiscreteMapping selectColorMapping = dMapFac.createVisualMappingFunction(
                'selected', Boolean.class, NODE_SELECTED_PAINT) as DiscreteMapping
        selectColorMapping.putMapValue(Boolean.TRUE, new Color(0x00CCCC))
        rcrVs.addVisualMappingFunction(selectColorMapping)

        PassthroughMapping concordancePassthrough = pMapFac.createVisualMappingFunction(
                'rcr.concordance.fill', String.class, NODE_FILL_COLOR) as PassthroughMapping
        rcrVs.addVisualMappingFunction(concordancePassthrough)

        cyRef.visualMappingManager.addVisualStyle(rcrVs);
        cyRef.cyEventHelper.flushPayloadEvents()
        selected.each { view ->
            cyRef.visualMappingManager.setVisualStyle(rcrVs, view)
            rcrVs.apply(view)
            view.updateView()
        }
    }
}
