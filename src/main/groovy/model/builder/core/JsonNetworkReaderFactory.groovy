package model.builder.core

import org.cytoscape.io.BasicCyFileFilter
import org.cytoscape.io.CyFileFilter
import org.cytoscape.io.DataCategory
import org.cytoscape.io.read.AbstractInputStreamTaskFactory
import org.cytoscape.io.util.StreamUtil
import org.cytoscape.work.TaskIterator

class JsonNetworkReaderFactory extends AbstractInputStreamTaskFactory {

    final Expando cyr

    static JsonNetworkReaderFactory create(Expando cyr, StreamUtil util) {
        BasicCyFileFilter filter = new BasicCyFileFilter(
            ["json"] as String[], ["application/json"] as String[],
            "Model JSON File", DataCategory.NETWORK, util);
        new JsonNetworkReaderFactory(cyr, filter)
    }

    JsonNetworkReaderFactory(Expando cyr, CyFileFilter filter) {
        super(filter)
        this.cyr = cyr
    }

    @Override
    TaskIterator createTaskIterator(InputStream stream, String name) {
        return new TaskIterator(new JsonNetworkReader(cyr, name, stream))
    }
}
