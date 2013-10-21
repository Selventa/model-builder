package model.builder.ui.internal

import ca.odell.glazedlists.BasicEventList
import ca.odell.glazedlists.event.ListEventListener
import ca.odell.glazedlists.gui.TableFormat
import ca.odell.glazedlists.swing.DefaultEventTableModel
import groovy.swing.SwingBuilder
import groovy.transform.TupleConstructor
import model.builder.ui.api.Dialogs
import org.jdesktop.swingx.JXTable
import org.jdesktop.swingx.decorator.HighlightPredicate
import org.jdesktop.swingx.decorator.ToolTipHighlighter

import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.RowSorter.SortKey
import javax.swing.SortOrder
import javax.swing.table.AbstractTableModel
import java.awt.BorderLayout
import java.awt.Color

import static javax.swing.ScrollPaneConstants.*
import static model.builder.common.facet.Functions.*

@TupleConstructor
class DialogsImpl implements Dialogs {

    SwingBuilder swing

    @Override
    JDialog pathFacetSearch(Iterator<Map> itemIterator, Closure denormalizeClosure) {
        def items = [itemIterator.take(1).next()]
        def fieldDescriptions = []

        def columns = ['Start node', 'End node', 'Goes Through', 'Is Causal?']
        def props = ['start_term', 'end_term', 'intermediates', 'causal']

        fieldDescriptions.addAll(describe(items, denormalizeClosure))
        def facets = facet(fieldDescriptions) as Map

        def resultEventList = new BasicEventList()
        resultEventList.addAll(fieldDescriptions)
        def resultsLabel
        def filteredResults = new DefaultEventTableModel(resultEventList,
                [
                        getColumnCount: {columns.size()},
                        getColumnName: {i -> columns[i]},
                        getColumnValue: {o, i -> o."${props[i]}"}
                ] as TableFormat
        )
        resultEventList.addListEventListener({
            listChanged: {
                resultsLabel.text = "Total: ${resultEventList.size()}"
            }
        } as ListEventListener)
        def results = swing.jxTable(model: filteredResults, columnControlVisible: true)

        def facetFieldModels = []
        def createTaskPane = {k, v ->
            def title = k.split('_').collect{it.capitalize()}.join(' ')
            def facetFieldModel = new FacetFieldModel(facets, facets[k], fieldDescriptions, resultEventList, results)
            def tbl
            def pane = swing.taskPane(title: title, animated: false, collapsed: true) {
                scrollPane(border: lineBorder(thickness: 1, color: Color.black),
                        constraints: BorderLayout.CENTER, background: Color.white) {
                    tbl = table(model: facetFieldModel, autoCreateRowSorter: true)
                }
            }
            tbl.rowSorter.sortKeys = [new SortKey(3, SortOrder.DESCENDING)]
            facetFieldModels << facetFieldModel
            pane
        }

        def dialog = swing.dialog(id: 'the_dialog', title: 'Pathfind',
                defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE, modal: false) {

            splitPane(continuousLayout: true, dividerLocation: 400, oneTouchExpandable: true,
                leftComponent: scrollPane(id: 'facetScrollPane', horizontalScrollBarPolicy: HORIZONTAL_SCROLLBAR_NEVER,
                        verticalScrollBarPolicy: VERTICAL_SCROLLBAR_AS_NEEDED, minimumSize: [400, 600]) {
                    taskPaneContainer(id: 'facetContainer') {
                        facets.each(createTaskPane)
                    }
                },
                rightComponent: panel {
                    borderLayout()
                    scrollPane(id: 'resultScrollPane', viewportView: results, constraints: BorderLayout.CENTER)
                    panel(constraints: BorderLayout.SOUTH) {
                        resultsLabel = label()
                    }
                }
            )
        }
        results.addHighlighter(new ToolTipHighlighter(HighlightPredicate.IS_TEXT_TRUNCATED))

        swing.doOutside {
            while (itemIterator.hasNext()) {
                // load in 100 chunk increments
                items = itemIterator.take(100).toList()
                def descriptions = describe(items, denormalizeClosure)

                swing.edt {
                    fieldDescriptions.addAll(descriptions)
                    resultEventList.addAll(descriptions)
                }
                def newFacets = facet(descriptions)
                def newFacetKeys = newFacets.keySet() - facets.keySet()
                mergeFacets([facets, newFacets])
                swing.edt {
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

        dialog.pack()
        dialog.size = [1000, 600]
        dialog.locationRelativeTo = null
        dialog.visible = true
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
                case 0: return 'Include'
                case 1: return 'Exclude'
                case 2: return 'Value'
                case 3: return 'Count'
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
}
