package model.builder.ui

import ca.odell.glazedlists.BasicEventList
import ca.odell.glazedlists.gui.TableFormat
import ca.odell.glazedlists.swing.AdvancedTableModel
import ca.odell.glazedlists.swing.DefaultEventTableModel
import groovy.swing.SwingBuilder
import model.builder.core.Activator
import model.builder.web.api.AccessInformation
import model.builder.web.api.AuthorizedAPI
import model.builder.web.api.APIManager
import model.builder.web.api.SearchProvider
import model.builder.web.api.WebResponse
import org.jdesktop.swingx.JXList
import org.jdesktop.swingx.JXTable
import org.jdesktop.swingx.JXTaskPane
import org.jdesktop.swingx.JXTaskPaneContainer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JDialog
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JList
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JPasswordField
import javax.swing.JSplitPane
import javax.swing.JTable
import javax.swing.JTextField
import javax.swing.ListSelectionModel
import javax.swing.event.ListSelectionListener
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Window

import static java.awt.GridBagConstraints.*
import static model.builder.ui.MessagePopups.errorAccessNotSet
import static model.builder.web.api.Constant.*

class UI {

    private static final Logger msg = LoggerFactory.getLogger("CyUserMessages")

    static def createSet(List initialItems, Closure onCreate) {
        def swing = Activator.swing

        swing.registerBeanFactory('taskPaneContainer', JXTaskPaneContainer.class)
        swing.registerBeanFactory('taskPane', JXTaskPane.class)
        swing.registerBeanFactory('jxList', JXList.class)
        swing.registerBeanFactory('jxTable', JXTable.class)

        // component setup
        def JTextField name, description
        def JFileChooser fileChooser = new JFileChooser(
                dialogTitle: 'Choose a text file',
                fileSelectionMode: JFileChooser.FILES_ONLY,
                multiSelectionEnabled: true)

        def items = new BasicEventList()
        items.addAll(initialItems)
        def filteredResults = new DefaultEventTableModel(items,
                [
                        getColumnCount: {1},
                        getColumnName: {i -> 'Item'},
                        getColumnValue: {o, i -> o}
                ] as TableFormat
        )
        def itemsTable = swing.jxTable(model: filteredResults, columnControlVisible: true)

        // dialog UI hierarchy
        def dialog = swing.dialog(id: 'the_dialog', title: 'Create Set',
                defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE, modal: true) {

            def dialog = the_dialog
            borderLayout()
            panel(border: titledBorder(title: 'Create Set'),
                    constraints: BorderLayout.NORTH) {

                borderLayout()
                panel(constraints: BorderLayout.CENTER) {
                    gridBagLayout()

                    label(text: 'Name', constraints: gbc(anchor: LINE_START,
                            gridx: 0, gridy: 0,
                            gridwidth: 1, gridheight: 1,
                            weightx: 0.0, weighty: 0.1,
                            insets: [10, 2, 0, 0]))
                    name = textField(constraints: gbc(anchor: LINE_START,
                            gridx: 1, gridy: 0,
                            gridwidth: 1, gridheight: 1,
                            weightx: 0.2, weighty: 0.1,
                            fill: HORIZONTAL, insets: [10, 0, 0, 0]))
                    label(text: 'Description', constraints: gbc(anchor: LINE_START,
                            gridx: 0, gridy: 1,
                            gridwidth: 1, gridheight: 1,
                            weightx: 0.0, weighty: 0.1,
                            insets: [10, 2, 0, 0]))
                    description = textField(constraints: gbc(anchor: LINE_START,
                            gridx: 1, gridy: 1,
                            gridwidth: 1, gridheight: 1,
                            weightx: 0.2, weighty: 0.1,
                            fill: HORIZONTAL, insets: [10, 0, 0, 0]))
                    button(text: 'Choose a file', actionPerformed: {
                        doOutside {
                            int ret = fileChooser.showOpenDialog(dialog)
                            if (ret == JFileChooser.APPROVE_OPTION) {
                                def newItems = fileChooser.selectedFiles.collect { f ->
                                    def lines = []
                                    f.eachLine('utf-8', {
                                        lines.add(it.trim())
                                    })
                                    lines
                                }.flatten().unique().sort()
                                swing.edt {
                                    items.removeAll {true}
                                    items.addAll(newItems)
                                }

                                createButton.enabled = true
                            }
                        }
                    }, constraints: gbc(anchor: LINE_START, gridx: 3, gridy: 1,
                            gridwidth: 1, gridheight: 1, weightx: 0.2, weighty: 0.1,
                            insets: [10, 0, 0, 0]))
                }
            }

            panel(constraints: BorderLayout.CENTER) {

                borderLayout()
                scrollPane(constraints: BorderLayout.CENTER, viewportView: itemsTable)
                panel(constraints: BorderLayout.EAST) {

                    button(icon: Util.icon('/delete.png', 'Delete selection'), actionPerformed: {
                        swing.edt {
                            def deleted = itemsTable.selectedRows.
                                collect(itemsTable.&convertRowIndexToModel).collect {items.get(it)}
                            def remaining = items - deleted
                            items.removeAll {true}
                            items.addAll(remaining)

                            if (!items) createButton.enabled = false
                        }
                    })
                }
            }

            panel(constraints: BorderLayout.SOUTH) {
                flowLayout(alignment: FlowLayout.RIGHT)
                button(text: 'Cancel', preferredSize: [85, 25],
                        actionPerformed: {dialog.dispose()})
                button(id: 'createButton', enabled: !items.empty, text: 'Create', preferredSize: [85, 25],
                    actionPerformed: {
                        swing.doOutside {
                            def ret = onCreate.call(dialog, name.text, description.text, items)
                            swing.edt {
                                if (ret) {
                                    msg.info("Created set ${name.text} (${items.size()} items).")
                                    dialog.dispose()
                                } else {
                                    msg.error("Could not create set.")
                                }
                            }
                        }
                    }
                )
            }
        }
        dialog.pack()
        dialog.size = [700, 400]
        dialog.locationRelativeTo = null
        dialog.visible = true
        dialog
    }

