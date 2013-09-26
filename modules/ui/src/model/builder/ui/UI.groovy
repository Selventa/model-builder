package model.builder.ui

import groovy.swing.SwingBuilder
import model.builder.web.api.AccessInformation
import model.builder.web.api.AuthorizedAPI
import model.builder.web.api.APIManager
import model.builder.web.api.WebResponse

import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JPasswordField
import javax.swing.JTable
import javax.swing.JTextField
import javax.swing.ListSelectionModel
import javax.swing.event.ListSelectionListener
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Window

import static java.awt.GridBagConstraints.*

class UI {

    static def configuration(APIManager mgr,
                             Closure doAuthenticate, Closure onSave) {
        def swing = new SwingBuilder()
        def dialog = swing.dialog(id: 'the_dialog', title: 'Configure SDP',
                                  defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE) {

            def dialog = the_dialog
            def JTextField host, email
            def JPasswordField pass
            def JTable configurationsTable
            borderLayout()
            panel(border: titledBorder(title: 'SDP Access Information'),
                  constraints: BorderLayout.NORTH) {

                borderLayout()
                panel(constraints: BorderLayout.CENTER) {
                    gridBagLayout()

                    label(text: 'Host', constraints: gbc(anchor: LINE_START,
                                                         gridx: 0, gridy: 0,
                                                         gridwidth: 1, gridheight: 1,
                                                         weightx: 0.0, weighty: 0.1,
                                                         insets: [10, 2, 0, 0]))
                    host = textField(constraints: gbc(anchor: LINE_START,
                                                  gridx: 1, gridy: 0,
                                                  gridwidth: 1, gridheight: 1,
                                                  weightx: 0.2, weighty: 0.1,
                                                  fill: HORIZONTAL, insets: [10, 0, 0, 0]))

                    label(text: 'Email', constraints: gbc(anchor: LINE_START,
                                                          gridx: 0, gridy: 1,
                                                          gridwidth: 1, gridheight: 1,
                                                          weightx: 0.0, weighty: 0.1,
                                                          insets: [10, 2, 0, 0]))
                    email = textField(constraints: gbc(anchor: LINE_START,
                                                   gridx: 1, gridy: 1,
                                                   gridwidth: 1, gridheight: 1,
                                                   weightx: 0.2, weighty: 0.1,
                                                   fill: HORIZONTAL, insets: [10, 0, 0, 0]))

                    label(text: 'Password', constraints: gbc(anchor: LINE_START,
                                                             gridx: 2, gridy: 1,
                                                             gridwidth: 1, gridheight: 1,
                                                             weightx: 0.0, weighty: 0.1,
                                                             insets: [10, 20, 0, 10]))
                    pass = passwordField(constraints: gbc(anchor: LINE_START,
                                                      gridx: 3, gridy: 1,
                                                      gridwidth: 1, gridheight: 1,
                                                      weightx: 0.2, weighty: 0.1,
                                                      fill: HORIZONTAL, insets: [10, 0, 0, 0]))

                    panel(constraints: gbc(anchor: LAST_LINE_END,
                                           gridx: 3, gridy: 2, gridwidth: 1, gridheight: 1,
                                           weightx: 0.3, weighty: 0.1, fill: HORIZONTAL,
                                           insets: [10, 0, 0, 0])) {
                        flowLayout(alignment: FlowLayout.RIGHT)
                        button(text: 'Add', preferredSize: [85, 25], actionPerformed: {
                            doOutside {
                                def hostVal = host.text, emailVal = email.text, passVal = pass.password as String
                                def String apiKey = doAuthenticate.call(hostVal, emailVal, passVal)
                                if (apiKey) {
                                    AccessInformation ai = new AccessInformation(false, hostVal, emailVal, apiKey, passVal)
                                    edt {
                                        // add to table
                                        resTable.model.with {
                                            rowsModel.value.add(ai)
                                            fireTableDataChanged()
                                        }
                                    }
                                } else {
                                    MessagePopups.errorConnectionAccess(host.text, email.text, pass.password as String)
                                }
                            }
                        })
                    }
                }
            }

            panel(constraints: BorderLayout.CENTER) {

                borderLayout()
                scrollPane(constraints: BorderLayout.CENTER) {

                    configurationsTable = table(id: 'resTable') {
                        tableModel {
                            closureColumn(header:  'Default', type: Boolean.class,
                                          read: {r -> r.defaultAccess},
                                          write: {r, v ->
                                              if (v) {
                                                  resTable.model.with {
                                                      rowsModel.value.each {
                                                          it.defaultAccess = false
                                                      }
                                                      fireTableDataChanged()
                                                  }
                                              }
                                              r.defaultAccess = v
                                          })
                            closureColumn(header:  'Name',    read: {it.toString()})
                            propertyColumn(header: 'Host' ,   propertyName: 'host',    editable: false)
                            propertyColumn(header: 'Email' ,  propertyName: 'email',   editable: false)
                        }

                        resTable.model.with {
                            mgr.all().each(rowsModel.value.&add)
                            fireTableDataChanged()
                        }
                    }
                }
                panel(constraints: BorderLayout.EAST) {

                    button(icon: Util.icon('/delete.png', 'Delete selection'), actionPerformed: {
                        def data = configurationsTable.model.rowsModel.value
                        configurationsTable.selectedRows.
                                collect(configurationsTable.&convertRowIndexToModel).
                                collect(data.&get).
                                each(data.&remove)
                        configurationsTable.model.with {
                            fireTableDataChanged()
                        }
                    })
                }
            }

            panel(constraints: BorderLayout.SOUTH) {
                flowLayout(alignment: FlowLayout.RIGHT)
                button(text: 'Cancel', preferredSize: [85, 25],
                       actionPerformed: {dialog.dispose()})
                button(text: 'Save', preferredSize: [85, 25], actionPerformed: {
                    configurationsTable.model.with {
                        def rows = rowsModel.value
                        onSave.call(rows as Set)
                    }
                })
            }
        }
        dialog.pack()
        dialog.size = [700, 400]
        dialog.locationRelativeTo = null
        dialog.visible = true
        dialog
    }

