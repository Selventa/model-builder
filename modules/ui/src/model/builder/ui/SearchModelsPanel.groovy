package model.builder.ui

import org.cytoscape.io.webservice.WebServiceClient
import org.cytoscape.work.TaskManager

import javax.swing.JList
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets

import static javax.swing.SwingConstants.RIGHT

import model.builder.common.Model
import model.builder.web.api.API
import model.builder.web.api.WebResponse

import javax.swing.RootPaneContainer
import java.awt.Window
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener

import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.JTextField
import javax.swing.ListSelectionModel
import javax.swing.border.TitledBorder
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import javax.swing.table.AbstractTableModel

final class SearchModelsPanel extends JPanel implements ActionListener {

    private final JTextField name
    private final JCheckBox humanChk
    private final JCheckBox mouseChk
    private final JCheckBox ratChk
    private final SearchResultsModel searchModel
    private final JTable models
    private final JList<String> tagsBox
    private final JButton open
    private final JButton search
    private final JButton cancel
    private final API client
    private final WebServiceClient cyWsClient
    private final TaskManager taskMgr
    private Map currentSearch

    SearchModelsPanel(API client, WebServiceClient cyWsClient, TaskManager taskMgr) {
        this.client = client
        this.cyWsClient = cyWsClient
        this.taskMgr = taskMgr

        setSize(650, 400)

        layout = new BorderLayout()
        searchModel = new SearchResultsModel()
        models = new JTable(searchModel)

        JPanel namePanel = new JPanel(new BorderLayout(10, 0))
        JLabel nameLbl = new JLabel('Name')
        nameLbl.setHorizontalAlignment(RIGHT)
        nameLbl.setPreferredSize(new Dimension(60, 20))
        name = new JTextField()
        namePanel.add(nameLbl, BorderLayout.WEST)
        namePanel.add(name, BorderLayout.CENTER)

        JPanel speciesPanel = new JPanel(new BorderLayout(10, 0))
        JLabel speciesLbl = new JLabel('Species')
        speciesLbl.setHorizontalAlignment(RIGHT)
        speciesLbl.setPreferredSize(new Dimension(60, 20))
        humanChk = new JCheckBox('Human')
        mouseChk = new JCheckBox('Mouse')
        ratChk = new JCheckBox('Rat')
        JPanel species = new JPanel(new FlowLayout(FlowLayout.LEFT))
        species.add(humanChk)
        species.add(mouseChk)
        species.add(ratChk)
        speciesPanel.add(speciesLbl, BorderLayout.WEST)
        speciesPanel.add(species, BorderLayout.CENTER)

        // create and populate tags combo box
        WebResponse res = client.modelTags()
        def facetTags = res.data.facet_counts.facet_fields.tags
        String[] tags = facetTags.collate(2).findAll {it[1] > 0}.collect {it[0]}.sort()
        JScrollPane tagsScroll = new JScrollPane(this.tagsBox = new JList<>(tags))
        this.tagsBox.visibleRowCount = 5

        JPanel tagsPanel = new JPanel(new BorderLayout(10, 0))
        JLabel tagsLbl = new JLabel('Tags')
        tagsLbl.setHorizontalAlignment(RIGHT)
        tagsLbl.setPreferredSize(new Dimension(60, 20))
        tagsPanel.add(tagsLbl, BorderLayout.WEST)
        tagsPanel.add(tagsScroll, BorderLayout.CENTER)

        JPanel fields = new JPanel(new GridBagLayout())
        fields.setBorder(new TitledBorder('Search Fields'))
        fields.add(namePanel, new GridBagConstraints(0,0,1,1,1,0.25, GridBagConstraints.PAGE_START, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0))
        fields.add(speciesPanel, new GridBagConstraints(0,1,1,1,1,0.25, GridBagConstraints.PAGE_START, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0))
        fields.add(tagsPanel, new GridBagConstraints(0,2,1,1,1,0.50, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0))
        add(fields, BorderLayout.NORTH)

        JScrollPane scroll = new JScrollPane(models)
        models.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
        models.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                boolean empty = models.getSelectionModel().isSelectionEmpty()
                open.setEnabled(!empty)
            }
        })
        add(scroll, BorderLayout.CENTER)

        JPanel buttons = new JPanel()
        buttons.setLayout(new FlowLayout(FlowLayout.RIGHT))
        cancel = new JButton('Cancel')
        buttons.add(cancel)
        search = new JButton('Search')
        buttons.add(search)
        open = new JButton('Open')
        open.setEnabled(false)
        buttons.add(open)
        add(buttons, BorderLayout.SOUTH)
        if (parent instanceof RootPaneContainer) parent.rootPane.defaultButton = open

        open.addActionListener(this)
        search.addActionListener(this)
        cancel.addActionListener(this)
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource()
        if (src == cancel) {
            def p = parent
            while (p != null) {
                if (p instanceof Window) {
                    p.dispose()
                    p = null
                } else {
                    p = p.parent
                }
            }
        } else if (src == open) {
            taskMgr.execute(cyWsClient.createTaskIterator(
                models.selectedRows.
                    collect {models.convertRowIndexToModel(it)}.
                    collect {searchModel.data[it]}))
        } else if (src == search) {
            def species = []
            if (humanChk.selected) species << '9606'
            if (mouseChk.selected) species << '10090'
            if (ratChk.selected) species << '10116'
            WebResponse res = client.searchModels(
                    currentSearch = [
                            name: name.text ? "*${name.text}*" : "",
                            species: species,
                            tags: tagsBox.selectedValuesList as String[],
                            rows: 100])

            def solr = res.data.response
            def models = solr.docs.collect {
                new Model(client.id(it.id), it.name)
            }.sort {it.name}
            searchModel.setData(solr.numFound, models)
        }
    }

    private final class SearchResultsModel extends AbstractTableModel {

        private final String[] headers = ['Name']
        private final List<Model> models = new ArrayList<>()
        private int total

        List<Model> getData() {
            return this.models
        }

        void setData(final int total, final List<Model> models) {
            this.total = total
            this.models.clear()
            this.models.addAll(models)
            fireTableDataChanged()
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getColumnCount() {
            return headers.length
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getColumnName(int ci) {
            return headers[ci]
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getRowCount() {
            return total
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getValueAt(int ri, int ci) {
            Model entry
            try {
                entry = models.get(ri)
            } catch (IndexOutOfBoundsException) {
                WebResponse res = client.searchModels(currentSearch + [start: models.size()])
                def solr = res.data.response
                def models = solr.docs.collect {
                    new Model(client.id(it.id), it.name)
                }
                models = this.models + models
                models.sort {it.name}
                searchModel.setData(solr.numFound, models)
                if (ri > models.size()) return null
                entry = models.get(ri)
            }
            if (ci == 0)
                return entry.name
            return null
        }
    }
}