    static def manageSets(APIManager mgr) {
        def swing = Activator.swing
        swing.optionPane()

        swing.registerBeanFactory('taskPaneContainer', JXTaskPaneContainer.class)
        swing.registerBeanFactory('taskPane', JXTaskPane.class)
        swing.registerBeanFactory('jxList', JXList.class)
        swing.registerBeanFactory('jxTable', JXTable.class)

        AuthorizedAPI api = mgr.byAccess(mgr.default)
        if (!api) {
            errorAccessNotSet()
            return
        }

        def elementList = new BasicEventList()
        def elementModel = new DefaultEventTableModel(elementList,
                [
                        getColumnCount: {1},
                        getColumnName: {i -> 'Item'},
                        getColumnValue: {o, i -> o}
                ] as TableFormat
        )

        def items = new BasicEventList()
        def sets = new DefaultEventTableModel(items,
                [
                        getColumnCount: {3},
                        getColumnName: {i -> ['Name', 'Description', 'Count'][i]},
                        getColumnValue: {o, i ->
                            switch(i) {
                                case 0: return o.name
                                case 1: return o.description
                                case 2: return o.elements.size()
                            }
                        }
                ] as TableFormat
        )
        def setsTable = swing.jxTable(model: sets, columnControlVisible: true,
                                      selectionMode: ListSelectionModel.SINGLE_SELECTION)
        setsTable.selectionModel.addListSelectionListener({evt->
            if (evt.valueIsAdjusting) return
            def selected = setsTable.selectedRows.
                    collect(setsTable.&convertRowIndexToModel).collect {items.get(it)}
            if (selected) {
                elementList.removeAll {true}
                elementList.addAll(selected.first().elements)
            }
        } as ListSelectionListener)

        def dialog = swing.dialog(id: 'the_dialog', title: 'Manage Sets',
                defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE) {

            def dialog = the_dialog
            borderLayout()

            panel(constraints: BorderLayout.CENTER) {

                borderLayout()
                splitPane(constraints: BorderLayout.CENTER,
                    topComponent: scrollPane(viewportView: setsTable),
                    bottomComponent: scrollPane(viewportView: table(model: elementModel)),
                    orientation: JSplitPane.VERTICAL_SPLIT, dividerLocation: 200, resizeWeight: 0.7
                )
                panel(constraints: BorderLayout.EAST) {

                    button(icon: Util.icon('/delete.png', 'Delete'), actionPerformed: {
                        def deleted = setsTable.selectedRows.
                                collect(setsTable.&convertRowIndexToModel).collect {items.get(it)}.first()
                        int ret = JOptionPane.showConfirmDialog(the_dialog,
                                "Are you sure you want to delete the '${deleted.name}' set?",
                                'Confirm deletion?', JOptionPane.YES_NO_OPTION)
                        if (ret == JOptionPane.YES_OPTION) {
                            swing.doOutside {
                                WebResponse res = api.deleteSet(new URI(deleted.uri).path)
                                if (res.statusCode == 202) {
                                    msg.info("Deleted the '${deleted.name}' set.")
                                    swing.edt {
                                        items.remove(deleted)
                                    }
                                }
                            }
                        }
                    })
                    button(icon: Util.icon('/add.png', 'Add'), actionPerformed: {
                        createSet([], { createDialog, name, desc, newItems ->
                            swing.doOutside {
                                WebResponse res = api.postSet(name, desc, newItems)
                                if (res.statusCode == 201) {
                                    msg.info("Created set ${name} (${newItems.size()} items).")
                                    res = api.sets()
                                    if (res.statusCode == 200) {
                                        swing.edt {
                                            createDialog.dispose()
                                            items.removeAll {true}
                                            items.addAll(res.data)
                                        }
                                    }
                                    true
                                } else {
                                    msg.error("Could not create set.")
                                    false
                                }
                            }
                        })
                    })
                }
            }

            panel(constraints: BorderLayout.SOUTH) {
                flowLayout(alignment: FlowLayout.RIGHT)
                button(text: 'Close', preferredSize: [85, 25],
                        actionPerformed: {dialog.dispose()})
            }
        }

        swing.doOutside {
            WebResponse res = api.sets()
            if (res.statusCode == 200) {
                swing.edt {
                    items.removeAll {true}
                    items.addAll(res.data)
                }
            }
        }

        dialog.pack()
        dialog.size = [700, 400]
        dialog.locationRelativeTo = null
        dialog.visible = true
        dialog
    }

