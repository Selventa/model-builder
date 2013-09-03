package model.builder.ui

import groovy.swing.SwingBuilder
import model.builder.common.SearchResult
import model.builder.web.api.API
import model.builder.web.api.WebResponse

import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.JTextField
import javax.swing.ListSelectionModel
import javax.swing.event.ListSelectionListener
import java.awt.FlowLayout
import java.awt.Window

import static java.awt.GridBagConstraints.*

class UI {

    static JDialog toImportComparison(API api, Expando cyRef, Closure importData) {
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
        def dialog = swing.dialog(title: 'Import Comparison')
        dialog.contentPane.add(searchPanel(api, tags, search, importData))
        dialog.pack()
        dialog.size = [600, 400]
        dialog.locationRelativeTo = null
        dialog.visible = true
        dialog
    }

    static JDialog toImportRcr(API api, Expando cyRef, Closure importData) {
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
        def dialog = swing.dialog(title: 'Import RCR Result')
        dialog.contentPane.add(searchPanel(api, tags, search, importData))
        dialog.pack()
        dialog.size = [600, 400]
        dialog.locationRelativeTo = null
        dialog.visible = true
        dialog
    }

    private static JPanel searchPanel(API api, Closure tagsClosure,
                                      Closure searchClosure, Closure importClosure) {
        new SwingBuilder().panel() {
            def JTextField name
            def JList tags
            def JTable resultsTable
            def JButton importButton

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
                        propertyColumn(header: "Name" ,propertyName: 'name', editable: false)
                        propertyColumn(header: "Tags" ,propertyName: 'tags', editable: false)
                        propertyColumn(header: "Created" ,propertyName: 'created', editable: false)
                    }
                    resTable.selectionModel.addListSelectionListener({ evt ->
                        importButton.enabled = true
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
                    def models = solr.docs.collect {
                        it.tags = it.tags.collect {it.name}.sort().join(', ') ?: ''
                        new SearchResult(api.id(searchKey: it.id),
                                it.name, it.created_at, it.tags)
                    }.sort {it.name}
                    resultsTable.model = tableModel(list: models) {
                        propertyColumn(header: "Name" ,propertyName: 'name', editable: false)
                        propertyColumn(header: "Tags" ,propertyName: 'tags', editable: false)
                        propertyColumn(header: "Created" ,propertyName: 'created', editable: false)
                    }
                })
                importButton = button(text: 'Import', enabled: false, actionPerformed: {
                    def data = resultsTable.model.rowsModel.value
                    def selected = resultsTable.selectedRows.collect(data.&get)
                    selected.each { importClosure.call(it.id) }
                })
            }
        }
    }
}
