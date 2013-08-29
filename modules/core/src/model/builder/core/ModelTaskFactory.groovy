package model.builder.core

import org.cytoscape.work.TaskIterator

interface ModelTaskFactory {

    boolean isReady(Map model)

    TaskIterator createTaskIterator(Map model)
}