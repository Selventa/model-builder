package model.builder.ui.internal

import ca.odell.glazedlists.BasicEventList
import ca.odell.glazedlists.event.ListEventListener
import ca.odell.glazedlists.gui.TableFormat
import ca.odell.glazedlists.swing.DefaultEventTableModel
import groovy.swing.SwingBuilder
import groovy.transform.TupleConstructor
import model.builder.ui.Util
import model.builder.ui.api.Dialogs
import model.builder.web.api.AuthorizedAPI
import model.builder.web.api.JsonStreamResponse
import model.builder.web.api.WebResponse
import org.cytoscape.application.CyApplicationManager
import org.jdesktop.swingx.JXTable
import org.jdesktop.swingx.decorator.HighlightPredicate
import org.jdesktop.swingx.decorator.ToolTipHighlighter
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.swing.DefaultComboBoxModel
import javax.swing.JDialog
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.RowSorter.SortKey
import javax.swing.SortOrder
import javax.swing.event.ListSelectionListener
import javax.swing.table.AbstractTableModel
import java.awt.BorderLayout
import java.awt.Color
import java.awt.FlowLayout

import static javax.swing.ScrollPaneConstants.*
import static model.builder.common.facet.Functions.*
import static org.cytoscape.model.CyNetwork.NAME
import static org.cytoscape.model.CyTableUtil.getNodesInState

@TupleConstructor
class DialogsImpl implements Dialogs {

    private static final Logger msg = LoggerFactory.getLogger("CyUserMessages")
    SwingBuilder swing

