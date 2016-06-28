package model.builder.core

import groovy.json.JsonException
import groovy.json.JsonSlurper
import groovy.transform.TupleConstructor
import org.cytoscape.io.read.CyNetworkReader
import org.cytoscape.model.CyNetwork
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import static ModelUtil.*

@TupleConstructor
class JsonNetworkReader extends AbstractTask implements CyNetworkReader {

    final Expando cyr
    final String name
    final InputStream stream

    // mutable state
    private CyNetworkView cyNv

    @Override
    void run(TaskMonitor tm) throws Exception {
        String content = stream.getText('UTF-8')
        try {
            def network = new JsonSlurper().parseText(content)
            def (boolean valid, List errors) = validateJsonFormat(network)
            if (!valid) {
                String errorMsg = errors.join("\n\t")
                throw new RuntimeException("The JSON is invalid for $name:\n\t$errorMsg")
            }
            cyNv = fromNetwork(network, cyr)
        } catch (JsonException ex) {
            throw new RuntimeException("A JSON file was expected for $name.")
        }
    }

    @Override
    CyNetwork[] getNetworks() {
        return [cyNv.model] as CyNetwork[]
    }

    @Override
    CyNetworkView buildCyNetworkView(CyNetwork cyN) {
        cyNv
    }
}
