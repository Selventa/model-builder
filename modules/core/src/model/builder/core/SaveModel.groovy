package model.builder.core

import org.cytoscape.model.CyNetwork
import org.cytoscape.task.AbstractNetworkTask
import org.cytoscape.work.TaskMonitor
import org.cytoscape.work.Tunable

class SaveModel extends AbstractNetworkTask {

    @Tunable(description="Revision summary")
    public String summary = '';

    SaveModel(CyNetwork network) {
        super(network)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void run(TaskMonitor monitor) throws Exception {}
}