    @Override
    JDialog pathSearch(CyApplicationManager appMgr, AuthorizedAPI api, Map controls,
                       Closure addEdges) {
        def merged = [
            direction: 'both', max_length: 2, causal_only: true,
            starts: 'Add from network(s) selection', ends: 'Add from network(s) selection',
            knowledge_network: 'Full - Human'
        ] + controls

        def starts = new BasicEventList()
        def startsModel = new DefaultEventTableModel(starts, [
            getColumnCount: {1},
            getColumnName: {i -> 'Start'},
            getColumnValue: {o, i -> o}] as TableFormat)
        def ends = new BasicEventList()
        def endsModel = new DefaultEventTableModel(ends, [
            getColumnCount: {1},
            getColumnName: {i -> 'End'},
            getColumnValue: {o, i -> o} ] as TableFormat)

        def JFileChooser fileChooser = new JFileChooser(
                dialogTitle: 'Choose a text file',
                fileSelectionMode: JFileChooser.FILES_ONLY,
                multiSelectionEnabled: true)

        def dialog = swing.dialog(id: 'path_search_dialog', title: 'Find Paths',
                defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE, modal: false) {

            borderLayout()
            panel(constraints: BorderLayout.CENTER) {
                gridLayout(rows: 1, columns: 2)
                panel() {
                    borderLayout()
                    panel(constraints: BorderLayout.NORTH) {
                        flowLayout(alignment: FlowLayout.LEFT)
                        label(text: 'Start of paths')
                    }
                    scrollPane(constraints: BorderLayout.CENTER, viewportView: jxTable(id: 'startsTable', model: startsModel))
                    panel(constraints: BorderLayout.SOUTH) {
                        borderLayout()
                        panel(constraints: BorderLayout.WEST) {
                            flowLayout(alignment: FlowLayout.LEFT)
                            button(icon: Util.icon('/delete.png', 'Delete selection'), actionPerformed: {
                                swing.edt {
                                    def deleted = startsTable.selectedRows.
                                            collect(startsTable.&convertRowIndexToModel).collect {starts.get(it)}
                                    def remaining = starts - deleted
                                    starts.removeAll {true}
                                    starts.addAll(remaining)

                                    if (!starts) findButton.enabled = false
                                }
                            })
                        }
                        panel(constraints: BorderLayout.EAST) {
                            flowLayout(alignment: FlowLayout.RIGHT)
                            comboBox(id: 'startsCombo',
                                items: ['Add from file(s)', 'Add from network(s) selection'],
                                selectedItem: merged.starts)
                            button(action: action(name: 'Add Starts', mnemonic: 'S', closure: {
                                def item = startsCombo.selectedItem
                                switch (item) {
                                    case 'Add from file(s)':
                                        doOutside {
                                            int ret = fileChooser.showOpenDialog(path_search_dialog)
                                            if (ret == JFileChooser.APPROVE_OPTION) {
                                                def fileItems = fileChooser.selectedFiles.collect { f ->
                                                    def lines = []
                                                    f.eachLine('utf-8', {
                                                        lines.add(it.trim())
                                                    })
                                                    lines
                                                }.flatten().unique().sort()
                                                swing.edt {
                                                    def newStarts = (starts + fileItems).unique().sort()
                                                    starts.removeAll {true}
                                                    starts.addAll(newStarts)
                                                    if (starts && ends) findButton.enabled = true
                                                }
                                            }
                                        }
                                        break
                                    case 'Add from network(s) selection':
                                        swing.doOutside {
                                            def selection = appMgr.selectedNetworks.collect { cyN ->
                                                getNodesInState(cyN, 'selected', true).collect {
                                                    cyN.getRow(it).get(NAME, String.class)
                                                }
                                            }.flatten().unique()
                                            swing.edt {
                                                def newStarts = (starts + selection).unique().sort()
                                                starts.removeAll {true}
                                                starts.addAll(newStarts)
                                                if (starts && ends) findButton.enabled = true
                                            }
                                        }
                                        break
                                }
                            }))
                        }
                    }
                }
                panel() {
                    borderLayout()
                    panel(constraints: BorderLayout.NORTH) {
                        flowLayout(alignment: FlowLayout.LEFT)
                        label(text: 'End of paths')
                    }
                    scrollPane(constraints: BorderLayout.CENTER, viewportView: jxTable(id: 'endsTable', model: endsModel))
                    panel(constraints: BorderLayout.SOUTH) {
                        borderLayout()
                        panel(constraints: BorderLayout.WEST) {
                            flowLayout(alignment: FlowLayout.LEFT)
                            button(icon: Util.icon('/delete.png', 'Delete selection'), actionPerformed: {
                                swing.edt {
                                    def deleted = endsTable.selectedRows.
                                            collect(endsTable.&convertRowIndexToModel).collect {ends.get(it)}
                                    def remaining = ends - deleted
                                    ends.removeAll {true}
                                    ends.addAll(remaining)

                                    if (!ends) findButton.enabled = false
                                }
                            })
                        }
                        panel(constraints: BorderLayout.EAST) {
                            flowLayout(alignment: FlowLayout.RIGHT)
                            comboBox(id: 'endsCombo',
                                    items: ['Add from file(s)', 'Add from network(s) selection'],
                                    selectedItem: merged.ends)
                            button(action: action(name: 'Add Ends', mnemonic: 'E', closure: {
                                def item = endsCombo.selectedItem
                                switch (item) {
                                    case 'Add from file(s)':
                                        doOutside {
                                            int ret = fileChooser.showOpenDialog(path_search_dialog)
                                            if (ret == JFileChooser.APPROVE_OPTION) {
                                                def fileItems = fileChooser.selectedFiles.collect { f ->
                                                    def lines = []
                                                    f.eachLine('utf-8', {
                                                        lines.add(it.trim())
                                                    })
                                                    lines
                                                }.flatten().unique().sort()
                                                swing.edt {
                                                    def newEnds = (ends + fileItems).unique().sort()
                                                    ends.removeAll {true}
                                                    ends.addAll(newEnds)
                                                    if (starts && ends) findButton.enabled = true
                                                }
                                            }
                                        }
                                        break
                                    case 'Add from network(s) selection':
                                        swing.doOutside {
                                            def selection = appMgr.selectedNetworks.collect { cyN ->
                                                getNodesInState(cyN, 'selected', true).collect {
                                                    cyN.getRow(it).get(NAME, String.class)
                                                }
                                            }.flatten().unique()
                                            swing.edt {
                                                def newEnds = (ends + selection).unique().sort()
                                                ends.removeAll {true}
                                                ends.addAll(newEnds)
                                                if (starts && ends) findButton.enabled = true
                                            }
                                        }
                                        break
                                }
                            }))
                        }
                    }
                }
            }

            panel(constraints: BorderLayout.SOUTH) {
                borderLayout()
                taskPaneContainer(constraints: BorderLayout.CENTER) {
                    taskPane(title: 'Controls', animated: false, collapsed: false) {
                        gridBagLayout()
                        label(text: 'Knowledge network', constraints: gbc(
                                gridx: 0, gridy: 0, gridwidth: 1, gridheight: 1,
                                anchor: FIRST_LINE_START, weightx: 0.05, weighty: 0.15,
                                insets: [0, 15, 0, 0]))
                        comboBox(id: 'knowledge_network', constraints: gbc(
                                gridx: 1, gridy: 0, gridwidth: 1, gridheight: 1,
                                anchor: FIRST_LINE_START, weightx: 0.5, weighty: 0.15)) {
                            swing.doOutside {
                                WebResponse res = api.knowledgeNetworks()
                                switch(res.statusCode) {
                                    case 200:
                                        def kns = res.data.collect { it.name }.sort() as String[]
                                        if (kns) {
                                            knowledge_network.model = new DefaultComboBoxModel(kns)
                                            knowledge_network.selectedItem = merged.knowledge_network
                                        }
                                        break
                                    default:
                                        msg.error("Error retrieving knowledge networks (${res.statusCode})")
                                        findButton.enabled = false
                                }
                            }
                        }
                        label(text: 'Search direction', constraints: gbc(
                                gridx: 0, gridy: 1, gridwidth: 1, gridheight: 1,
                                anchor: FIRST_LINE_START, weightx: 0.05, weighty: 0.15,
                                insets: [0, 15, 0, 0]))
                        comboBox(id: 'direction', constraints: gbc(
                                gridx: 1, gridy: 1, gridwidth: 1, gridheight: 1,
                                anchor: FIRST_LINE_START, weightx: 0.5, weighty: 0.15),
                                items: ['out', 'in', 'both'], selectedItem: merged.direction)
                        label(text: 'Max path length', constraints: gbc(
                                gridx: 0, gridy: 2, gridwidth: 1, gridheight: 1,
                                anchor: FIRST_LINE_START, weightx: 0.05, weighty: 0.15,
                                insets: [0, 15, 0, 0]))
                        slider(id: 'max_path_length', value: merged.max_length, minimum: 1, maximum: 4,
                                paintTicks: true, paintTrack: true, snapToTicks: true,
                                majorTickSpacing: 1, paintLabels: true, constraints: gbc(
                                gridx: 1, gridy: 2, gridwidth: 1, gridheight: 1,
                                anchor: FIRST_LINE_START, weightx: 0.5, weighty: 0.15))
                        label(text: 'Causal only?', constraints: gbc(
                                gridx: 0, gridy: 3, gridwidth: 1, gridheight: 1,
                                anchor: FIRST_LINE_START, weightx: 0.05, weighty: 0.15,
                                insets: [0, 15, 0, 0]))
                        human = checkBox(id: 'causal_only', selected: merged.causal_only, constraints: gbc(
                                gridx: 1, gridy: 3, gridwidth: 1, gridheight: 1,
                                anchor: FIRST_LINE_START, weightx: 0.8, weighty: 0.15))
                        label(text: 'Number of paths', constraints: gbc(
                                gridx: 0, gridy: 4, gridwidth: 1, gridheight: 1,
                                anchor: FIRST_LINE_START, weightx: 0.05, weighty: 0.15,
                                insets: [0, 15, 0, 0]))
                        spinner(id: 'num_returned', model:spinnerNumberModel(minimum:1,
                                maximum: 5000, value: 500, stepSize: 500), constraints: gbc(
                                gridx: 1, gridy: 4, gridwidth: 1, gridheight: 1,
                                anchor: FIRST_LINE_START, weightx: 0.8, weighty: 0.15))
                    }
                }
                panel(constraints: BorderLayout.SOUTH) {
                    flowLayout(alignment: FlowLayout.RIGHT)
                    label(id: 'pathMsg')
                    button(defaultButton: true, action: action(name: 'Cancel', mnemonic: 'C', closure: {
                        path_search_dialog.dispose()
                    }))
                    button(id: 'findButton', action: action(enabled: false, name: 'Find', mnemonic: 'F', closure: {
                        pathMsg.text = 'Finding paths...'
                        swing.doOutside {
                            def params = [
                                direction: direction.selectedItem,
                                max_path_length: max_path_length.value,
                                num_returned: num_returned.value
                            ]
                            if (causal_only.selected) params.rel_include = [
                                    'increases', 'directlyIncreases', 'decreases',
                                    'directlyDecreases', 'rateLimitingStepOf'
                            ]
                            JsonStreamResponse res = api.paths(knowledge_network.selectedItem, starts, ends, params)
                            if (!res.jsonObjects.hasNext()) {
                                swing.edt {
                                    pathMsg.text = 'Zero paths found'
                                }
                            } else {
                                pathFacet(appMgr, res.jsonObjects, denormalizePath, addEdges)
                                pathMsg.text = ''
                            }
                        }
                    }))
                }
            }
        }

        dialog.pack()
        dialog.size = [1000, 600]
        dialog.locationRelativeTo = null
        dialog.visible = true
        dialog
    }

