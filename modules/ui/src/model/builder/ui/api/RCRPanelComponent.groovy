package model.builder.ui.api

import ca.odell.glazedlists.swing.AdvancedTableModel
import groovy.swing.SwingBuilder
import model.builder.ui.Activator
import model.builder.ui.SearchTableScrollable
import model.builder.web.api.APIManager
import model.builder.web.api.AccessInformation
import model.builder.web.api.AuthorizedAPI
import model.builder.web.api.SearchProvider
import model.builder.web.api.WebResponse
import model.builder.web.api.event.SetDefaultAccessInformationEvent
import model.builder.web.api.event.SetDefaultAccessInformationListener
import org.cytoscape.application.swing.CytoPanelComponent
import org.cytoscape.application.swing.CytoPanelName
import org.jdesktop.swingx.JXTable

import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.JTextField
import javax.swing.ListSelectionModel
import javax.swing.event.ListSelectionListener
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout

import static CytoPanelName.WEST
import static model.builder.web.api.Constant.RCR_RESULT_TYPE

public class RCRPanelComponent implements CytoPanelComponent, SetDefaultAccessInformationListener {

    private SwingBuilder          swing
    private AuthorizedAPI         api
    private JPanel                panel
    private JList                 tags
    private SearchTableScrollable searchTable
    private Closure               onViewDetail
    private Closure               onPaintScores

    RCRPanelComponent(AuthorizedAPI api, Closure onViewDetail, Closure onPaintScores) {
        this.swing = Activator.swing
        this.api = api
        this.onViewDetail  = onViewDetail
        this.onPaintScores = onPaintScores
        this.panel = initUI()
    }

    @Override
    void handleEvent(SetDefaultAccessInformationEvent event) {
        APIManager mgr = event.source
        AccessInformation newDefault = event.payloadCollection[1]
        if (mgr && newDefault) {
            api = mgr.byAccess(newDefault)
            searchTable.clearRows()
            loadTags()
        }
    }

    @Override
    Component getComponent() {
        return panel
    }

    @Override
    CytoPanelName getCytoPanelName() {
        return WEST
    }

    @Override
    String getTitle() {
        return 'RCR'
    }

    @Override
    Icon getIcon() {
        return null
    }

    private JPanel initUI() {
        swing.panel() {
            def JTextField name
            def JButton viewButton, paintButton

            borderLayout()
            splitPane(orientation: JSplitPane.VERTICAL_SPLIT,
                    dividerLocation: 200,
                    constraints: BorderLayout.CENTER) {
                panel {
                    gridBagLayout()
                    label(text: 'Name',
                            constraints: gbc(
                                    gridx: 0, gridy: 0, gridwidth: 1, gridheight: 1,
                                    anchor: FIRST_LINE_START, weightx: 0.05, weighty: 0.0,
                                    insets: [0, 15, 0, 0]))
                    name = textField(constraints: gbc(
                            gridx: 1, gridy: 0, gridwidth: 1, gridheight: 1,
                            anchor: PAGE_START, weightx: 0.8, weighty: 0.0,
                            fill: HORIZONTAL))
                    label(text: 'Tags', constraints: gbc(
                            gridx: 0, gridy: 1, gridwidth: 1, gridheight: 1,
                            anchor: FIRST_LINE_START, weightx: 0.05, weighty: 0.90,
                            insets: [0, 15, 0, 0]))
                    scrollPane(constraints: gbc(
                            gridx: 1, gridy: 1, gridwidth: 1, gridheight: 1,
                            anchor: FIRST_LINE_START, weightx: 0.8, weighty: 0.90,
                            fill: BOTH)) {
                        tags = list(items: [])
                    }
                }
                panel {
                    borderLayout()
                    searchTable = searchTableScrollable(constraints: BorderLayout.CENTER)
                    searchTable.table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
                    searchTable.table.selectionModel.addListSelectionListener({ evt ->
                        viewButton.enabled = true
                        paintButton.enabled = true
                    } as ListSelectionListener)
                    panel(constraints: BorderLayout.SOUTH) {
                        flowLayout(alignment: FlowLayout.RIGHT)
                        button(text: 'Search', actionPerformed: {
                            swing.doOutside {
                                if (api) {
                                    def nameSearch = (name.text ?: "").trim()
                                    if (!(nameSearch.startsWith("*") && nameSearch.endsWith("*"))) {
                                        nameSearch = "*$nameSearch*"
                                    }
                                    Map search = [
                                            type: RCR_RESULT_TYPE,
                                            name: nameSearch,
                                            tags: tags.selectedValuesList,
                                            sort: 'name asc'
                                    ]
                                    searchTable.searchProvider = new SearchProvider(api, search)
                                }
                            }
                        })
                        viewButton = button(text: 'View', enabled: false, actionPerformed: {
                            JXTable table = (JXTable) searchTable.getTable()
                            def data = ((AdvancedTableModel<Expando>) table.model)
                            def selected = table.selectedRows.collect {
                                int viewIndex ->
                                    def modelIndex = table.convertRowIndexToModel(viewIndex)
                                    data.getElementAt(modelIndex)
                            }

                            swing.doOutside {
                                onViewDetail.call(api, selected.collect { it.id })
                            }
                        })
                        paintButton = button(text: 'Paint Scores', enabled: false, actionPerformed: {
                            JXTable table = (JXTable) searchTable.getTable()
                            def data = ((AdvancedTableModel<Expando>) table.model)
                            def selected = table.selectedRows.collect {
                                int viewIndex ->
                                    def modelIndex = table.convertRowIndexToModel(viewIndex)
                                    data.getElementAt(modelIndex)
                            }

                            swing.doOutside {
                                onPaintScores.call(api, selected.collect { it.id })
                            }
                        })
                    }
                }
            }

            loadTags()
        }
    }

    private void loadTags() {
        // load tags outside EDT
        swing.doOutside {
            WebResponse res = api.tags([RCR_RESULT_TYPE])
            def facetTags = res.data.facet_counts.facet_fields.tags
            def tagData = facetTags.collate(2).findAll {it[1] > 0}.collect {it[0]}.sort()
            swing.edt {
                tags.listData = tagData
            }
        }
    }
}