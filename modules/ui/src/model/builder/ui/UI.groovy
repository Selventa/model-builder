package model.builder.ui

import groovy.swing.SwingBuilder
import model.builder.web.api.API

import javax.swing.JDialog
import javax.swing.event.ListSelectionListener
import java.awt.FlowLayout

import static java.awt.GridBagConstraints.*

class UI {

    static JDialog toImportRCR(API api) {
        def swing = new SwingBuilder()
        def dialog = swing.dialog(title: 'Import RCR Result')
        def panel = swing.panel() {
            gridBagLayout()
            label(text: 'Name',
                  constraints: gbc(
                      gridx: 0, gridy: 0, gridwidth: 1, gridheight: 1,
                      anchor: FIRST_LINE_START, weightx: 0.05, weighty: 0.15,
                      insets: [0, 15, 0, 0]))
            textField(constraints: gbc(
                      gridx: 1, gridy: 0, gridwidth: 1, gridheight: 1,
                      anchor: PAGE_START, weightx: 0.8, weighty: 0.15,
                      fill: HORIZONTAL))
            label(text: 'Tags', constraints: gbc(
                      gridx: 0, gridy: 1, gridwidth: 1, gridheight: 1,
                      anchor: FIRST_LINE_START, weightx: 0.05, weighty: 0.15,
                      insets: [0, 15, 0, 0]))
            scrollPane(constraints: gbc(
                      gridx: 1, gridy: 1, gridwidth: 1, gridheight: 1,
                      anchor: PAGE_START, weightx: 0.8, weighty: 0.35,
                      fill: BOTH)) {
                list(items: ['One', 'Two', 'Three'])
            }
            scrollPane(constraints: gbc(
                      gridx: 0, gridy: 2, gridwidth: 2, gridheight: 1,
                      anchor: PAGE_START, weightx: 0.8, weighty: 0.85,
                      fill: BOTH)) {
                table(id: 'resultsTable') {
                    tableModel {
                        propertyColumn(header: "Name" ,propertyName: 'name')
                        propertyColumn(header: "Tags" ,propertyName: 'tags')
                        propertyColumn(header: "Created" ,propertyName: 'created')
                    }
                    resultsTable.selectionModel.addListSelectionListener({ evt ->
                        importButton.enabled = true
                    } as ListSelectionListener)
                }
            }
            panel(constraints: gbc(gridx: 0, gridy: 3, gridwidth: 2, gridheight: 1,
                                   anchor: PAGE_END, weightx: 1.0, weighty: 0.1,
                                   fill: HORIZONTAL)) {
                flowLayout(alignment: FlowLayout.RIGHT)
                button(text: 'Cancel', actionPerformed: {dialog.dispose()})
                button(text: 'Search', actionPerformed: {})
                button(id: 'importButton', text: 'Import', enabled: false, actionPerformed: {})
            }
        }
        dialog.contentPane.add(panel)
        dialog.pack()
        dialog.size = [600, 400]
        dialog.locationRelativeTo = null
        dialog.visible = true
        dialog
    }

    static void main(String[] args) {
        def swing = new SwingBuilder()
        swing.edt {
            toImportRCR()
        }
    }
}