    @Override
    JDialog pathFacet(CyApplicationManager appMgr, Iterator<Map> itemIterator,
                      Closure denormalize, Closure addEdges) {
        def items = [itemIterator.take(1).next()]
        def fieldDescriptions = []

        def columns = ['Start node', 'End node', 'Goes Through', 'Is Causal?']

        fieldDescriptions.addAll(describe(items, denormalize))
        def facets = facet(fieldDescriptions) as Map

        def resultEventList = new BasicEventList()
        resultEventList.addAll(fieldDescriptions)
        def filteredResults = new DefaultEventTableModel(resultEventList,
                [
                        getColumnCount: {columns.size()},
                        getColumnName: {i -> columns[i]},
                        getColumnValue: {o, i ->
                            switch(i) {
                                case 0: return o.start_term
                                case 1: return o.end_term
                                case 2: return o.intermediate_entities.join(', ')
                                case 3: return o.causal
                            }
                        }
                ] as TableFormat
        )
        def resultsLabel
        resultEventList.addListEventListener({
            listChanged: {
                resultsLabel.text = "Total: ${resultEventList.size()}"
            }
        } as ListEventListener)
        def addPathsButton
        def results = swing.jxTable(model: filteredResults, columnControlVisible: true)
        results.selectionModel.addListSelectionListener([
            valueChanged: { evt ->
                addPathsButton.enabled = results.selectedRows
            }
        ] as ListSelectionListener)

        def facetFieldModels = []
        def createTaskPane = {k, v ->
            def title = k.split('_').collect{it.capitalize()}.join(' ')
            def facetFieldModel = new FacetFieldModel(facets, facets[k], fieldDescriptions, resultEventList, results)
            def tbl
            def pane = swing.taskPane(title: title, animated: false, collapsed: true) {
                scrollPane(border: lineBorder(thickness: 1, color: Color.black),
                        constraints: BorderLayout.CENTER, background: Color.white) {
                    tbl = swing.table(model: facetFieldModel, autoCreateRowSorter: true)
                }
            }
            tbl.columnModel.getColumn(0).minWidth = 40
            tbl.columnModel.getColumn(0).maxWidth = 40
            tbl.columnModel.getColumn(0).preferredWidth = 40
            tbl.columnModel.getColumn(0).resizable = false

            tbl.columnModel.getColumn(1).minWidth = 40
            tbl.columnModel.getColumn(1).maxWidth = 40
            tbl.columnModel.getColumn(1).preferredWidth = 40
            tbl.columnModel.getColumn(1).resizable = false

            tbl.columnModel.getColumn(2).minWidth = 100
            tbl.columnModel.getColumn(2).resizable = true

            tbl.columnModel.getColumn(3).minWidth = 60
            tbl.columnModel.getColumn(3).maxWidth = 60
            tbl.columnModel.getColumn(3).preferredWidth = 60
            tbl.columnModel.getColumn(3).resizable = false
            tbl.rowSorter.sortKeys = [new SortKey(3, SortOrder.DESCENDING)]
            facetFieldModels << facetFieldModel
            pane
        }

        def dialog = swing.dialog(id: 'path_facet_dialog', title: 'Pathfind',
                defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE, modal: false) {

            panel {
                borderLayout()
                splitPane(constraints: BorderLayout.CENTER, continuousLayout: true,
                          dividerLocation: 400, oneTouchExpandable: true,
                    leftComponent: scrollPane(id: 'facetScrollPane',
                            horizontalScrollBarPolicy: HORIZONTAL_SCROLLBAR_NEVER,
                            verticalScrollBarPolicy: VERTICAL_SCROLLBAR_AS_NEEDED,
                            minimumSize: [400, 600]) {
                        taskPaneContainer(id: 'facetContainer') {
                            facets.each(createTaskPane)
                        }
                    },
                    rightComponent: panel {
                        borderLayout()
                        scrollPane(id: 'resultScrollPane', viewportView: results,
                                constraints: BorderLayout.CENTER)
                        panel(constraints: BorderLayout.SOUTH) {
                            resultsLabel = label()
                        }
                    }
                )
                panel(constraints: BorderLayout.SOUTH) {
                    flowLayout(alignment: FlowLayout.RIGHT)
                    button(defaultButton: true, action: action(name: 'Cancel', mnemonic: 'C', closure: {
                        path_facet_dialog.dispose()
                    }))
                    addPathsButton = button(action: action(enabled: false, name: 'Add Selected Paths', mnemonic: 'A', closure: {
                        swing.doOutside {
                            def sel = results.selectedRows.
                                collect(results.&convertRowIndexToModel).
                                collect {resultEventList.get(it)}
                            def selItems = sel.
                                collect(fieldDescriptions.&indexOf).
                                collect(items.&get)
                            addEdges.call(selItems.collect{it.path}.flatten().findAll{it.relationship})
                        }
                    }))
                }
            }
        }
        results.addHighlighter(new ToolTipHighlighter(HighlightPredicate.IS_TEXT_TRUNCATED))

        dialog.pack()
        dialog.size = [1000, 600]
        dialog.locationRelativeTo = null
        dialog.visible = true

        swing.doOutside {
            while (itemIterator.hasNext()) {
                // load in 100 chunk increments
                def next = itemIterator.take(100).toList()
                items += next
                def descriptions = describe(next, denormalize)

                swing.edt {
                    fieldDescriptions.addAll(descriptions)
                    resultEventList.addAll(descriptions)

                    def newFacets = facet(descriptions)
                    def newFacetKeys = newFacets.keySet() - facets.keySet()
                    mergeFacets([facets, newFacets])
                    newFacets.each { k, v ->
                        if (k in newFacetKeys) {
                            def taskPane = createTaskPane.call(k, v)
                            facetContainer.add(taskPane)
                            facetScrollPane.doLayout()
                        }
                    }
                    facetFieldModels.each {it.fireTableDataChanged()}
                }
            }
        }

        dialog
    }

