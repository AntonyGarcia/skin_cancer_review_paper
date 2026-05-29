package com.mycompany.processinggui;

public class JFrameWindowDatasets extends javax.swing.JFrame {

    private final javax.swing.DefaultListModel<JsonFileItem> jsonListModel = new javax.swing.DefaultListModel<>();
    private final javax.swing.DefaultListModel<String> datasetListModel = new javax.swing.DefaultListModel<>();
    private final javax.swing.JList<JsonFileItem> jsonFileList = new javax.swing.JList<>(jsonListModel);
    private final javax.swing.JList<String> datasetList = new javax.swing.JList<>(datasetListModel);
    private final javax.swing.JLabel statusLabel = new javax.swing.JLabel(" ");
    private final javax.swing.JButton refreshButton = new javax.swing.JButton("Refresh");
    private final javax.swing.JButton openJsonButton = new javax.swing.JButton("Open JSON");

    public JFrameWindowDatasets() {
        initComponents();
        loadCompliantJsonFiles();
        setupSelectionListener();
        setupContextMenu();
        setupButtons();
    }

    private void initComponents() {
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Dataset Transparency Review");
        setMinimumSize(new java.awt.Dimension(1100, 650));

        jsonFileList.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        datasetList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        javax.swing.JScrollPane leftScroll = new javax.swing.JScrollPane(jsonFileList);
        javax.swing.JScrollPane rightScroll = new javax.swing.JScrollPane(datasetList);

        javax.swing.JPanel leftPanel = new javax.swing.JPanel(new java.awt.BorderLayout(4, 4));
        leftPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Compliant JSON files"));
        leftPanel.add(leftScroll, java.awt.BorderLayout.CENTER);

        javax.swing.JPanel rightPanel = new javax.swing.JPanel(new java.awt.BorderLayout(4, 4));
        rightPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Datasets claimed in selected paper"));
        rightPanel.add(rightScroll, java.awt.BorderLayout.CENTER);

        javax.swing.JSplitPane splitPane = new javax.swing.JSplitPane(
                javax.swing.JSplitPane.HORIZONTAL_SPLIT,
                leftPanel,
                rightPanel);
        splitPane.setResizeWeight(0.55);

        javax.swing.JPanel topBar = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 4));
        topBar.add(refreshButton);
        topBar.add(openJsonButton);

        javax.swing.JPanel bottomBar = new javax.swing.JPanel(new java.awt.BorderLayout());
        bottomBar.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 6, 3, 6));
        bottomBar.add(statusLabel, java.awt.BorderLayout.CENTER);

        getContentPane().setLayout(new java.awt.BorderLayout(6, 6));
        getContentPane().add(topBar, java.awt.BorderLayout.NORTH);
        getContentPane().add(splitPane, java.awt.BorderLayout.CENTER);
        getContentPane().add(bottomBar, java.awt.BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    private void setupSelectionListener() {
        jsonFileList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            refreshDatasetListForSelection();
        });
    }

    private void setupContextMenu() {
        javax.swing.JPopupMenu menu = new javax.swing.JPopupMenu();

        javax.swing.JMenuItem nonCompliantItem = new javax.swing.JMenuItem("Non-Compliant");
        nonCompliantItem.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
        nonCompliantItem.addActionListener(e -> moveSelectedJsonFilesToNonCompliant());

        javax.swing.JMenuItem openItem = new javax.swing.JMenuItem("Open JSON");
        openItem.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
        openItem.addActionListener(e -> openSelectedJson());

        menu.add(openItem);
        menu.addSeparator();
        menu.add(nonCompliantItem);

        jsonFileList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(java.awt.event.MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                int idx = jsonFileList.locationToIndex(e.getPoint());
                if (idx >= 0 && !jsonFileList.isSelectedIndex(idx)) {
                    jsonFileList.setSelectedIndex(idx);
                }

                boolean hasSelection = !jsonFileList.getSelectedValuesList().isEmpty();
                openItem.setEnabled(jsonFileList.getSelectedValuesList().size() == 1);
                nonCompliantItem.setEnabled(hasSelection);
                if (hasSelection) {
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    private void setupButtons() {
        refreshButton.addActionListener(e -> loadCompliantJsonFiles());
        openJsonButton.addActionListener(e -> openSelectedJson());
        openJsonButton.setEnabled(false);
    }

    private void loadCompliantJsonFiles() {
        jsonListModel.clear();
        datasetListModel.clear();

        java.nio.file.Path compliantDir = findCompliantDir();
        if (compliantDir == null) {
            statusLabel.setText("Could not locate json/compliant.");
            openJsonButton.setEnabled(false);
            return;
        }

        try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.walk(compliantDir)) {
            stream
                    .filter(p -> p.toString().endsWith(".json"))
                    .sorted(java.util.Comparator.comparing(p -> p.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .forEach(p -> jsonListModel.addElement(new JsonFileItem(p)));
            updateStatus();
        } catch (java.io.IOException ex) {
            statusLabel.setText("Could not read compliant JSON files: " + ex.getMessage());
        }

        openJsonButton.setEnabled(false);
    }

    private void refreshDatasetListForSelection() {
        datasetListModel.clear();

        java.util.List<JsonFileItem> selected = jsonFileList.getSelectedValuesList();
        openJsonButton.setEnabled(selected.size() == 1);
        if (selected.isEmpty()) {
            updateStatus();
            return;
        }

        java.util.Set<String> datasetNames = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (JsonFileItem item : selected) {
            datasetNames.addAll(readDatasetNames(item.path));
        }

        for (String datasetName : datasetNames) {
            datasetListModel.addElement(datasetName);
        }

        if (datasetListModel.isEmpty()) {
            datasetListModel.addElement("(No datasets_used names found)");
        }

        statusLabel.setText("Selected JSON files: " + selected.size()
                + " | Dataset names shown: " + datasetNames.size()
                + " | Compliant JSON files: " + jsonListModel.size());
    }

    private java.util.List<String> readDatasetNames(java.nio.file.Path jsonPath) {
        java.util.List<String> names = new java.util.ArrayList<>();
        try {
            String content = java.nio.file.Files.readString(jsonPath, java.nio.charset.StandardCharsets.UTF_8);
            org.json.JSONObject record = new org.json.JSONObject(content);
            Object datasetsObj = record.opt("datasets_used");
            if (!(datasetsObj instanceof org.json.JSONArray datasets)) {
                return names;
            }

            for (int i = 0; i < datasets.length(); i++) {
                Object item = datasets.opt(i);
                String name;
                if (item instanceof org.json.JSONObject obj) {
                    name = cleanTerm(obj.opt("name"));
                } else {
                    name = cleanTerm(item);
                }
                if (!name.isEmpty() && !names.contains(name)) {
                    names.add(name);
                }
            }
        } catch (Exception ignored) {
            // Keep the review list usable even if one JSON file cannot be parsed.
        }
        return names;
    }

    private void moveSelectedJsonFilesToNonCompliant() {
        java.util.List<JsonFileItem> selected = jsonFileList.getSelectedValuesList();
        if (selected.isEmpty()) return;

        int confirm = javax.swing.JOptionPane.showConfirmDialog(this,
                "Move " + selected.size() + " JSON file(s) to non_compliant?",
                "Non-Compliant",
                javax.swing.JOptionPane.YES_NO_OPTION,
                javax.swing.JOptionPane.WARNING_MESSAGE);
        if (confirm != javax.swing.JOptionPane.YES_OPTION) return;

        java.nio.file.Path jsonRoot = findJsonRoot();
        if (jsonRoot == null) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Could not locate json root.",
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            return;
        }

        java.nio.file.Path nonCompliantDir = jsonRoot.resolve("non_compliant");
        int moved = 0;
        java.util.List<String> errors = new java.util.ArrayList<>();

        try {
            java.nio.file.Files.createDirectories(nonCompliantDir);
            for (JsonFileItem item : selected) {
                try {
                    moveJsonToDirectory(item.path, nonCompliantDir);
                    moved++;
                } catch (Exception ex) {
                    errors.add(item.path.getFileName() + ": " + ex.getMessage());
                }
            }
        } catch (java.io.IOException ex) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Could not create non_compliant folder: " + ex.getMessage(),
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            return;
        }

        loadCompliantJsonFiles();
        if (!errors.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Moved " + moved + " file(s), but " + errors.size() + " failed:\n"
                            + String.join("\n", errors.subList(0, Math.min(errors.size(), 10))),
                    "Move Complete With Errors",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
        } else {
            statusLabel.setText("Moved " + moved + " file(s) to non_compliant. Compliant JSON files: "
                    + jsonListModel.size());
        }
    }

    private java.nio.file.Path moveJsonToDirectory(
            java.nio.file.Path source,
            java.nio.file.Path destinationDir) throws java.io.IOException {
        java.nio.file.Path destination = destinationDir.resolve(source.getFileName());
        if (java.nio.file.Files.exists(destination)) {
            int duplicateNumber = 1;
            String stem = source.getFileName().toString().replaceAll("\\.json$", "");
            while (true) {
                java.nio.file.Path candidate = destinationDir.resolve(stem + "__dup" + duplicateNumber + ".json");
                if (!java.nio.file.Files.exists(candidate)) {
                    destination = candidate;
                    break;
                }
                duplicateNumber++;
            }
        }
        return java.nio.file.Files.move(source, destination);
    }

    private void openSelectedJson() {
        java.util.List<JsonFileItem> selected = jsonFileList.getSelectedValuesList();
        if (selected.size() != 1) return;

        java.nio.file.Path path = selected.get(0).path;
        try {
            String raw = java.nio.file.Files.readString(path, java.nio.charset.StandardCharsets.UTF_8);
            String pretty = new org.json.JSONObject(raw).toString(4);
            showJsonWindow(path.getFileName().toString(), pretty);
        } catch (Exception ex) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Could not open JSON: " + ex.getMessage(),
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showJsonWindow(String title, String content) {
        javax.swing.JDialog dialog = new javax.swing.JDialog(this, title, false);
        dialog.setMinimumSize(new java.awt.Dimension(900, 650));
        dialog.setSize(1100, 750);
        dialog.setLocationRelativeTo(this);

        javax.swing.JTextArea textArea = new javax.swing.JTextArea(content);
        textArea.setEditable(false);
        textArea.setFont(new java.awt.Font("Consolas", java.awt.Font.PLAIN, 13));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(false);
        textArea.setCaretPosition(0);
        textArea.setComponentPopupMenu(createCopyMenu(textArea));

        dialog.add(new javax.swing.JScrollPane(textArea), java.awt.BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private javax.swing.JPopupMenu createCopyMenu(javax.swing.JTextArea textArea) {
        javax.swing.JPopupMenu menu = new javax.swing.JPopupMenu();
        javax.swing.JMenuItem copyItem = new javax.swing.JMenuItem("Copy");
        copyItem.addActionListener(e -> textArea.copy());
        menu.add(copyItem);
        return menu;
    }

    private void updateStatus() {
        statusLabel.setText("Compliant JSON files: " + jsonListModel.size());
    }

    private java.nio.file.Path findCompliantDir() {
        java.nio.file.Path jsonRoot = findJsonRoot();
        if (jsonRoot == null) return null;

        java.nio.file.Path compliantDir = jsonRoot.resolve("compliant");
        if (!java.nio.file.Files.isDirectory(compliantDir)) return null;
        return compliantDir;
    }

    private java.nio.file.Path findJsonRoot() {
        java.nio.file.Path base = java.nio.file.Paths.get("").toAbsolutePath();
        while (base != null) {
            java.nio.file.Path candidate = base.resolve("json");
            if (java.nio.file.Files.isDirectory(candidate)) {
                return candidate;
            }
            base = base.getParent();
        }
        return null;
    }

    private String cleanTerm(Object value) {
        if (value == null || value == org.json.JSONObject.NULL) return "";
        String text = value.toString().trim();
        return "null".equalsIgnoreCase(text) ? "" : text;
    }

    public static void main(String args[]) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Window".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(JFrameWindowDatasets.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(JFrameWindowDatasets.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(JFrameWindowDatasets.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(JFrameWindowDatasets.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        java.awt.EventQueue.invokeLater(() -> new JFrameWindowDatasets().setVisible(true));
    }

    private static class JsonFileItem {
        private final java.nio.file.Path path;

        private JsonFileItem(java.nio.file.Path path) {
            this.path = path;
        }

        @Override
        public String toString() {
            return path.getFileName().toString();
        }
    }
}
