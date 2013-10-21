package model.builder.ui

import groovy.swing.SwingBuilder
import model.builder.ui.api.Dialogs
import model.builder.ui.internal.DialogsImpl
import org.cytoscape.service.util.AbstractCyActivator
import org.jdesktop.swingx.JXList
import org.jdesktop.swingx.JXTable
import org.jdesktop.swingx.JXTaskPane
import org.jdesktop.swingx.JXTaskPaneContainer
import org.osgi.framework.BundleContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Activator extends AbstractCyActivator {

    private static final Logger msg = LoggerFactory.getLogger("CyUserMessages")

    /**
     * {@inheritDoc}
     */
    @Override
    void start(BundleContext bc) {
        SwingBuilder swing = new SwingBuilder()
        swing.registerBeanFactory('taskPaneContainer', JXTaskPaneContainer.class)
        swing.registerBeanFactory('taskPane', JXTaskPane.class)
        swing.registerBeanFactory('jxList', JXList.class)
        swing.registerBeanFactory('jxTable', JXTable.class)

        Dialogs dialogs = new DialogsImpl(swing)
        registerService(bc, dialogs, Dialogs.class, [:] as Properties)
    }
}
