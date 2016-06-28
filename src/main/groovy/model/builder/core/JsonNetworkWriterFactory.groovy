package model.builder.core

import groovy.transform.TupleConstructor
import org.cytoscape.io.BasicCyFileFilter
import org.cytoscape.io.CyFileFilter
import org.cytoscape.io.DataCategory
import org.cytoscape.io.util.StreamUtil
import org.cytoscape.io.write.CyNetworkViewWriterFactory
import org.cytoscape.io.write.CyWriter
import org.cytoscape.model.CyNetwork
import org.cytoscape.view.model.CyNetworkView

@TupleConstructor
class JsonNetworkWriterFactory implements CyNetworkViewWriterFactory {

    final CyFileFilter filter

    static JsonNetworkWriterFactory create(StreamUtil util) {
        BasicCyFileFilter filter = new BasicCyFileFilter(
            ["json"] as String[], ["application/json"] as String[],
            "Model JSON File", DataCategory.NETWORK, util);
        new JsonNetworkWriterFactory(filter)
    }

    @Override
    CyWriter createWriter(OutputStream stream, CyNetworkView cyNv) {
        return new JsonNetworkWriter(cyNv, stream)
    }

    @Override
    CyWriter createWriter(OutputStream stream, CyNetwork cyN) {
        return new JsonNetworkWriter(cyN, stream)
    }

    @Override
    CyFileFilter getFileFilter() {
        return filter
    }
}
