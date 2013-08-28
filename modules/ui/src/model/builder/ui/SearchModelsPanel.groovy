package model.builder.ui

import model.builder.common.Model
import model.builder.web.api.API

import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.RootPaneContainer
import java.awt.Window

import static java.lang.System.currentTimeMillis
import static javax.swing.SwingConstants.RIGHT

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import java.awt.event.KeyListener

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

    private final SearchField name
    private final JCheckBox humanChk
    private final JCheckBox mouseChk
    private final JCheckBox ratChk
    private final SearchResultsModel searchModel
    private final JTable models
    private final JComboBox<String> tagsBox
    private final Timer timer
    private final JButton open
    private final JButton cancel
    private final API client

    SearchModelsPanel(API client) {
        this.client = client

        setSize(650, 400)

        timer = new Timer('Timer')

        layout = new BorderLayout()
        searchModel = new SearchResultsModel()
        models = new JTable(searchModel)

        JPanel namePanel = new JPanel(new BorderLayout(10, 0))
        JLabel nameLbl = new JLabel('Name')
        nameLbl.setHorizontalAlignment(RIGHT)
        nameLbl.setPreferredSize(new Dimension(60, 20))
        name = new SearchField()
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
        open = new JButton('Open')
        open.setEnabled(false)
        buttons.add(open)
        add(buttons, BorderLayout.SOUTH)
        if (parent instanceof RootPaneContainer) parent.rootPane.defaultButton = open

        open.addActionListener(this)
        cancel.addActionListener(this)

        SearchTask task = new SearchTask()
        timer.schedule(task, 250)
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource()
        if (src == cancel)
            if (parent instanceof Window) parent.dispose()
        else if (src == open) {
            // find selected model
            int row = models.getSelectedRow()
        } else if (src == humanChk || src == mouseChk || src == ratChk) {
            timer.schedule(new SearchTask(), 0)
        } else if (src == tagsBox) {
            timer.schedule(new SearchTask(), 0)
        }
    }

    private final class SearchField extends JTextField implements KeyListener {

        private TimerTask task

        SearchField() {
            super()
            addKeyListener(this)
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void keyTyped(KeyEvent e) {
            if (task == null) {
                task = new SearchTask()
                timer.schedule(task, 250)
            } else {
                long last = task.scheduledExecutionTime()
                long now = currentTimeMillis()
                if ((now - last) < 250) task.cancel()
                task = new SearchTask()

                try {
                    timer.schedule(task, 250)
                } catch (IllegalStateException excp) {
                    // already schedules or cancelled; forget about it
                }
            }
        }

        /**
         * {@inheritDoc}
         * <br><br>
         * <strong><em>Unused</em></strong>
         */
        @Override
        public void keyPressed(KeyEvent e) {}

        /**
         * {@inheritDoc}
         * <br><br>
         * <strong><em>Unused</em></strong>
         */
        @Override
        public void keyReleased(KeyEvent e) {}
    }

    private class SearchTask extends TimerTask {

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            name.getText()
            List<String> species = new ArrayList<>()
            if (humanChk.isSelected()) species.add('9606')
            if (mouseChk.isSelected()) species.add('10090')
            if (ratChk.isSelected()) species.add('10116')

            searchModel.setData(new ArrayList<>())
        }
    }

    private final class SearchResultsModel extends AbstractTableModel {

        private final String[] headers = ['Name']
        private final List<Model> models = new ArrayList<>()

        List<Model> getData() {
            return this.models
        }

        void setData(final List<Model> models) {
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
            return models.size()
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getValueAt(int ri, int ci) {
            Model entry = models.get(ri)

            if (ci == 0)
                return entry.name
            return null
        }
    }
}