    @TupleConstructor
    private final class FacetFieldModel extends AbstractTableModel {

        Map facets
        Map facetField
        List allResults
        List filteredResults
        JXTable resultsTable

        @Override
        int getRowCount() {
            return facetField.size()
        }

        @Override
        int getColumnCount() {
            return 4
        }

        @Override
        String getColumnName(int col) {
            switch(col) {
                case 0: return 'Incl'
                case 1: return 'Excl'
                case 2: return 'Value'
                case 3: return '#'
            }
        }

        @Override
        Class<?> getColumnClass(int col) {
            switch(col) {
                case 0: return Boolean.class
                case 1: return Boolean.class
                case 2: return Object.class
                case 3: return Integer.class
            }
        }

        @Override
        boolean isCellEditable(int row, int col) {
            col == 0 || col == 1
        }

        @Override
        Object getValueAt(int row, int col) {
            def fieldValue = facetField.values().toList().get(row)
            switch(col) {
                case 0: return fieldValue.filterComparison == 'inclusion'
                case 1: return fieldValue.filterComparison == 'exclusion'
                case 2: return fieldValue.value
                case 3: return fieldValue.count
            }
        }

        @Override
        void setValueAt(Object val, int row, int col) {
            def fieldValue = facetField.values().toList().get(row)
            switch(col) {
                case 0:
                    fieldValue.filterComparison = (val ? 'inclusion' : 'unset'); break
                case 1:
                    fieldValue.filterComparison = (val ? 'exclusion' : 'unset'); break
                case 2: fieldValue.value = val; break
                case 3: fieldValue.count = val; break
            }

            swing.edt {
                def res = filter(allResults, allResults, facets)
                filteredResults.removeAll {true}
                filteredResults.addAll(res)
                resultsTable.doLayout()
            }
        }
    }