    static JDialog importModel(AuthorizedAPI api, Closure importData) {
        def tags = {
            WebResponse res = api.tags(['model'])
            def facetTags = res.data.facet_counts.facet_fields.tags
            facetTags.collate(2).findAll {it[1] > 0}.collect {it[0]}.sort()
        }

        def search = { data ->
            def species = []
            if (data.human) species << '9606'
            if (data.mouse) species << '10090'
            if (data.rat) species << '10116'

            api.searchModels([
                    name: data.name ? "*${data.name}*" : "",
                    species: species,
                    tags: (data.tags ?: []) as String[],
                    rows: 100])
        }

        def swing = new SwingBuilder()
        def dialog = swing.dialog(title: 'Import Model')
        dialog.contentPane.add(modelSearchPanel(api, tags, search, importData))
        dialog.pack()
        dialog.size = [600, 400]
        dialog.locationRelativeTo = null
        dialog.visible = true
        dialog
    }

    static JDialog addComparison(AuthorizedAPI api, Closure importData) {
        def tags = {
            WebResponse res = api.tags(['comparison'])
            def facetTags = res.data.facet_counts.facet_fields.tags
            facetTags.collate(2).findAll {it[1] > 0}.collect {it[0]}.sort()
        }

        def search = { data ->
            api.searchComparisons([
                name: data.name ? "*${data.name}*" : "",
                tags: (data.tags ?: []) as String[],
                rows: 100])
        }

        def swing = new SwingBuilder()
        def dialog = swing.dialog(title: 'Add Comparison')
        dialog.contentPane.add(searchPanel(api, tags, search, importData))
        dialog.pack()
        dialog.size = [600, 400]
        dialog.locationRelativeTo = null
        dialog.visible = true
        dialog
    }

    static JDialog addRcr(AuthorizedAPI api, Closure importData) {
        def tags = {
            WebResponse res = api.tags(['rcr_result'])
            def facetTags = res.data.facet_counts.facet_fields.tags
            facetTags.collate(2).findAll {it[1] > 0}.collect {it[0]}.sort()
        }

        def search = { data ->
            api.searchRcrResults([
                    name: data.name ? "*${data.name}*" : "",
                    tags: (data.tags ?: []) as String[],
                    rows: 100])
        }

        def swing = new SwingBuilder()
        def dialog = swing.dialog(title: 'Add RCR Result')
        dialog.contentPane.add(searchPanel(api, tags, search, importData))
        dialog.pack()
        dialog.size = [600, 400]
        dialog.locationRelativeTo = null
        dialog.visible = true
        dialog
    }

