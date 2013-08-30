package model.builder.ui

import org.cytoscape.application.swing.CySwingApplication
import org.cytoscape.io.webservice.WebServiceClient
import org.cytoscape.io.webservice.swing.WebServiceGUIClient
import org.cytoscape.util.swing.OpenBrowser
import org.cytoscape.work.TaskManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.swing.*
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener

import static java.awt.Dialog.ModalityType.APPLICATION_MODAL

class SdpModelImportProvider<T> extends JDialog {

    private static final Logger logger = LoggerFactory.getLogger(SdpModelImportProvider.class);
    private static final String NO_CLIENT = "No Service Client";

    private JButton cancelButton;
    private JComboBox datasourceComboBox;
    private JLabel datasourceLabel;
    private JTabbedPane mainTabbedPane;
    private JPanel propertyPanel;
    private JScrollPane propertyScrollPane;
    private JButton searchButton;
    private JScrollPane searchTermScrollPane;
    private JTextPane queryTextPane;
    private JButton aboutButton;
    private JPanel buttonPanel;
    private JPanel queryPanel;
    private JButton clearButton;
    private JPanel dataQueryPanel;
    private JPanel datasourcePanel;
    private JLabel titleIconLabel;
    private JPanel titlePanel;

    // Registered web service clients
    private Set<WebServiceClient> clients;

    // Client-Dependent GUI panels
    private Map<WebServiceClient, Container> serviceUIPanels = new HashMap<WebServiceClient, Container>();
    private int numClients;

    private final TaskManager<?, ?> taskManager;

    private final Class<T> type;

    private final OpenBrowser openBrowser;

    boolean readyToShow;

    public SdpModelImportProvider(final Class<T> type, final String title, final CySwingApplication cySwingApplicationServiceRef, final TaskManager<?, ?> taskManager, final OpenBrowser openBrowser) {
        super(cySwingApplicationServiceRef.getJFrame(), true);
        if (taskManager == null)
            throw new NullPointerException("TaskManager is null.");

        this.type = type;
        this.taskManager = taskManager;
        this.openBrowser = openBrowser;

        numClients = 0;
        this.clients = new HashSet<WebServiceClient>();

        initGUI();

        datasourceComboBox.addItem(NO_CLIENT);
        setComponentsEnabled(false);

        this.setTitle(title);
    }


    public void addClient(
            final WebServiceClient client, @SuppressWarnings("rawtypes") Map props) {

        if(!typeCheck(client))
            return;

        if(this.numClients == 0)
            this.datasourceComboBox.removeAllItems();

        datasourceComboBox.addItem(client);
        this.clients.add((WebServiceClient) client);
        numClients++;
        setComponentsEnabled(true);

        if (client instanceof WebServiceGUIClient) {
            serviceUIPanels.put((WebServiceClient) client, null);
        }
        if(datasourceComboBox.getModel().getSize() != 0)
            datasourceComboBox.setSelectedIndex(0);
        datasourceComboBoxActionPerformed(null);
        logger.info("New network import client registered: " + client);
    }


    public void removeClient(
            final WebServiceClient client, @SuppressWarnings("rawtypes") Map props) {

        if(!typeCheck(client))
            return;

        datasourceComboBox.removeItem(client);
        this.clients.remove(client);
        serviceUIPanels.remove(client);
        numClients--;

        if(numClients == 0) {
            this.datasourceComboBox.removeAllItems();
            this.datasourceComboBox.addItem(NO_CLIENT);
            setComponentsEnabled(false);
        }
    }


    private boolean typeCheck(final WebServiceClient client) {
        final Class<?>[] interfaces = client.getClass().getInterfaces();
        boolean found = false;
        for(final Class<?> inf: interfaces) {
            if(inf.equals(type)) {
                found = true;
                break;
            }
        }
        return found;
    }


    private void initGUI() {

        initComponents();

        // If we have no data sources, show the install panel
        getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(queryPanel, BorderLayout.CENTER);
        this.pack();

        // Initialize GUI panel.
        datasourceComboBoxActionPerformed(null);
    }

    private void setComponentsEnabled(boolean enable) {
        datasourceComboBox.setEnabled(enable);
        this.searchButton.setEnabled(enable);
        this.aboutButton.setEnabled(enable);
        this.cancelButton.setEnabled(enable);
    }



