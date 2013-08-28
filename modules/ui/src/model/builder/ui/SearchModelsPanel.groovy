package model.builder.ui

import static javax.swing.SwingConstants.RIGHT

import model.builder.common.Model
import model.builder.web.api.API
import model.builder.web.api.WebResponse

import javax.swing.RootPaneContainer
import java.awt.Window
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener

import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
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
    private final JComboBox<String> tagsBox
    private final JButton open
    private final JButton search
    private final JButton cancel
    private final API client
    private Map currentSearch

    SearchModelsPanel(API client) {
        this.client = client

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
        humanChk.addActionListener(this)
        mouseChk = new JCheckBox('Mouse')
        mouseChk.addActionListener(this)
        ratChk = new JCheckBox('Rat')
        ratChk.addActionListener(this)
        JPanel species = new JPanel(new FlowLayout(FlowLayout.LEFT))
        species.add(humanChk)
        species.add(mouseChk)
        species.add(ratChk)
        speciesPanel.add(speciesLbl, BorderLayout.WEST)
        speciesPanel.add(species, BorderLayout.CENTER)

        // create and populate tags combo box
        JComboBox<String> tcmb = new JComboBox<>(new String[0])
        //String[] modelTags = client.tagsWithModels()
        String[] modelTags = []
        tcmb = new JComboBox<>(modelTags)
        this.tagsBox = tcmb

        JPanel tagsPanel = new JPanel(new BorderLayout(10, 0))
        JLabel tagsLbl = new JLabel('Tags')
        tagsLbl.setHorizontalAlignment(RIGHT)
        tagsLbl.setPreferredSize(new Dimension(60, 20))
        tagsBox.addActionListener(this)
        tagsPanel.add(tagsLbl, BorderLayout.WEST)
        tagsPanel.add(tagsBox, BorderLayout.CENTER)

        JPanel fields = new JPanel(new GridLayout(3, 1))
        fields.setBorder(new TitledBorder('Search Fields'))
        fields.add(namePanel, BorderLayout.CENTER)
        fields.add(speciesPanel, BorderLayout.CENTER)
        fields.add(tagsPanel, BorderLayout.CENTER)
        add(fields, BorderLayout.NORTH)

        JScrollPane scroll = new JScrollPane(models)
        models.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
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
            int[] rows = models.selectedRows
        } else if (src == search) {
            WebResponse res = client.searchModels(
                    currentSearch = [name: "*${name.text}*", rows: 100])
            def solr = res.data.response
            def models = solr.docs.collect {
                new Model(client.uri(it.id), it.name)
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
                    new Model(client.uri(it.id), it.name)
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