    static def configuration(APIManager mgr,
                             Closure doAuthenticate, Closure onSave) {
        def swing = Activator.swing
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
                                def hostVal = host.text.toLowerCase(), emailVal = email.text.toLowerCase(), passVal = pass.password as String
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
                                    edt {
                                        MessagePopups.errorConnectionAccess(host.text, email.text, pass.password as String)
                                    }
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
                button(text: 'OK', preferredSize: [85, 25], actionPerformed: {
                    swing.doOutside {
                        configurationsTable.model.with {
                            def rows = rowsModel.value
                            onSave.call(rows as Set)
                        }
                        the_dialog.dispose()
                        msg.info('SDP configuration saved successfully.')
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
        def swing = Activator.swing
        def tags = {
            WebResponse res = api.tags([MODEL_TYPE])
            def facetTags = res.data.facet_counts.facet_fields.tags
            facetTags.collate(2).findAll {it[1] > 0}.collect {it[0]}.sort()
        }

        def dialog = swing.dialog(title: 'Import Model')
        dialog.contentPane.add(modelSearchPanel(swing, api, tags, importData))
        dialog.pack()
        dialog.size = [800, 600]
        dialog.locationRelativeTo = null
        dialog.visible = true
        dialog
    }

    static JDialog addComparison(AuthorizedAPI api, Closure importData) {
        def swing = Activator.swing
        def tags = {
            WebResponse res = api.tags([COMPARISON_TYPE])
            def facetTags = res.data.facet_counts.facet_fields.tags
            facetTags.collate(2).findAll {it[1] > 0}.collect {it[0]}.sort()
        }

        def dialog = swing.dialog(title: 'Add Comparison')
        dialog.contentPane.add(searchPanel(COMPARISON_TYPE, swing, api, tags, importData))
        dialog.pack()
        dialog.size = [800, 600]
        dialog.locationRelativeTo = null
        dialog.visible = true
        dialog
    }

    static JDialog addRcr(AuthorizedAPI api, Closure importData) {
        def swing = Activator.swing

        def tags = {
            WebResponse res = api.tags([RCR_RESULT_TYPE])
            def facetTags = res.data.facet_counts.facet_fields.tags
            facetTags.collate(2).findAll {it[1] > 0}.collect {it[0]}.sort()
        }

        def dialog = swing.dialog(title: 'Add RCR Result')
        dialog.contentPane.add(searchPanel(RCR_RESULT_TYPE, swing, api, tags, importData))
        dialog.pack()
        dialog.size = [800, 600]
        dialog.locationRelativeTo = null
        dialog.visible = true
        dialog
    }

    private static JPanel searchPanel(String type, SwingBuilder swing,
                                      AuthorizedAPI api, Closure tagsClosure,
                                      Closure importClosure) {
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
                            swing.doOutside {
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
                        })
                        addButton = button(text: 'Add', enabled: false, actionPerformed: {
                            JXTable table = (JXTable) searchTable.getTable()
                            def data = ((AdvancedTableModel<Expando>) table.model)
                            def selected = table.selectedRows.collect {
                                int viewIndex ->
                                    def modelIndex = table.convertRowIndexToModel(viewIndex)
                                    data.getElementAt(modelIndex)
                            }

                            swing.doOutside {
                                selected.each { importClosure.call(it.id) }
                            }
                        })
                    }
                }
            }