    private static def denormalizePath = { item ->
        def param = ~/[A-Z]+:"?([^"),]+)"?/
        def relationships = item.path.collect {it.relationship}.findAll().unique()
        def causal = [
                'increases', 'decreases', 'directlyIncreases',
                'directlyDecreases', 'rateLimitingStepOf'
        ]
        def nodes = item.path.collect {it.label}.findAll()
        def intermediate_terms = nodes.subList(1, nodes.size() - 1)
        def intermediate_entities = intermediate_terms.collect {
            def m = (it =~ param)
            def l = []
            while (m.find()) l << m.group(1)
            l
        }.findAll().flatten()

        def start_entities = []
        def m = (item.start.label =~ param)
        while (m.find()) start_entities << m.group(1)

        def end_entities = []
        m = (item.end.label =~ param)
        while (m.find()) end_entities << m.group(1)

        // static fields
        def description = [
                causal: relationships.every {it in causal},
                start_term: item.start.label,
                end_term: item.end.label,
                intermediate_terms: intermediate_terms,
                start_entity: start_entities,
                end_entity: end_entities,
                intermediate_entities: intermediate_entities,
                length: (int) (item.path.size() / 2),
                relationships: relationships
        ].withDefault {[]}

        // dynamic annotation fields
        item.path.collect { it.evidence*.annotations }.
                flatten().findAll().
                inject([:].withDefault { [] }) { agg, next ->
                    next.each { k, v ->
                        agg[k] << v
                    }
                    agg
                }.each { k, v ->
            v.unique().each {
                description[k] << it
            }
        }
        description
    }
}
