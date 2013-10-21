package model.builder.ui

import groovy.swing.SwingBuilder
import org.jdesktop.swingx.JXTaskPane
import org.jdesktop.swingx.JXTaskPaneContainer

import javax.swing.JFrame
import java.awt.GridLayout

class SwingxTest {

    public static void main(String[] args) {
//        JFrame frame = new JFrame("Frame")
//        JXTaskPaneContainer container = new JXTaskPaneContainer()
//        container.layout = new GridLayout(3, 1)
//        container.autoscrolls = false
//        ['One', 'Two', 'Three'].collect {new JXTaskPane(it)}.each {
//            it.animated = false
//        }.each(container.&add)
//        frame.getContentPane().add(container)
//        frame.visible = true

        def swing = new SwingBuilder()
        swing.registerBeanFactory('taskPaneContainer', JXTaskPaneContainer.class)
        swing.registerBeanFactory('taskPane', JXTaskPane.class)

        swing.edt {
            swing.frame(title: 'Test', visible: true, show: true) {
                taskPaneContainer {
                    taskPane(title: 'One', animated: false)
                    taskPane(title: 'Two', animated: false)
                    taskPane(title: 'Three', animated: false)
                }
            }
        }
    }
}
