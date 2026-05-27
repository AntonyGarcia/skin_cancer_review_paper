/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.mycompany.processinggui;

/**
 *
 * @author Antony Garcia
 */
public class JFrameWindowClasses extends javax.swing.JFrame {

    /**
     * Creates new form JFrameWindow
     */
    public JFrameWindowClasses() {
        initComponents();
        loadClassNamesIntoList();
        setupContextMenu();
        setupList2ContextMenu();
        setupTextFieldRename();
        setupButtons();
    }

    private void loadClassNamesIntoList() {
        java.util.Set<String> allNames = collectUniqueClassTerms();
        java.nio.file.Path jsonRoot = findJsonRoot();
        if (jsonRoot != null) {
            aliasFilePath = jsonRoot.getParent().resolve("class_aliases.json");
        }
        classFilePaths.clear();
        classFilePaths.putAll(collectClassFilePaths());
        datasetFileCounts.clear();
        for (java.util.Map.Entry<String, java.util.Set<java.nio.file.Path>> entry : classFilePaths.entrySet()) {
            datasetFileCounts.put(entry.getKey(), entry.getValue().size());
        }
        loadOrInitAliasData(allNames);
        refreshList1();
    }

    private void setupContextMenu() {
        jList1.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // Show paper count in parentheses; red for non-compliant, green for clusters with >1 alias
        jList1.setCellRenderer(new javax.swing.DefaultListCellRenderer() {
            private final java.awt.Color ALIAS_GREEN = new java.awt.Color(144, 238, 144);
            private final java.awt.Color NC_RED = new java.awt.Color(255, 160, 160);
            private final java.awt.Color NA_YELLOW = new java.awt.Color(255, 255, 153);
            @Override
            public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                String term = (String) value;
                int count = getTermFileCount(term);
                String display = count > 0 ? term + " (" + count + ")" : term;
                super.getListCellRendererComponent(list, display, index, isSelected, cellHasFocus);
                if (!isSelected) {
                    if (nonCompliantTerms.contains(term)) {
                        setBackground(NC_RED);
                        setOpaque(true);
                    } else if (notAvailableTerms.contains(term)) {
                        setBackground(NA_YELLOW);
                        setOpaque(true);
                    } else {
                        java.util.List<String> aliases = aliasData.get(term);
                        if (aliases != null && aliases.size() > 1) {
                            setBackground(ALIAS_GREEN);
                            setOpaque(true);
                        }
                    }
                }
                return this;
            }
        });