            // load tags outside EDT
            swing.doOutside {
                List<String> tagData = tagsClosure.call()
                swing.edt {
                    tags.listData = tagData.toArray(new String[tagData.size()])
                }
            }
        }
    }

    private static JPanel modelSearchPanel(SwingBuilder swing, AuthorizedAPI api, Closure tagsClosure,
                                           Closure importClosure) {
        swing.panel() {
            def JTextField name
            def JCheckBox human
            def JCheckBox mouse
            def JCheckBox rat
            def JList<String> tags
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
                                    anchor: FIRST_LINE_START, weightx: 0.05, weighty: 0.05,
                                    insets: [0, 15, 0, 0]))
                    name = textField(constraints: gbc(
                            gridx: 1, gridy: 0, gridwidth: 3, gridheight: 1,
                            anchor: PAGE_START, weightx: 0.8, weighty: 0.05,
                            fill: HORIZONTAL))
                    label(text: 'Species',
                            constraints: gbc(
                                    gridx: 0, gridy: 1, gridwidth: 1, gridheight: 1,
                                    anchor: FIRST_LINE_START, weightx: 0.05, weighty: 0.05,
                                    insets: [0, 15, 0, 0]))
                    human = checkBox(text: 'Human', constraints: gbc(
                            gridx: 1, gridy: 1, gridwidth: 1, gridheight: 1,
                            anchor: PAGE_START, weightx: 0.8, weighty: 0.05,
                            fill: HORIZONTAL))
                    mouse = checkBox(text: 'Mouse', constraints: gbc(
                            gridx: 2, gridy: 1, gridwidth: 1, gridheight: 1,
                            anchor: PAGE_START, weightx: 0.8, weighty: 0.05,
                            fill: HORIZONTAL))
                    rat = checkBox(text: 'Rat', constraints: gbc(
                            gridx: 3, gridy: 1, gridwidth: 1, gridheight: 1,
                            anchor: PAGE_START, weightx: 0.8, weighty: 0.05,
                            fill: HORIZONTAL))
                    label(text: 'Tags', constraints: gbc(
                            gridx: 0, gridy: 2, gridwidth: 1, gridheight: 1,
                            anchor: FIRST_LINE_START, weightx: 0.05, weighty: 0.90,
                            insets: [0, 15, 0, 0]))
                    scrollPane(constraints: gbc(
                            gridx: 1, gridy: 2, gridwidth: 3, gridheight: 1,
                            anchor: PAGE_START, weightx: 0.8, weighty: 0.90,
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
                            swing.doOutside {
                                def nameSearch = (name.text ?: "").trim()
                                if (!(nameSearch.startsWith("*") && nameSearch.endsWith("*"))) {
                                    nameSearch = "*$nameSearch*"
                                }
                                Map modelSearch = [
                                        type: MODEL_TYPE,
                                        name: nameSearch,
                                        tags: tags.selectedValuesList,
                                        sort: 'name asc'
                                ]
                                def species = []
                                if (human.selected) species << '9606'
                                if (mouse.selected) species << '10090'
                                if (rat.selected) species << '10116'
                                if (!species.empty) modelSearch['species'] = species
                                searchTable.searchProvider = new SearchProvider(api, modelSearch)
                            }
                        })
                        addButton = button(text: 'Import', enabled: false, actionPerformed: {
                            JXTable table = (JXTable) searchTable.getTable()
                            def data = ((AdvancedTableModel<Expando>) table.model)
                            def selected = table.selectedRows.collect {
                                int viewIndex ->
                                    def modelIndex = table.convertRowIndexToModel(viewIndex)
                                    data.getElementAt(modelIndex)
                            }

                            swing.doOutside {
                                importClosure.call(selected)
                            }
                        })
                    }
                }
            }

            // load tags outside EDT
            swing.doOutside {
                List<String> tagData = tagsClosure.call()
                swing.edt {
                    tags.listData = tagData.toArray(new String[tagData.size()])
                }
            }
        }
    }
}