    private static JPanel searchPanel(AuthorizedAPI api, Closure tagsClosure,
                                      Closure searchClosure, Closure importClosure) {
        new SwingBuilder().panel() {
            def JTextField name
            def JList tags
            def JTable resultsTable
            def JButton addButton

            gridBagLayout()
            label(text: 'Name',
                    constraints: gbc(
                            gridx: 0, gridy: 0, gridwidth: 1, gridheight: 1,
                            anchor: FIRST_LINE_START, weightx: 0.05, weighty: 0.15,
                            insets: [0, 15, 0, 0]))
            name = textField(constraints: gbc(
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
                tags = list(items: tagsClosure.call())
            }
            scrollPane(constraints: gbc(
                    gridx: 0, gridy: 2, gridwidth: 2, gridheight: 1,
                    anchor: PAGE_START, weightx: 0.8, weighty: 0.85,
                    fill: BOTH)) {
                resultsTable = table(id: 'resTable', selectionMode: ListSelectionModel.SINGLE_SELECTION) {
                    tableModel {
                        propertyColumn(header: 'Name' ,propertyName: 'name', editable: false)
                        propertyColumn(header: 'Tags' ,propertyName: 'tags', editable: false)
                        propertyColumn(header: 'Created' ,propertyName: 'created_at', editable: false)
                        propertyColumn(header: 'Updated' ,propertyName: 'updated_at', editable: false)
                    }
                    resTable.selectionModel.addListSelectionListener({ evt ->
                        addButton.enabled = true
                    } as ListSelectionListener)
                }
            }
            panel(constraints: gbc(gridx: 0, gridy: 3, gridwidth: 2, gridheight: 1,
                    anchor: PAGE_END, weightx: 1.0, weighty: 0.1,
                    fill: HORIZONTAL)) {
                flowLayout(alignment: FlowLayout.RIGHT)
                button(id: 'cancelButton', text: 'Cancel', actionPerformed: {
                    def p = cancelButton.parent
                    while (p != null) {
                        if (p instanceof Window) {
                            p.dispose()
                            p = null
                        } else {
                            p = p.parent
                        }
                    }
                })
                button(text: 'Search', actionPerformed: {
                    WebResponse res = searchClosure.call([name: name.text,
                                                          tags: tags.selectedValuesList])
                    def solr = res.data.response
                    def results = solr.docs.collect {
                        it.tags = it.tags.collect {it.name}.sort().join(', ') ?: ''
                        it.id = api.id(searchKey: it.id)
                        it
                    }.sort {it.name}
                    resultsTable.model = tableModel(list: results) {
                        propertyColumn(header: 'Name' ,propertyName: 'name', editable: false)
                        propertyColumn(header: 'Tags' ,propertyName: 'tags', editable: false)
                        propertyColumn(header: 'Created' ,propertyName: 'created_at', editable: false)
                        propertyColumn(header: 'Updated' ,propertyName: 'updated_at', editable: false)
                    }
                })
                addButton = button(text: 'Add', enabled: false, actionPerformed: {
                    def data = resultsTable.model.rowsModel.value
                    def selected = resultsTable.selectedRows.collect(data.&get)
                    selected.each { importClosure.call(it.id) }
                })
            }
        }
    }

    private static JPanel modelSearchPanel(AuthorizedAPI api, Closure tagsClosure,
                                           Closure searchClosure, Closure importClosure) {
        def swing = new SwingBuilder()
        swing.panel() {
            def JTextField name
            def JCheckBox human
            def JCheckBox mouse
            def JCheckBox rat
            def JList tags
            def JTable resultsTable
            def JButton addButton

            gridBagLayout()
            label(text: 'Name',
                    constraints: gbc(
                            gridx: 0, gridy: 0, gridwidth: 1, gridheight: 1,
                            anchor: FIRST_LINE_START, weightx: 0.05, weighty: 0.15,
                            insets: [0, 15, 0, 0]))
            name = textField(constraints: gbc(
                    gridx: 1, gridy: 0, gridwidth: 1, gridheight: 1,
                    anchor: PAGE_START, weightx: 0.8, weighty: 0.15,
                    fill: HORIZONTAL))
            label(text: 'Species',
                    constraints: gbc(
                            gridx: 0, gridy: 1, gridwidth: 1, gridheight: 1,
                            anchor: FIRST_LINE_START, weightx: 0.05, weighty: 0.15,
                            insets: [0, 15, 0, 0]))
            human = checkBox(text: 'Human', constraints: gbc(
                    gridx: 1, gridy: 1, gridwidth: 1, gridheight: 1,
                    anchor: PAGE_START, weightx: 0.8, weighty: 0.15,
                    fill: HORIZONTAL))
            mouse = checkBox(text: 'Mouse', constraints: gbc(
                    gridx: 1, gridy: 1, gridwidth: 1, gridheight: 1,
                    anchor: PAGE_START, weightx: 0.8, weighty: 0.15,
                    fill: HORIZONTAL))
            rat =   checkBox(text: 'Rat', constraints: gbc(
                    gridx: 1, gridy: 1, gridwidth: 1, gridheight: 1,
                    anchor: PAGE_START, weightx: 0.8, weighty: 0.15,
                    fill: HORIZONTAL))
            label(text: 'Tags', constraints: gbc(
                    gridx: 0, gridy: 2, gridwidth: 1, gridheight: 1,
                    anchor: FIRST_LINE_START, weightx: 0.05, weighty: 0.15,
                    insets: [0, 15, 0, 0]))
            scrollPane(constraints: gbc(
                    gridx: 1, gridy: 2, gridwidth: 1, gridheight: 1,
                    anchor: PAGE_START, weightx: 0.8, weighty: 0.35,
                    fill: BOTH)) {
                tags = list(items: tagsClosure.call())
            }
            scrollPane(constraints: gbc(
                    gridx: 0, gridy: 3, gridwidth: 2, gridheight: 1,
                    anchor: PAGE_START, weightx: 0.8, weighty: 0.85,
                    fill: BOTH)) {
                resultsTable = table(id: 'resTable', selectionMode: ListSelectionModel.MULTIPLE_INTERVAL_SELECTION) {
                    tableModel {
                        propertyColumn(header: 'Name' ,propertyName: 'name', editable: false)
                        propertyColumn(header: 'Tags' ,propertyName: 'tags', editable: false)
                        propertyColumn(header: 'Created' ,propertyName: 'created_at', editable: false)
                        propertyColumn(header: 'Updated' ,propertyName: 'updated_at', editable: false)
                    }
                    resTable.selectionModel.addListSelectionListener({ evt ->
                        addButton.enabled = true
                    } as ListSelectionListener)
                }
            }
            panel(constraints: gbc(gridx: 0, gridy: 4, gridwidth: 2, gridheight: 1,
                    anchor: PAGE_END, weightx: 1.0, weighty: 0.1,
                    fill: HORIZONTAL)) {
                flowLayout(alignment: FlowLayout.RIGHT)
                button(id: 'cancelButton', text: 'Cancel', actionPerformed: {
                    def p = cancelButton.parent
                    while (p != null) {
                        if (p instanceof Window) {
                            p.dispose()
                            p = null
                        } else {
                            p = p.parent
                        }
                    }
                })
                button(text: 'Search', actionPerformed: {
                    WebResponse res = searchClosure.call([
                            name: name.text,
                            tags: tags.selectedValuesList,
                            human: human.selected, mouse: mouse.selected,
                            rat: rat.selected])
                    def solr = res.data.response
                    def results = solr.docs.collect {
                        it.tags = it.tags.collect {it.name}.sort().join(', ') ?: ''
                        it.id = api.id(searchKey: it.id)
                        it
                    }.sort {it.name}
                    resultsTable.model = tableModel(list: results) {
                        propertyColumn(header: 'Name' ,propertyName: 'name', editable: false)
                        propertyColumn(header: 'Tags' ,propertyName: 'tags', editable: false)
                        propertyColumn(header: 'Created' ,propertyName: 'created_at', editable: false)
                        propertyColumn(header: 'Updated' ,propertyName: 'updated_at', editable: false)
                    }
                })
                addButton = button(text: 'Import', enabled: false, actionPerformed: {
                    def data = resultsTable.model.rowsModel.value
                    def selected = resultsTable.selectedRows.collect(data.&get)
                    importClosure.call(selected)
                })
            }
        }
    }
}