        // Single selection: show term's aliases in jList3 and fill jTextField1
        jList1.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            java.util.List<String> sel = jList1.getSelectedValuesList();
            if (sel.size() == 1) {
                String term = sel.get(0);
                currentSelectedTerm = term;
                jTextField1.setText(term);
                jList3Model.clear();
                java.util.List<String> aliases = aliasData.getOrDefault(term, java.util.Arrays.asList(term));
                for (String alias : aliases) {
                    jList3Model.addElement(alias);
                }
                jButton3.setText(nonCompliantTerms.contains(term) ? "Compliant" : "Non-Compliant");
                jButton7.setText(notAvailableTerms.contains(term) ? "Mark as Available" : "Dataset N/A");
                boolean hasFiles = getTermFileCount(term) > 0;
                boolean singleFile = getTermFileCount(term) == 1;
                jButton4.setEnabled(hasFiles);
                jButton5.setEnabled(singleFile);
                jButton6.setEnabled(true);
                jButton7.setEnabled(true);
            } else {
                jButton4.setEnabled(false);
                jButton5.setEnabled(false);
                jButton6.setEnabled(false);
                jButton7.setEnabled(false);
            }
        });

        javax.swing.JPopupMenu list1Menu = new javax.swing.JPopupMenu();

        javax.swing.JMenuItem copyItem = new javax.swing.JMenuItem("Copy");
        copyItem.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
        copyItem.addActionListener(e -> copyFromList1());

        javax.swing.JMenuItem mergeItem = new javax.swing.JMenuItem("Merge");
        mergeItem.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
        mergeItem.addActionListener(e -> mergeSelectedClasses());

        javax.swing.JMenuItem doiItem = new javax.swing.JMenuItem("Copy DOI");
        doiItem.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
        doiItem.addActionListener(e -> copyDoiToClipboard());

        javax.swing.JMenuItem nonCompliantItem = new javax.swing.JMenuItem("Non-Compliant");
        nonCompliantItem.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
        nonCompliantItem.addActionListener(e -> toggleNonCompliant());

        javax.swing.JMenuItem notAvailableItem = new javax.swing.JMenuItem("Dataset N/A");
        notAvailableItem.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
        notAvailableItem.addActionListener(e -> toggleNotAvailable());

        list1Menu.add(copyItem);
        list1Menu.addSeparator();
        list1Menu.add(mergeItem);
        list1Menu.addSeparator();
        list1Menu.add(doiItem);
        list1Menu.addSeparator();
        list1Menu.add(nonCompliantItem);
        list1Menu.add(notAvailableItem);

        jList1.addMouseListener(new java.awt.event.MouseAdapter() {
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
                int selected = jList1.getSelectedIndices().length;
                if (selected < 1) return;
                copyItem.setEnabled(true);
                mergeItem.setEnabled(selected > 1);
                doiItem.setEnabled(selected == 1 && currentSelectedTerm != null
                        && getTermFileCount(currentSelectedTerm) == 1);
                if (selected == 1 && currentSelectedTerm != null) {
                    nonCompliantItem.setEnabled(true);
                    nonCompliantItem.setText(nonCompliantTerms.contains(currentSelectedTerm)
                            ? "Mark as Compliant" : "Non-Compliant");
                    notAvailableItem.setEnabled(true);
                    notAvailableItem.setText(notAvailableTerms.contains(currentSelectedTerm)
                            ? "Mark as Available" : "Dataset N/A");
                } else {
                    nonCompliantItem.setEnabled(false);
                    notAvailableItem.setEnabled(false);
                }
                list1Menu.show(e.getComponent(), e.getX(), e.getY());
            }
        });
    }

    private void setupList2ContextMenu() {
        jList3Model = new javax.swing.DefaultListModel<>();
        jList3.setModel(jList3Model);
        jList3.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        jList3.setCellRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                String alias = (String) value;
                int count = datasetFileCounts.getOrDefault(alias, 0);
                String display = count > 0 ? alias + " (" + count + ")" : alias;
                return super.getListCellRendererComponent(list, display, index, isSelected, cellHasFocus);
            }
        });

        javax.swing.JPopupMenu list3Menu = new javax.swing.JPopupMenu();
        javax.swing.JMenuItem pasteItem = new javax.swing.JMenuItem("Paste");
        pasteItem.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
        pasteItem.addActionListener(e -> pasteToList2());
        list3Menu.add(pasteItem);

        jList3.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger() && !listClipboard.isEmpty())
                    list3Menu.show(e.getComponent(), e.getX(), e.getY());
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger() && !listClipboard.isEmpty())
                    list3Menu.show(e.getComponent(), e.getX(), e.getY());
            }
        });
    }

    private void copyFromList1() {
        listClipboard.clear();
        listClipboard.addAll(jList1.getSelectedValuesList());
    }

    private void pasteToList2() {
        String currentTerm = jTextField1.getText().trim();
        if (currentTerm.isEmpty() || !aliasData.containsKey(currentTerm)) return;

        java.util.List<String> mainAliases = aliasData.get(currentTerm);
        boolean changed = false;

        for (String item : listClipboard) {
            java.util.List<String> itemAliases = aliasData.getOrDefault(item, java.util.Arrays.asList(item));
            for (String alias : itemAliases) {
                if (!mainAliases.contains(alias)) {
                    mainAliases.add(alias);
                    changed = true;
                }
            }
            if (!item.equals(currentTerm) && aliasData.containsKey(item)) {
                aliasData.remove(item);
                changed = true;
            }
        }

        if (changed) {
            saveAliasData();
            jList3Model.clear();
            for (String alias : mainAliases) {
                jList3Model.addElement(alias);
            }
            refreshList1();
            jList1.setSelectedValue(currentTerm, true);
        }
    }

    private void mergeSelectedClasses() {
        java.util.List<String> selected = jList1.getSelectedValuesList();
        if (selected.size() < 2) return;

        String mainTerm = selected.get(0);
        java.util.List<String> mainAliases = new java.util.ArrayList<>(
                aliasData.getOrDefault(mainTerm, java.util.Arrays.asList(mainTerm)));

        for (int i = 1; i < selected.size(); i++) {
            String other = selected.get(i);
            java.util.List<String> otherAliases = aliasData.getOrDefault(other, java.util.Arrays.asList(other));
            for (String alias : otherAliases) {
                if (!mainAliases.contains(alias)) {
                    mainAliases.add(alias);
                }
            }
            aliasData.remove(other);
        }

        aliasData.put(mainTerm, mainAliases);
        saveAliasData();

        jTextField1.setText(mainTerm);
        jList3Model.clear();
        for (String alias : mainAliases) {
            jList3Model.addElement(alias);
        }

        refreshList1();
        jList1.setSelectedValue(mainTerm, true);
    }

    private java.util.Set<String> collectUniqueClassTerms() {
        java.util.Set<String> terms = new java.util.HashSet<>();
        for (java.nio.file.Path path : listJsonFiles(false)) {
            try {
                String content = java.nio.file.Files.readString(path, java.nio.charset.StandardCharsets.UTF_8);
                terms.addAll(collectClassTerms(new org.json.JSONObject(content)));
            } catch (Exception e) {
                // skip files with parse errors
            }
        }
        return terms;
    }

    private java.util.Map<String, java.util.Set<java.nio.file.Path>> collectClassFilePaths() {
        java.util.Map<String, java.util.Set<java.nio.file.Path>> pathsByTerm = new java.util.HashMap<>();
        for (java.nio.file.Path path : listJsonFiles(false)) {
            try {
                String content = java.nio.file.Files.readString(path, java.nio.charset.StandardCharsets.UTF_8);
                java.util.Set<String> termsInFile = collectClassTerms(new org.json.JSONObject(content));
                for (String term : termsInFile) {
                    pathsByTerm.computeIfAbsent(term, k -> new java.util.HashSet<>()).add(path);
                }
            } catch (Exception e) {
                // skip files with parse errors
            }
        }
        return pathsByTerm;
    }

    private java.util.List<java.nio.file.Path> listJsonFiles(boolean includeNonCompliant) {
        java.nio.file.Path jsonRoot = findJsonRoot();
        if (jsonRoot == null) {
            return java.util.Collections.emptyList();
        }
        try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.walk(jsonRoot)) {
            return stream
                    .filter(p -> p.toString().endsWith(".json"))
                    .filter(p -> includeNonCompliant || !isNonCompliantJson(pathNormalize(p)))
                    .sorted(java.util.Comparator.comparing(java.nio.file.Path::toString))
                    .collect(java.util.stream.Collectors.toList());
        } catch (java.io.IOException e) {
            return java.util.Collections.emptyList();
        }
    }

    private java.nio.file.Path pathNormalize(java.nio.file.Path path) {
        return path.toAbsolutePath().normalize();
    }

    private boolean isNonCompliantJson(java.nio.file.Path path) {
        return path != null
                && path.getParent() != null
                && "non_compliant".equals(path.getParent().getFileName().toString());
    }

    private java.util.Set<String> collectClassTerms(org.json.JSONObject record) {
        java.util.Set<String> terms = new java.util.LinkedHashSet<>();
        collectClassTerms(record, terms);
        return terms;
    }

    private void collectClassTerms(org.json.JSONObject record, java.util.Set<String> terms) {
        Object lesionTypesObj = record.opt("lesion_types_studied");
        if (lesionTypesObj instanceof org.json.JSONArray lesionTypes) {
            for (int i = 0; i < lesionTypes.length(); i++) {
                Object item = lesionTypes.opt(i);
                if (item instanceof org.json.JSONObject obj) {
                    addCleanTerm(terms, cleanTerm(obj.opt("lesion_type")));
                }
            }
        }
    }

    private String cleanTerm(Object value) {
        if (value == null || value == org.json.JSONObject.NULL) return "";
        String term = value.toString().trim();
        return "null".equalsIgnoreCase(term) ? "" : term;
    }

    private void addCleanTerm(java.util.Collection<String> terms, String term) {
        String clean = cleanTerm(term);
        if (!clean.isEmpty() && !terms.contains(clean)) {
            terms.add(clean);
        }
    }

    private java.nio.file.Path findJsonRoot() {
        java.nio.file.Path base = java.nio.file.Paths.get("").toAbsolutePath();
        while (base != null) {
            java.nio.file.Path candidate = base.resolve("json");
            if (java.nio.file.Files.exists(candidate) && java.nio.file.Files.isDirectory(candidate)) {
                return candidate;
            }
            base = base.getParent();
        }
        return null;
    }

    private void loadOrInitAliasData(java.util.Set<String> allNames) {
        aliasData.clear();
        nonCompliantTerms.clear();
        notAvailableTerms.clear();
        boolean fileExists = aliasFilePath != null && java.nio.file.Files.exists(aliasFilePath);
        boolean aliasDataChanged = false;
        if (fileExists) {
            try {
                String content = java.nio.file.Files.readString(aliasFilePath, java.nio.charset.StandardCharsets.UTF_8);
                org.json.JSONArray arr = new org.json.JSONArray(content);
                for (int i = 0; i < arr.length(); i++) {
                    org.json.JSONObject entry = arr.getJSONObject(i);
                    String term = cleanTerm(entry.optString("term", ""));
                    if (!allNames.contains(term)) {
                        aliasDataChanged = true;
                        continue;
                    }
                    org.json.JSONArray aliasArr = entry.getJSONArray("aliases");
                    java.util.List<String> aliases = new java.util.ArrayList<>();
                    for (int j = 0; j < aliasArr.length(); j++) {
                        String alias = cleanTerm(aliasArr.getString(j));
                        if (allNames.contains(alias) && !aliases.contains(alias)) {
                            aliases.add(alias);
                        } else if (!alias.isEmpty()) {
                            aliasDataChanged = true;
                        }
                    }
                    if (!aliases.contains(term)) {
                        aliases.add(0, term);
                        aliasDataChanged = true;
                    }
                    aliasData.put(term, aliases);
                    if (entry.optBoolean("non_compliant", false)) {
                        nonCompliantTerms.add(term);
                    }
                    if (entry.optBoolean("not_available", false)) {
                        notAvailableTerms.add(term);
                    }
                }
            } catch (Exception e) {
                aliasData.clear();
                nonCompliantTerms.clear();
                notAvailableTerms.clear();
                fileExists = false;
            }
        }
        java.util.Set<String> knownAliases = new java.util.HashSet<>();
        for (java.util.List<String> aliases : aliasData.values()) {
            knownAliases.addAll(aliases);
        }
        boolean newNamesAdded = false;
        for (String name : allNames) {
            if (!knownAliases.contains(name)) {
                aliasData.put(name, new java.util.ArrayList<>(java.util.Arrays.asList(name)));
                newNamesAdded = true;
            }
        }
        if (!fileExists || newNamesAdded || aliasDataChanged) {
            saveAliasData();
        }
    }

    private void saveAliasData() {
        if (aliasFilePath == null) return;
        try {
            org.json.JSONArray arr = new org.json.JSONArray();
            java.util.List<String> sortedTerms = new java.util.ArrayList<>(aliasData.keySet());
            sortedTerms.sort(String.CASE_INSENSITIVE_ORDER);
            for (String term : sortedTerms) {
                org.json.JSONObject entry = new org.json.JSONObject();
                entry.put("term", term);
                entry.put("aliases", new org.json.JSONArray(aliasData.get(term)));
                if (nonCompliantTerms.contains(term)) {
                    entry.put("non_compliant", true);
                }
                if (notAvailableTerms.contains(term)) {
                    entry.put("not_available", true);
                }
                arr.put(entry);
            }
            java.nio.file.Files.writeString(aliasFilePath, arr.toString(2), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            // ignore save errors
        }
    }

    private void refreshList1() {
        String filter = jTextFieldSearch.getText().trim().toLowerCase();
        java.util.List<String> allTerms = aliasData.keySet().stream()
                .filter(t -> getTermFileCount(t) > 0)
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        allTerms.sort(String.CASE_INSENSITIVE_ORDER);
        java.util.List<String> terms = filter.isEmpty()
                ? allTerms
                : allTerms.stream().filter(t -> classTermMatchesFilter(t, filter))
                          .collect(java.util.stream.Collectors.toList());
        jList1.setListData(terms.toArray(new String[0]));
        jLabel1.setText(filter.isEmpty()
                ? "Unique datasets: " + terms.size()
                : "Showing: " + terms.size() + " / " + allTerms.size());
        jList1.repaint();
    }

    private boolean classTermMatchesFilter(String term, String filter) {
        if (term.toLowerCase().contains(filter)) return true;
        for (String alias : aliasData.getOrDefault(term, java.util.Collections.emptyList())) {
            if (alias.toLowerCase().contains(filter)) return true;
        }
        return false;
    }

    private void setupButtons() {
        jButton2.addActionListener(e -> breakCluster());
        jButton3.addActionListener(e -> toggleNonCompliant());
        jButton4.addActionListener(e -> openJsonViewer());
        jButton4.setEnabled(false);
        jButton5.setEnabled(false);
        jButton6.setText("Google Search");
        jButton6.setEnabled(false);
        jButton6.addActionListener(e -> openGoogleSearch());
        jButton7.addActionListener(e -> toggleNotAvailable());
        jButton7.setEnabled(false);
        jTextFieldSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { refreshList1(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { refreshList1(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { refreshList1(); }
        });
    }

    private java.util.List<java.nio.file.Path> findMatchingClassFiles(boolean includeNonCompliant) {
        if (currentSelectedTerm == null || !aliasData.containsKey(currentSelectedTerm)) {
            return java.util.Collections.emptyList();
        }
        java.util.Set<String> aliases = new java.util.HashSet<>(aliasData.get(currentSelectedTerm));
        java.util.List<java.nio.file.Path> matchingFiles = new java.util.ArrayList<>();
        for (java.nio.file.Path path : listJsonFiles(includeNonCompliant)) {
            try {
                String content = java.nio.file.Files.readString(path, java.nio.charset.StandardCharsets.UTF_8);
                if (recordHasClassAlias(new org.json.JSONObject(content), aliases)) {
                    matchingFiles.add(path);
                }
            } catch (Exception ignored) {}
        }
        return matchingFiles;
    }

    private boolean recordHasClassAlias(org.json.JSONObject record, java.util.Set<String> aliases) {
        for (String classTerm : collectClassTerms(record)) {
            if (containsIgnoreCase(aliases, classTerm)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsIgnoreCase(java.util.Collection<String> values, String target) {
        for (String value : values) {
            if (value.equalsIgnoreCase(target)) {
                return true;
            }
        }
        return false;
    }

    private String displayJsonPath(java.nio.file.Path path) {
        java.nio.file.Path jsonRoot = findJsonRoot();
        if (jsonRoot != null) {
            try {
                return jsonRoot.relativize(path).toString();
            } catch (Exception ignored) {}
        }
        return path.getFileName().toString();
    }

    private java.nio.file.Path moveJsonToDirectory(
            java.nio.file.Path source,
            java.nio.file.Path destinationDir) throws java.io.IOException {
        java.nio.file.Path dest = destinationDir.resolve(source.getFileName());
        if (java.nio.file.Files.exists(dest)) {
            int dup = 1;
            String stem = source.getFileName().toString().replaceAll("\\.json$", "");
            while (true) {
                java.nio.file.Path candidate = destinationDir.resolve(stem + "__dup" + dup + ".json");
                if (!java.nio.file.Files.exists(candidate)) {
                    dest = candidate;
                    break;
                }
                dup++;
            }
        }
        return java.nio.file.Files.move(source, dest);
    }

    private void openJsonViewer() {
        if (currentSelectedTerm == null || !aliasData.containsKey(currentSelectedTerm)) return;
        java.util.List<java.nio.file.Path> matchingFiles = findMatchingClassFiles(true);

        if (matchingFiles.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "No JSON files found for: " + currentSelectedTerm,
                    "Not Found", javax.swing.JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        java.nio.file.Path chosen;
        if (matchingFiles.size() == 1) {
            chosen = matchingFiles.get(0);
        } else {
            String[] names = matchingFiles.stream()
                    .map(this::displayJsonPath)
                    .toArray(String[]::new);
            String selected = (String) javax.swing.JOptionPane.showInputDialog(
                    this, "Select a file to open:", "Open JSON",
                    javax.swing.JOptionPane.PLAIN_MESSAGE, null, names, names[0]);
            if (selected == null) return;
            chosen = matchingFiles.stream()
                    .filter(p -> displayJsonPath(p).equals(selected))
                    .findFirst().orElse(null);
            if (chosen == null) return;
        }

        try {
            String raw = java.nio.file.Files.readString(chosen, java.nio.charset.StandardCharsets.UTF_8);
            String pretty = new org.json.JSONObject(raw).toString(4);
            showJsonWindow(chosen.getFileName().toString(), pretty, chosen);
        } catch (Exception ex) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Could not read file: " + ex.getMessage(),
                    "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showJsonWindow(String title, String content, java.nio.file.Path filePath) {
        javax.swing.JDialog dialog = new javax.swing.JDialog(this, title, false);
        dialog.setMinimumSize(new java.awt.Dimension(1000, 700));
        dialog.setSize(1200, 800);
        dialog.setLocationRelativeTo(this);

        javax.swing.JTextArea textArea = new javax.swing.JTextArea(content);
        textArea.setEditable(false);
        textArea.setFont(new java.awt.Font("Consolas", java.awt.Font.PLAIN, 13));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(false);
        textArea.setCaretPosition(0);

        // Default highlights: class-related keys in green, class values in yellow
        javax.swing.text.Highlighter hl = textArea.getHighlighter();
        javax.swing.text.Highlighter.HighlightPainter greenPaint =
                new javax.swing.text.DefaultHighlighter.DefaultHighlightPainter(new java.awt.Color(144, 238, 144));
        javax.swing.text.Highlighter.HighlightPainter yellowPaint =
                new javax.swing.text.DefaultHighlighter.DefaultHighlightPainter(new java.awt.Color(255, 230, 80));
        String fullText = textArea.getText();
        String[] keyTokens = {"\"lesion_types_studied\"", "\"lesion_type\""};
        for (String keyToken : keyTokens) {
            int ki = 0;
            while ((ki = fullText.indexOf(keyToken, ki)) != -1) {
                try { hl.addHighlight(ki, ki + keyToken.length(), greenPaint); } catch (Exception ignored) {}
                ki += keyToken.length();
            }
        }
        try {
            org.json.JSONObject rec = new org.json.JSONObject(content);
            for (String name : collectClassTerms(rec)) {
                String quoted = "\"" + name + "\"";
                int ni = 0;
                while ((ni = fullText.indexOf(quoted, ni)) != -1) {
                    try { hl.addHighlight(ni, ni + quoted.length(), yellowPaint); } catch (Exception ignored) {}
                    ni += quoted.length();
                }
            }
        } catch (Exception ignored) {}

        // Context menu with Copy
        javax.swing.JPopupMenu ctxMenu = new javax.swing.JPopupMenu();
        javax.swing.JMenuItem copyItem = new javax.swing.JMenuItem("Copy");
        copyItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_C, java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        copyItem.addActionListener(e -> textArea.copy());
        ctxMenu.add(copyItem);
        textArea.setComponentPopupMenu(ctxMenu);

        // Search bar (hidden by default, shown with Ctrl+F)
        javax.swing.JTextField searchField = new javax.swing.JTextField();
        searchField.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
        searchField.setToolTipText("Find (Enter = next, Shift+Enter = previous, Esc = close)");

        javax.swing.JLabel matchLabel = new javax.swing.JLabel("  ");
        matchLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));

        javax.swing.JButton closeSearch = new javax.swing.JButton("✕");
        closeSearch.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 11));
        closeSearch.setMargin(new java.awt.Insets(1, 4, 1, 4));
        closeSearch.setFocusable(false);

        javax.swing.JPanel searchBar = new javax.swing.JPanel(new java.awt.BorderLayout(4, 0));
        searchBar.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, java.awt.Color.GRAY),
                javax.swing.BorderFactory.createEmptyBorder(3, 6, 3, 6)));
        javax.swing.JPanel searchInner = new javax.swing.JPanel(new java.awt.BorderLayout(4, 0));
        searchInner.add(new javax.swing.JLabel("Find: "), java.awt.BorderLayout.WEST);
        searchInner.add(searchField, java.awt.BorderLayout.CENTER);
        searchInner.add(matchLabel, java.awt.BorderLayout.EAST);
        searchBar.add(searchInner, java.awt.BorderLayout.CENTER);
        searchBar.add(closeSearch, java.awt.BorderLayout.EAST);
        searchBar.setVisible(false);

        // Search logic
        final int[] lastMatchPos = {-1};
        Runnable doSearch = () -> {
            String needle = searchField.getText();
            if (needle.isEmpty()) { matchLabel.setText("  "); return; }
            String haystack = textArea.getText();
            String haystackLc = haystack.toLowerCase();
            String needleLc = needle.toLowerCase();
            int start = lastMatchPos[0] + 1;
            int idx = haystackLc.indexOf(needleLc, start);
            if (idx == -1 && start > 0) idx = haystackLc.indexOf(needleLc, 0); // wrap
            if (idx == -1) {
                matchLabel.setText("not found");
                matchLabel.setForeground(java.awt.Color.RED);
            } else {
                lastMatchPos[0] = idx;
                textArea.setCaretPosition(idx + needle.length());
                textArea.moveCaretPosition(idx);
                matchLabel.setText("  ");
                matchLabel.setForeground(java.awt.Color.DARK_GRAY);
            }
        };
        Runnable doPrevSearch = () -> {
            String needle = searchField.getText();
            if (needle.isEmpty()) { matchLabel.setText("  "); return; }
            String haystack = textArea.getText();
            String haystackLc = haystack.toLowerCase();
            String needleLc = needle.toLowerCase();
            int end = lastMatchPos[0] <= 0 ? haystackLc.length() : lastMatchPos[0];
            int idx = haystackLc.lastIndexOf(needleLc, end - 1);
            if (idx == -1) idx = haystackLc.lastIndexOf(needleLc); // wrap
            if (idx == -1) {
                matchLabel.setText("not found");
                matchLabel.setForeground(java.awt.Color.RED);
            } else {
                lastMatchPos[0] = idx;
                textArea.setCaretPosition(idx + needle.length());
                textArea.moveCaretPosition(idx);
                matchLabel.setText("  ");
                matchLabel.setForeground(java.awt.Color.DARK_GRAY);
            }
        };

        searchField.addActionListener(e -> doSearch.run());
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                    searchBar.setVisible(false);
                    textArea.requestFocusInWindow();
                } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER && e.isShiftDown()) {
                    doPrevSearch.run();
                    e.consume();
                }
            }
        });
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { lastMatchPos[0] = -1; doSearch.run(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { lastMatchPos[0] = -1; doSearch.run(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) {}
        });
        closeSearch.addActionListener(e -> { searchBar.setVisible(false); textArea.requestFocusInWindow(); });

        // Ctrl+F to open search bar
        textArea.getInputMap(javax.swing.JComponent.WHEN_FOCUSED).put(
                javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F,
                        java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "openSearch");
        textArea.getActionMap().put("openSearch", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                searchBar.setVisible(true);
                searchField.requestFocusInWindow();
                searchField.selectAll();
            }
        });

        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(textArea);
        scroll.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Top toolbar
        boolean alreadyNonCompliant = filePath != null
                && filePath.getParent() != null
                && filePath.getParent().getFileName().toString().equals("non_compliant");
        javax.swing.JButton moveBtn = new javax.swing.JButton(
                alreadyNonCompliant ? "Remove from List" : "Move to Non-Compliant");
        moveBtn.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
        moveBtn.setEnabled(filePath != null);
        moveBtn.addActionListener(ev -> {
            String confirmMsg = alreadyNonCompliant
                    ? "Remove datasets of \"" + filePath.getFileName() + "\" from the list?"
                    : "Move \"" + filePath.getFileName() + "\" to non_compliant?";
            int confirm = javax.swing.JOptionPane.showConfirmDialog(dialog,
                    confirmMsg, "Confirm", javax.swing.JOptionPane.YES_NO_OPTION);
            if (confirm != javax.swing.JOptionPane.YES_OPTION) return;
            try {
                if (!alreadyNonCompliant) {
                    java.nio.file.Path ncDir = findJsonRoot();
                    if (ncDir == null) {
                        javax.swing.JOptionPane.showMessageDialog(dialog, "Could not locate json root.",
                                "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    ncDir = ncDir.resolve("non_compliant");
                    java.nio.file.Files.createDirectories(ncDir);
                    moveJsonToDirectory(filePath, ncDir);
                }
                loadClassNamesIntoList();
                dialog.dispose();
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(dialog,
                        "Could not move file: " + ex.getMessage(),
                        "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        });
        javax.swing.JPanel topBar = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 4));
        topBar.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.GRAY));
        topBar.add(moveBtn);

        dialog.setLayout(new java.awt.BorderLayout());
        dialog.add(topBar, java.awt.BorderLayout.NORTH);
        dialog.add(scroll, java.awt.BorderLayout.CENTER);
        dialog.add(searchBar, java.awt.BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void openGoogleSearch() {
        if (currentSelectedTerm == null) return;
        try {
            String query = java.net.URLEncoder.encode(currentSelectedTerm, java.nio.charset.StandardCharsets.UTF_8);
            java.awt.Desktop.getDesktop().browse(new java.net.URI("https://www.google.com/search?q=" + query));
        } catch (Exception ex) {
            javax.swing.JOptionPane.showMessageDialog(this, "Could not open browser: " + ex.getMessage(),
                    "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private int getTermFileCount(String term) {
        java.util.List<String> aliases = aliasData.getOrDefault(term, java.util.Collections.emptyList());
        java.util.Set<java.nio.file.Path> files = new java.util.HashSet<>();
        for (String alias : aliases) {
            java.util.Set<java.nio.file.Path> aliasFiles = classFilePaths.get(alias);
            if (aliasFiles != null) {
                files.addAll(aliasFiles);
            }
        }
        return files.size();
    }

    private void copyDoiToClipboard() {
        if (currentSelectedTerm == null || !aliasData.containsKey(currentSelectedTerm)) return;
        java.util.List<java.nio.file.Path> matches = findMatchingClassFiles(false);
        java.nio.file.Path found = matches.size() == 1 ? matches.get(0) : null;

        if (found == null) {
            javax.swing.JOptionPane.showMessageDialog(this, "No matching file found.",
                    "Copy DOI", javax.swing.JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            String fc = java.nio.file.Files.readString(found, java.nio.charset.StandardCharsets.UTF_8);
            org.json.JSONObject rec = new org.json.JSONObject(fc);
            String doi = rec.optString("doi", rec.optString("DOI", ""));
            if (doi.isEmpty()) {
                javax.swing.JOptionPane.showMessageDialog(this, "No DOI field found in this file.",
                        "Copy DOI", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new java.awt.datatransfer.StringSelection(doi), null);
        } catch (Exception ex) {
            javax.swing.JOptionPane.showMessageDialog(this, "Error reading file: " + ex.getMessage(),
                    "Copy DOI", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private void breakCluster() {
        if (currentSelectedTerm == null || !aliasData.containsKey(currentSelectedTerm)) return;
        java.util.List<String> aliases = aliasData.remove(currentSelectedTerm);
        nonCompliantTerms.remove(currentSelectedTerm);
        notAvailableTerms.remove(currentSelectedTerm);
        for (String alias : aliases) {
            if (!aliasData.containsKey(alias)) {
                aliasData.put(alias, new java.util.ArrayList<>(java.util.Arrays.asList(alias)));
            }
        }
        currentSelectedTerm = null;
        jTextField1.setText("");
        jList3Model.clear();
        saveAliasData();
        refreshList1();
    }

    private void toggleNonCompliant() {
        if (currentSelectedTerm == null || !aliasData.containsKey(currentSelectedTerm)) return;
        if (nonCompliantTerms.contains(currentSelectedTerm)) {
            nonCompliantTerms.remove(currentSelectedTerm);
            jButton3.setText("Non-Compliant");
            saveAliasData();
            refreshList1();
            jList1.setSelectedValue(currentSelectedTerm, true);
        } else {
            moveCurrentClassFilesToNonCompliant();
        }
    }

    private void moveCurrentClassFilesToNonCompliant() {
        java.util.List<java.nio.file.Path> matchingFiles = findMatchingClassFiles(false);
        if (matchingFiles.isEmpty()) {
            nonCompliantTerms.add(currentSelectedTerm);
            jButton3.setText("Compliant");
            saveAliasData();
            refreshList1();
            jList1.setSelectedValue(currentSelectedTerm, true);
            return;
        }

        java.nio.file.Path jsonRoot = findJsonRoot();
        if (jsonRoot == null) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Could not locate json root.",
                    "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            java.nio.file.Path nonCompliantDir = jsonRoot.resolve("non_compliant");
            java.nio.file.Files.createDirectories(nonCompliantDir);
            for (java.nio.file.Path path : matchingFiles) {
                moveJsonToDirectory(path, nonCompliantDir);
            }
            nonCompliantTerms.add(currentSelectedTerm);
            saveAliasData();
            currentSelectedTerm = null;
            jTextField1.setText("");
            jList3Model.clear();
            loadClassNamesIntoList();
        } catch (Exception ex) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Could not move matching JSON files: " + ex.getMessage(),
                    "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private void toggleNotAvailable() {
        if (currentSelectedTerm == null || !aliasData.containsKey(currentSelectedTerm)) return;
        if (notAvailableTerms.contains(currentSelectedTerm)) {
            notAvailableTerms.remove(currentSelectedTerm);
            jButton7.setText("Dataset N/A");
        } else {
            notAvailableTerms.add(currentSelectedTerm);
            jButton7.setText("Mark as Available");
        }
        saveAliasData();
        refreshList1();
        jList1.setSelectedValue(currentSelectedTerm, true);
    }

    private void setupTextFieldRename() {
        jTextField1.addActionListener(e -> renameCurrentTerm());
        jTextField1.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                renameCurrentTerm();
            }
        });
    }

    private void renameCurrentTerm() {
        if (currentSelectedTerm == null) return;
        String newTerm = jTextField1.getText().trim();
        if (newTerm.isEmpty()) {
            jTextField1.setText(currentSelectedTerm);
            return;
        }
        if (newTerm.equals(currentSelectedTerm)) return;
        if (!aliasData.containsKey(currentSelectedTerm)) return;

        java.util.List<String> aliases = aliasData.remove(currentSelectedTerm);
        // The alias list is preserved exactly; the label is independent of its contents.
        if (aliasData.containsKey(newTerm)) {
            // Merge into the existing term with that name
            java.util.List<String> existing = aliasData.get(newTerm);
            for (String a : aliases) {
                if (!existing.contains(a)) existing.add(a);
            }
        } else {
            aliasData.put(newTerm, aliases);
        }
        currentSelectedTerm = newTerm;
        saveAliasData();
        refreshList1();
        jList1.setSelectedValue(newTerm, true);
        jList3Model.clear();
        java.util.List<String> updated = aliasData.getOrDefault(newTerm, java.util.Arrays.asList(newTerm));
        for (String alias : updated) {
            jList3Model.addElement(alias);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList<>();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jList3 = new javax.swing.JList<>();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jTextFieldSearch = new javax.swing.JTextField();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jButton7 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jList1.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane1.setViewportView(jList1);

        jLabel1.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel1.setText("Dataset count: ");

        jLabel2.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel2.setText("Term: ");

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Aliases", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI", 0, 14))); // NOI18N

        jList3.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jScrollPane3.setViewportView(jList3);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 589, Short.MAX_VALUE)
                .addContainerGap())
        );

        jButton1.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jButton1.setText("Save");

        jButton2.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jButton2.setText("Break Cluster");

        jButton3.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jButton3.setText("Non-Compliant");

        jLabel3.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel3.setText("Search: ");

        jTextFieldSearch.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N

        jButton4.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jButton4.setText("Open JSON");

        jButton5.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jButton5.setText("Open PDF");

        jButton6.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jButton6.setText("Search Google");

        jButton7.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jButton7.setText("Dataset N/A");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldSearch)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton5))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 600, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextField1))
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButton1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(jTextFieldSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton4)
                    .addComponent(jButton5)
                    .addComponent(jButton6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jButton1)
                    .addComponent(jButton2)
                    .addComponent(jButton3)
                    .addComponent(jButton7))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Window".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(JFrameWindowClasses.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(JFrameWindowClasses.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(JFrameWindowClasses.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(JFrameWindowClasses.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new JFrameWindowClasses().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JList<String> jList1;
    private javax.swing.JList<String> jList3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextFieldSearch;
    // End of variables declaration//GEN-END:variables

    private final java.util.List<String> listClipboard = new java.util.ArrayList<>();
    private javax.swing.DefaultListModel<String> jList3Model;
    private java.nio.file.Path aliasFilePath;
    private final java.util.LinkedHashMap<String, java.util.List<String>> aliasData = new java.util.LinkedHashMap<>();
    private final java.util.Map<String, Integer> datasetFileCounts = new java.util.HashMap<>();
    private final java.util.Map<String, java.util.Set<java.nio.file.Path>> classFilePaths = new java.util.HashMap<>();
    private final java.util.Set<String> nonCompliantTerms = new java.util.HashSet<>();
    private final java.util.Set<String> notAvailableTerms = new java.util.HashSet<>();
    private String currentSelectedTerm = null;
}
