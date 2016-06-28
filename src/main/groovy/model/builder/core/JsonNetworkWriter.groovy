package model.builder.core

import groovy.json.JsonBuilder
import org.cytoscape.io.write.CyWriter
import org.cytoscape.model.CyNetwork
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor

import static model.builder.core.ModelUtil.fromView

class JsonNetworkWriter extends AbstractTask implements CyWriter {

    final CyNetwork cyN
    final CyNetworkView cyNv
    final OutputStream stream

    JsonNetworkWriter(CyNetwork cyN, OutputStream stream) {
        this.cyN = cyN
        this.cyNv = null
        this.stream = stream
    }

    JsonNetworkWriter(CyNetworkView cyNv, OutputStream stream) {
        this.cyNv = cyNv
        this.cyN = null
        this.stream = stream
    }

    @Override
    void run(TaskMonitor tm) throws Exception {
        def network = cyN ? fromView(cyN) : fromView(cyNv)
        def json = new JsonBuilder(network).toPrettyString()
        stream.withWriter('UTF-8', {it.write(json)})
    }
}
