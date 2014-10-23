package model.builder.ui.api

import ca.odell.glazedlists.swing.AdvancedTableModel
import groovy.swing.SwingBuilder
import model.builder.ui.Activator
import model.builder.ui.SearchTableScrollable
import model.builder.web.api.APIManager
import model.builder.web.api.AuthorizedAPI
import model.builder.web.api.SearchProvider
import model.builder.web.api.WebResponse
import org.cytoscape.application.swing.CytoPanelComponent
import org.cytoscape.application.swing.CytoPanelName
import org.jdesktop.swingx.JXTable

import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.JTextField
import javax.swing.event.ListSelectionListener
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout

import static CytoPanelName.WEST
import static model.builder.web.api.Constant.RCR_RESULT_TYPE

public class RCRPanelComponent implements CytoPanelComponent {

    private APIManager apiManager;
    private JPanel panel;
    private Closure onViewDetail;

    RCRPanelComponent(APIManager apiManager, Closure onViewDetail) {
        this.apiManager = apiManager
        this.onViewDetail = onViewDetail

        this.panel = initUI();
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
        SwingBuilder swing = Activator.swing

        def tags = {
            AuthorizedAPI api = apiManager.byAccess(apiManager.default)
            WebResponse res = api.tags([RCR_RESULT_TYPE])
            def facetTags = res.data.facet_counts.facet_fields.tags
            facetTags.collate(2).findAll {it[1] > 0}.collect {it[0]}.sort()
        }

        searchPanel(RCR_RESULT_TYPE, swing, apiManager, tags, onViewDetail)
    }

    static JPanel searchPanel(String type, SwingBuilder swing,
                              APIManager apiManager, Closure loadTags,
                              Closure onViewDetail) {
        swing.panel() {
            def JTextField name
            def JList tags
            def SearchTableScrollable searchTable
            def JButton addButton

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
                    searchTable.table.selectionModel.addListSelectionListener({ evt ->
                        addButton.enabled = true
                    } as ListSelectionListener)
                    panel(constraints: BorderLayout.SOUTH) {
                        flowLayout(alignment: FlowLayout.RIGHT)
                        button(text: 'Search', actionPerformed: {
                            swing.doOutside {
                                if (apiManager.byAccess(apiManager.default)) {
                                    AuthorizedAPI api = apiManager.byAccess(apiManager.default)

                                    def nameSearch = (name.text ?: "").trim()
                                    if (!(nameSearch.startsWith("*") && nameSearch.endsWith("*"))) {
                                        nameSearch = "*$nameSearch*"
                                    }
                                    Map search = [
                                            type: type,
                                            name: nameSearch,
                                            tags: tags.selectedValuesList,
                                            sort: 'name asc'
                                    ]
                                    searchTable.searchProvider = new SearchProvider(api, search)
                                }
                            }
                        })
                        addButton = button(text: 'View Detail', enabled: false, actionPerformed: {
                            JXTable table = (JXTable) searchTable.getTable()
                            def data = ((AdvancedTableModel<Expando>) table.model)
                            def selected = table.selectedRows.collect {
                                int viewIndex ->
                                    def modelIndex = table.convertRowIndexToModel(viewIndex)
                                    data.getElementAt(modelIndex)
                            }

                            swing.doOutside {
                                selected.each { onViewDetail.call(it.id) }
                            }
                        })
                    }
                }
            }

            // load tags outside EDT
            swing.doOutside {
                def tagData = loadTags.call()
                swing.edt {
                    tags.listData = tagData
                }
            }
        }
    }
}