    private void initComponents() {
        mainTabbedPane = new JTabbedPane();
        searchTermScrollPane = new JScrollPane();
        queryTextPane = new JTextPane();
        propertyPanel = new JPanel();

        queryTextPane.setFont(new Font("SansSerif", 0, 12));
        queryTextPane.setText("Please enter search terms...");
        searchTermScrollPane.setViewportView(queryTextPane);

        mainTabbedPane.addTab("Query", searchTermScrollPane);

        GroupLayout propertyPanelLayout = new GroupLayout(propertyPanel);
        propertyPanel.setLayout(propertyPanelLayout);
        propertyPanelLayout.setHorizontalGroup(propertyPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGap(0, 408, Short.MAX_VALUE));
        propertyPanelLayout.setVerticalGroup(propertyPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGap(0, 303, Short.MAX_VALUE));

        propertyScrollPane = new JScrollPane();
        propertyScrollPane.setViewportView(propertyPanel);
        mainTabbedPane.addTab("Search Property", propertyScrollPane);

        titlePanel = new JPanel();
        titleIconLabel = new JLabel();
        datasourcePanel = new JPanel();
        datasourceLabel = new JLabel();
        datasourceComboBox = new JComboBox();
        datasourceComboBox.setRenderer(new ClientComboBoxCellRenderer());
        aboutButton = new JButton();
        buttonPanel = new JPanel();
        searchButton = new JButton();
        cancelButton = new JButton();
        clearButton = new JButton();
        dataQueryPanel = new JPanel();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        titlePanel.setBackground(new Color(0, 0, 0));

//		titleIconLabel.setIcon(NETWORK_IMPORT_ICON);

        GroupLayout titlePanelLayout = new GroupLayout(titlePanel);
        titlePanel.setLayout(titlePanelLayout);
        titlePanelLayout.setHorizontalGroup(titlePanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(titleIconLabel, GroupLayout.PREFERRED_SIZE, 461, GroupLayout.PREFERRED_SIZE));
        titlePanelLayout.setVerticalGroup(titlePanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(titleIconLabel));

        datasourceLabel.setFont(new Font("SansSerif", 0, 12));
        datasourceLabel.setText("Data Source");

        datasourceComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                datasourceComboBoxActionPerformed(evt);
            }
        });

        aboutButton.setText("About");
        aboutButton.setMargin(new Insets(2, 5, 2, 5));
        aboutButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                aboutButtonActionPerformed(evt);
            }
        });

        GroupLayout datasourcePanelLayout = new GroupLayout(datasourcePanel);
        datasourcePanel.setLayout(datasourcePanelLayout);
        datasourcePanelLayout.setHorizontalGroup(datasourcePanelLayout.createParallelGroup(
                GroupLayout.Alignment.LEADING).addGroup(
                datasourcePanelLayout.createSequentialGroup().addContainerGap().addComponent(datasourceLabel)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(datasourceComboBox, 0, 301, Short.MAX_VALUE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(aboutButton)
                        .addContainerGap()));
        datasourcePanelLayout.setVerticalGroup(datasourcePanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                datasourcePanelLayout
                        .createSequentialGroup()
                        .addContainerGap()
                        .addGroup(
                        datasourcePanelLayout
                                .createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(datasourceLabel)
                                .addComponent(aboutButton)
                                .addComponent(datasourceComboBox, GroupLayout.PREFERRED_SIZE,
                                GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

        buttonPanel.setBorder(BorderFactory.createEtchedBorder());

        searchButton.setText("Search");
        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                searchButtonActionPerformed();
            }
        });

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        clearButton.setText("Clear");
        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                clearButtonActionPerformed(evt);
            }
        });

        GroupLayout buttonPanelLayout = new GroupLayout(buttonPanel);
        buttonPanel.setLayout(buttonPanelLayout);
        buttonPanelLayout.setHorizontalGroup(buttonPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                GroupLayout.Alignment.TRAILING,
                buttonPanelLayout.createSequentialGroup().addContainerGap().addComponent(clearButton)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 225, Short.MAX_VALUE)
                        .addComponent(cancelButton).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(searchButton).addContainerGap()));
        buttonPanelLayout.setVerticalGroup(buttonPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(
                GroupLayout.Alignment.TRAILING,
                buttonPanelLayout
                        .createSequentialGroup()
                        .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(
                        buttonPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(searchButton).addComponent(cancelButton)
                                .addComponent(clearButton)).addContainerGap()));

        GroupLayout dataQueryPanelLayout = new GroupLayout(dataQueryPanel);
        dataQueryPanel.setLayout(dataQueryPanelLayout);
        dataQueryPanelLayout.setHorizontalGroup(dataQueryPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGap(0, 461, Short.MAX_VALUE));
        dataQueryPanelLayout.setVerticalGroup(dataQueryPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGap(0, 247, Short.MAX_VALUE));

        queryPanel = new JPanel();
        GroupLayout layout = new GroupLayout(queryPanel);
        queryPanel.setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(titlePanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(datasourcePanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(buttonPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(dataQueryPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(
                layout.createSequentialGroup()
                        .addComponent(titlePanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                        GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(datasourcePanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                        GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(dataQueryPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE,
                        Short.MAX_VALUE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(buttonPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
                        GroupLayout.PREFERRED_SIZE)));

        dataQueryPanel.setLayout(new BorderLayout());
    }

    public void prepareForDisplay() {
        // Initialize the selected client GUI here so that we can compute the
        // proper initial bounds of the dialog when it's first displayed.
        readyToShow = true;
        datasourceComboBoxActionPerformed(null);
    }

    private void searchButtonActionPerformed() {
        final Object selected = datasourceComboBox.getSelectedItem();
        if (selected == null)
            return;

        WebServiceClient client = null;
        if (selected instanceof WebServiceClient) {
            client = (WebServiceClient) selected;
        } else {
            throw new IllegalStateException("Selected cleint is not a compatible client.");
        }

        // Set query. Just pass the text in the panel.
        taskManager.execute(client.createTaskIterator(this.queryTextPane.getText()));

    }

    /**
     * Clear query text field.
     */
    private void clearButtonActionPerformed(ActionEvent evt) {
        // Just set empty string for the field.
        queryTextPane.setText("");
    }

    private void cancelButtonActionPerformed(ActionEvent evt) {
        // Do nothing. Just hide this window.
        queryTextPane.setText("");
        dispose();
    }

    private void datasourceComboBoxActionPerformed(ActionEvent evt) {
        // This method gets triggered whenever the model changes.  However,
        // we don't want to initialize the GUI until we're actually ready to
        // show the dialog.
        if (!readyToShow) {
            return;
        }

        Object selected = datasourceComboBox.getSelectedItem();
        if (selected == null) {
            selected = datasourceComboBox.getItemAt(0);
            if (selected == null)
                return;
        }

        queryTextPane.setText("");

        if (!(selected instanceof WebServiceClient)) return;

        final WebServiceClient client = (WebServiceClient) selected;

        // Update Panel
        dataQueryPanel.removeAll();

        Container gui = getUIPanel(client);

        if (gui != null) {
            // This service has custom panel.
            dataQueryPanel.add(gui, BorderLayout.CENTER);
            // Hide button panel.
            buttonPanel.setVisible(false);
        } else {
            // Otherwise, use the default panel.
            dataQueryPanel.add(mainTabbedPane, BorderLayout.CENTER);
            buttonPanel.setVisible(true);
        }

        pack();
        repaint();
    }

    private Container getUIPanel(WebServiceClient client) {
        Container container = serviceUIPanels.get(client);
        if (container == null && client instanceof WebServiceGUIClient) {
            container = ((WebServiceGUIClient) client).getQueryBuilderGUI();
            if (container != null) {
                serviceUIPanels.put(client, container);
            }
        }
        return container;
    }


    private void aboutButtonActionPerformed(ActionEvent evt) {

        final WebServiceClient wsc = (WebServiceClient) datasourceComboBox.getSelectedItem();

        final String clientName = wsc.getDisplayName();
        final String description = wsc.getDescription();

        final AboutDialog aboutDialog = new AboutDialog(this, APPLICATION_MODAL, openBrowser);
        aboutDialog.showDialog("About " + clientName, null, description);
    }

    private final class ClientComboBoxCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof WebServiceClient) {
                String displayName = ((WebServiceClient) value).getDisplayName();
                this.setText(displayName);
            }

            return this;

        }
    }
}
