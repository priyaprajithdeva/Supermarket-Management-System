package com.mycompany.supermarket;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SuperMarketSystem extends JFrame {

    // ─── DB ───────────────────────────────────────────────────────────────────
    Connection con;

    // ─── Navigation ──────────────────────────────────────────────────────────
    CardLayout card = new CardLayout();
    JPanel mainPanel = new JPanel(card);

    // ─── Colors ───────────────────────────────────────────────────────────────
    static final Color PRIMARY     = new Color(0x1D9E75);
    static final Color PRIMARY_DK  = new Color(0x0F6E56);
    static final Color DANGER      = new Color(0xA32D2D);
    static final Color WARNING     = new Color(0xBA7517);
    static final Color BG          = new Color(0xF8F9FA);
    static final Color SIDEBAR_BG   = new Color(0x1E2A2E);
    static final Color SIDEBAR_TXT  = new Color(0xB0C4BC);
    static final Color SIDEBAR_SEL  = new Color(0x2D3F3B);
    static final Color NAV_ACTIVE_BG = new Color(0x1D9E75);   // bright green fill
    static final Color NAV_HOVER_BG  = new Color(0x263D38);   // subtle hover
    static final Color WHITE       = Color.WHITE;
    static final Color BORDER      = new Color(0xDEE2E6);
    static final Color TEXT_MAIN   = new Color(0x1A1A1A);
    static final Color TEXT_MUTED  = new Color(0x6C757D);
    static final Color LABEL_COLOR = new Color(0x1A1A1A);  // solid dark — visible on all backgrounds

    // ─── Product panel state ──────────────────────────────────────────────────
    JTable productTable;
    DefaultTableModel productModel;
    JTextField pname, pprice, pqty, pcat, psearch;
    JLabel pFormTitle;
    int selectedProductId = -1;

    // ─── Customer panel state ─────────────────────────────────────────────────
    JTable customerTable;
    DefaultTableModel customerModel;
    JTextField cname, cphone, cemail, csearch;
    JLabel cFormTitle;
    int selectedCustomerId = -1;

    // ─── Bill panel state ─────────────────────────────────────────────────────
    JTable billTable, billItemTable;
    DefaultTableModel billModel, billItemModel;
    JComboBox<String> billProductCombo;
    JTextField billQtyField, billDiscField;
    JLabel billTotalLabel;
    double[] billItemPrices = new double[0];
    java.util.List<int[]> billItems = new java.util.ArrayList<>(); // {product_id, qty, price}
    JTextArea receiptArea;

    // ─── Analytics ────────────────────────────────────────────────────────────
    JLabel totalSalesLabel, totalRevenueLabel, totalProductsLabel, totalCustomersLabel;
    JTable topProductsTable;
    DefaultTableModel topProductsModel;

    // ══════════════════════════════════════════════════════════════════════════
    public SuperMarketSystem() {
        setTitle("SuperMarket Pro");
        setSize(1150, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        connectDB();
        buildUI();
        setVisible(true);
    }

    // ── DB ────────────────────────────────────────────────────────────────────
    void connectDB() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/SuperMarketDB?useSSL=false&serverTimezone=UTC",
                "root", "dbms");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "DB connection failed.\n" + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Main Layout ───────────────────────────────────────────────────────────
    void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(mainPanel, BorderLayout.CENTER);
        add(root);

        mainPanel.add(loginPanel(),     "login");
        mainPanel.add(dashboardPanel(), "dashboard");
        mainPanel.add(productPanel(),   "products");
        mainPanel.add(customerPanel(),  "customers");
        mainPanel.add(billingPanel(),   "billing");
        mainPanel.add(analyticsPanel(), "analytics");

        card.show(mainPanel, "login");
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────
    JPanel sidebarPanel;
    JButton[] navButtons;

    JPanel buildSidebar() {
        sidebarPanel = new JPanel();
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setBackground(SIDEBAR_BG);
        sidebarPanel.setPreferredSize(new Dimension(190, 0));
        sidebarPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        // Logo
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 18));
        logoPanel.setBackground(SIDEBAR_BG);
        logoPanel.setMaximumSize(new Dimension(190, 60));
        JLabel logo = new JLabel("SuperMarket Pro");
        logo.setForeground(WHITE);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 15));
        logoPanel.add(logo);
        sidebarPanel.add(logoPanel);

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(0x2D3F3B));
        sep.setMaximumSize(new Dimension(190, 1));
        sidebarPanel.add(sep);
        sidebarPanel.add(Box.createVerticalStrut(8));

        String[][] navItems = {
            {"Dashboard",  "⊞", "dashboard"},
            {"Products",   "▦", "products"},
            {"Customers",  "♟", "customers"},
            {"Billing",    "▤", "billing"},
            {"Analytics",  "◈", "analytics"},
        };

        navButtons = new JButton[navItems.length];
        for (int i = 0; i < navItems.length; i++) {
            final String page = navItems[i][2];
            final int idx = i;
            JButton btn = new JButton(navItems[i][1] + "  " + navItems[i][0]);
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setMaximumSize(new Dimension(190, 40));
            btn.setMinimumSize(new Dimension(190, 40));
            btn.setPreferredSize(new Dimension(190, 40));
            btn.setForeground(SIDEBAR_TXT);
            btn.setBackground(SIDEBAR_BG);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            btn.setBorder(new EmptyBorder(0, 18, 0, 0));
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    if (!btn.getBackground().equals(NAV_ACTIVE_BG))
                        btn.setBackground(NAV_HOVER_BG);
                }
                public void mouseExited(MouseEvent e) {
                    if (!btn.getBackground().equals(NAV_ACTIVE_BG))
                        btn.setBackground(SIDEBAR_BG);
                }
            });
            btn.addActionListener(e -> {
                navigateTo(page, idx);
            });
            navButtons[i] = btn;
            sidebarPanel.add(btn);
        }

        sidebarPanel.add(Box.createVerticalGlue());

        // Logout
        JButton logoutBtn = new JButton("⏻  Logout");
        logoutBtn.setHorizontalAlignment(SwingConstants.LEFT);
        logoutBtn.setMaximumSize(new Dimension(190, 40));
        logoutBtn.setMinimumSize(new Dimension(190, 40));
        logoutBtn.setPreferredSize(new Dimension(190, 40));
        logoutBtn.setForeground(new Color(0xF09595));
        logoutBtn.setBackground(SIDEBAR_BG);
        logoutBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        logoutBtn.setBorder(new EmptyBorder(0, 18, 0, 0));
        logoutBtn.setFocusPainted(false);
        logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> {
            card.show(mainPanel, "login");
            sidebarPanel.setVisible(false);
        });
        sidebarPanel.add(logoutBtn);
        sidebarPanel.add(Box.createVerticalStrut(12));

        sidebarPanel.setVisible(false);
        return sidebarPanel;
    }

    void navigateTo(String page, int idx) {
        for (JButton b : navButtons) {
            b.setBackground(SIDEBAR_BG);
            b.setForeground(SIDEBAR_TXT);
            b.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            b.setBorder(new EmptyBorder(0, 18, 0, 0));
        }
        navButtons[idx].setBackground(NAV_ACTIVE_BG);
        navButtons[idx].setForeground(WHITE);
        navButtons[idx].setFont(new Font("Segoe UI", Font.BOLD, 13));
        navButtons[idx].setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 4, 0, 0, WHITE),
            new EmptyBorder(0, 14, 0, 0)));

        switch (page) {
            case "products":   loadProducts(); break;
            case "customers":  loadCustomers(); break;
            case "billing":    loadBillingProducts(); loadBills(); break;
            case "analytics":  loadAnalytics(); break;
        }
        card.show(mainPanel, page);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // LOGIN
    // ══════════════════════════════════════════════════════════════════════════
    JPanel loginPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(BG);

        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(WHITE);
        box.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            new EmptyBorder(36, 40, 36, 40)
        ));
        box.setMaximumSize(new Dimension(340, 320));

        JLabel title = new JLabel("SuperMarket Pro");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(PRIMARY_DK);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Sign in to continue");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(TEXT_MUTED);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField user = new JTextField();
        user.setMaximumSize(new Dimension(260, 36));
        user.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        user.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            new EmptyBorder(4, 10, 4, 10)));

        JPasswordField pass = new JPasswordField();
        pass.setMaximumSize(new Dimension(260, 36));
        pass.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        pass.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            new EmptyBorder(4, 10, 4, 10)));

        JButton loginBtn = mkBtn("Login", PRIMARY, WHITE);
        loginBtn.setMaximumSize(new Dimension(260, 38));
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        loginBtn.addActionListener(e -> doLogin(user.getText(), new String(pass.getPassword())));
        pass.addActionListener(e -> doLogin(user.getText(), new String(pass.getPassword())));

        box.add(title);
        box.add(Box.createVerticalStrut(4));
        box.add(sub);
        box.add(Box.createVerticalStrut(24));
        box.add(fieldLabel("Username")); box.add(Box.createVerticalStrut(4));
        box.add(user); box.add(Box.createVerticalStrut(12));
        box.add(fieldLabel("Password")); box.add(Box.createVerticalStrut(4));
        box.add(pass); box.add(Box.createVerticalStrut(20));
        box.add(loginBtn);

        outer.add(box);
        return outer;
    }

    void doLogin(String user, String pass) {
        if (con == null) { JOptionPane.showMessageDialog(this, "No DB connection."); return; }
        try {
            PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM admin WHERE username=? AND password=?");
            ps.setString(1, user); ps.setString(2, pass);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                sidebarPanel.setVisible(true);
                navigateTo("dashboard", 0);
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials.", "Login Failed", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DASHBOARD
    // ══════════════════════════════════════════════════════════════════════════
    JPanel dashboardPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(24, 24, 24, 24));

        JLabel title = new JLabel("Dashboard");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        p.add(title, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 2, 16, 16));
        grid.setBackground(BG);
        grid.setBorder(new EmptyBorder(20, 0, 0, 0));

        String[][] cards = {
            {"Products",   "Manage inventory and stock"},
            {"Customers",  "View and manage customers"},
            {"Billing",    "Generate and print invoices"},
            {"Analytics",  "View sales reports"},
        };
        String[] pages = {"products","customers","billing","analytics"};
        int[] navIdx    = {1, 2, 3, 4};

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            JPanel c = new JPanel(new BorderLayout());
            c.setBackground(WHITE);
            c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(20, 20, 20, 20)));
            JLabel name = new JLabel(cards[i][0]);
            name.setFont(new Font("Segoe UI", Font.BOLD, 16));
            JLabel desc = new JLabel(cards[i][1]);
            desc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            desc.setForeground(TEXT_MUTED);
            JButton go = mkBtn("Open →", PRIMARY, WHITE);
            go.addActionListener(e -> navigateTo(pages[idx], navIdx[idx]));
            c.add(name, BorderLayout.NORTH);
            c.add(desc, BorderLayout.CENTER);
            c.add(go, BorderLayout.SOUTH);
            grid.add(c);
        }

        p.add(grid, BorderLayout.CENTER);
        return p;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PRODUCTS  – full CRUD + search + category filter
    // ══════════════════════════════════════════════════════════════════════════
    JPanel productPanel() {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(20, 20, 20, 20));

        // ── Left: table ───────────────────────────────────────────────────────
        JPanel left = new JPanel(new BorderLayout(0, 10));
        left.setBackground(BG);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setBackground(BG);
        psearch = new JTextField(16);
        styleField(psearch);
        psearch.putClientProperty("JTextField.placeholderText", "Search products…");
        JButton searchBtn = mkBtn("Search", PRIMARY, WHITE);
        JButton clearBtn  = mkBtn("Clear",  null, TEXT_MAIN);
        searchBtn.addActionListener(e -> searchProducts(psearch.getText()));
        clearBtn.addActionListener(e -> { psearch.setText(""); loadProducts(); });
        toolbar.add(mkLabel("Products")); 
        toolbar.add(psearch);
        toolbar.add(searchBtn);
        toolbar.add(clearBtn);

        JLabel ptitle = new JLabel("Products");
        ptitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BG);
        topBar.add(ptitle, BorderLayout.WEST);
        topBar.add(toolbar, BorderLayout.EAST);

        productModel = new DefaultTableModel(
            new Object[]{"ID", "Name", "Category", "Price (₹)", "Qty", "Status"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
            public Class<?> getColumnClass(int c) {
                if (c == 0 || c == 4) return Integer.class;
                if (c == 3) return Double.class;
                return String.class;
            }
        };
        productTable = new JTable(productModel);
        styleTable(productTable);
        productTable.getColumnModel().getColumn(0).setMaxWidth(50);
        productTable.getColumnModel().getColumn(4).setMaxWidth(60);
        productTable.getColumnModel().getColumn(5).setMaxWidth(80);

        // Row color for low stock — MUST respect selection state
        productTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (sel) {
                    c.setBackground(new Color(0xB2DFDB));
                    c.setForeground(TEXT_MAIN);
                } else {
                    try {
                        int qty = (int) t.getValueAt(row, 4);
                        c.setBackground(qty < 5 ? new Color(0xFFF3CD) : WHITE);
                    } catch (Exception ex) { c.setBackground(WHITE); }
                    c.setForeground(TEXT_MAIN);
                }
                return c;
            }
        });

        // Use ListSelectionListener — reliable, fires on keyboard + mouse
        productTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) fillProductForm();
        });

        left.add(topBar, BorderLayout.NORTH);
        left.add(new JScrollPane(productTable), BorderLayout.CENTER);

        // ── Right: form ───────────────────────────────────────────────────────
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBackground(WHITE);
        right.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            new EmptyBorder(16, 16, 16, 16)));
        right.setPreferredSize(new Dimension(220, 0));

        pFormTitle = new JLabel("Add Product");
        pFormTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        pFormTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        pname  = styledField(); pprice = styledField();
        pqty   = styledField(); pcat   = styledField();

        JButton addBtn = mkBtn("Add Product",    PRIMARY,  WHITE);
        JButton updBtn = mkBtn("Update",          WARNING,  WHITE);
        JButton delBtn = mkBtn("Delete",          DANGER,   WHITE);
        JButton clrBtn = mkBtn("Clear Form",     null,     TEXT_MAIN);

        for (JButton b : new JButton[]{addBtn, updBtn, delBtn, clrBtn}) {
            b.setMaximumSize(new Dimension(200, 34));
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        addBtn.addActionListener(e -> addProduct());
        updBtn.addActionListener(e -> updateProduct());
        delBtn.addActionListener(e -> deleteProduct());
        clrBtn.addActionListener(e -> clearProductForm());

        right.add(pFormTitle);
        right.add(Box.createVerticalStrut(14));
        right.add(formRow("Name",     pname));
        right.add(formRow("Category", pcat));
        right.add(formRow("Price",    pprice));
        right.add(formRow("Quantity", pqty));
        right.add(Box.createVerticalStrut(12));
        right.add(addBtn); right.add(Box.createVerticalStrut(6));
        right.add(updBtn); right.add(Box.createVerticalStrut(6));
        right.add(delBtn); right.add(Box.createVerticalStrut(6));
        right.add(clrBtn);

        p.add(left,  BorderLayout.CENTER);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    void loadProducts() {
        if (con == null) return;
        try {
            productModel.setRowCount(0);
            ResultSet rs = con.createStatement().executeQuery("SELECT * FROM products ORDER BY id");
            while (rs.next()) {
                int qty = rs.getInt("quantity");
                String status = qty == 0 ? "Out of Stock" : qty < 5 ? "Low Stock" : "In Stock";
                productModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("category"),
                    rs.getDouble("price"),
                    qty,
                    status
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    void searchProducts(String q) {
        if (con == null) return;
        try {
            productModel.setRowCount(0);
            PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM products WHERE name LIKE ? OR category LIKE ?");
            ps.setString(1, "%" + q + "%"); ps.setString(2, "%" + q + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int qty = rs.getInt("quantity");
                String status = qty == 0 ? "Out of Stock" : qty < 5 ? "Low Stock" : "In Stock";
                productModel.addRow(new Object[]{
                    rs.getInt("id"), rs.getString("name"),
                    rs.getString("category"), rs.getDouble("price"),
                    qty, status
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    void fillProductForm() {
        int row = productTable.getSelectedRow();
        if (row == -1) return;
        selectedProductId = (int) productModel.getValueAt(row, 0);
        pname.setText(productModel.getValueAt(row, 1).toString());
        pcat.setText(productModel.getValueAt(row, 2).toString());
        pprice.setText(String.valueOf(productModel.getValueAt(row, 3)));
        pqty.setText(productModel.getValueAt(row, 4).toString());
        pFormTitle.setText("Edit Product #" + selectedProductId);
    }

    void clearProductForm() {
        selectedProductId = -1;
        pname.setText(""); pcat.setText(""); pprice.setText(""); pqty.setText("");
        pFormTitle.setText("Add Product");
        productTable.clearSelection();
    }

    void addProduct() {
        if (!validateFields(pname, pprice, pqty)) return;
        try {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO products(name,category,price,quantity) VALUES(?,?,?,?)");
            ps.setString(1, pname.getText());
            ps.setString(2, pcat.getText());
            ps.setDouble(3, Double.parseDouble(pprice.getText()));
            ps.setInt(4, Integer.parseInt(pqty.getText()));
            ps.executeUpdate();
            toast("Product added successfully.");
            loadProducts(); clearProductForm();
        } catch (Exception e) { e.printStackTrace(); }
    }

    void updateProduct() {
        if (selectedProductId == -1) { JOptionPane.showMessageDialog(this, "Select a product first."); return; }
        if (!validateFields(pname, pprice, pqty)) return;
        try {
            PreparedStatement ps = con.prepareStatement(
                "UPDATE products SET name=?,category=?,price=?,quantity=? WHERE id=?");
            ps.setString(1, pname.getText());
            ps.setString(2, pcat.getText());
            ps.setDouble(3, Double.parseDouble(pprice.getText()));
            ps.setInt(4, Integer.parseInt(pqty.getText()));
            ps.setInt(5, selectedProductId);
            ps.executeUpdate();
            toast("Product updated.");
            loadProducts(); clearProductForm();
        } catch (Exception e) { e.printStackTrace(); }
    }

    void deleteProduct() {
        if (selectedProductId == -1) { JOptionPane.showMessageDialog(this, "Select a product first."); return; }
        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete product #" + selectedProductId + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            PreparedStatement ps = con.prepareStatement("DELETE FROM products WHERE id=?");
            ps.setInt(1, selectedProductId);
            ps.executeUpdate();
            toast("Product deleted.");
            loadProducts(); clearProductForm();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CUSTOMERS – full CRUD + search
    // ══════════════════════════════════════════════════════════════════════════
    JPanel customerPanel() {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(20, 20, 20, 20));

        // ── Left ──────────────────────────────────────────────────────────────
        JPanel left = new JPanel(new BorderLayout(0, 10));
        left.setBackground(BG);

        JLabel ctitle = new JLabel("Customers");
        ctitle.setFont(new Font("Segoe UI", Font.BOLD, 20));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setBackground(BG);
        csearch = new JTextField(16); styleField(csearch);
        csearch.putClientProperty("JTextField.placeholderText", "Search…");
        JButton sb = mkBtn("Search", PRIMARY, WHITE);
        JButton cb = mkBtn("Clear",  null,    TEXT_MAIN);
        sb.addActionListener(e -> searchCustomers(csearch.getText()));
        cb.addActionListener(e -> { csearch.setText(""); loadCustomers(); });
        toolbar.add(csearch); toolbar.add(sb); toolbar.add(cb);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BG);
        topBar.add(ctitle,  BorderLayout.WEST);
        topBar.add(toolbar, BorderLayout.EAST);

        customerModel = new DefaultTableModel(
            new Object[]{"ID", "Name", "Phone", "Email", "Registered"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        customerTable = new JTable(customerModel);
        styleTable(customerTable);
        customerTable.getColumnModel().getColumn(0).setMaxWidth(50);
        customerTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { fillCustomerForm(); }
        });

        left.add(topBar, BorderLayout.NORTH);
        left.add(new JScrollPane(customerTable), BorderLayout.CENTER);

        // ── Right ─────────────────────────────────────────────────────────────
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBackground(WHITE);
        right.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            new EmptyBorder(16, 16, 16, 16)));
        right.setPreferredSize(new Dimension(220, 0));

        cFormTitle = new JLabel("Add Customer");
        cFormTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        cFormTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        cname  = styledField(); cphone = styledField(); cemail = styledField();

        JButton addBtn = mkBtn("Add Customer", PRIMARY, WHITE);
        JButton updBtn = mkBtn("Update",        WARNING, WHITE);
        JButton delBtn = mkBtn("Delete",        DANGER,  WHITE);
        JButton clrBtn = mkBtn("Clear Form",   null,    TEXT_MAIN);

        for (JButton b : new JButton[]{addBtn, updBtn, delBtn, clrBtn}) {
            b.setMaximumSize(new Dimension(200, 34)); b.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        addBtn.addActionListener(e -> addCustomer());
        updBtn.addActionListener(e -> updateCustomer());
        delBtn.addActionListener(e -> deleteCustomer());
        clrBtn.addActionListener(e -> clearCustomerForm());

        right.add(cFormTitle);
        right.add(Box.createVerticalStrut(14));
        right.add(formRow("Name",  cname));
        right.add(formRow("Phone", cphone));
        right.add(formRow("Email", cemail));
        right.add(Box.createVerticalStrut(12));
        right.add(addBtn); right.add(Box.createVerticalStrut(6));
        right.add(updBtn); right.add(Box.createVerticalStrut(6));
        right.add(delBtn); right.add(Box.createVerticalStrut(6));
        right.add(clrBtn);

        p.add(left,  BorderLayout.CENTER);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    void loadCustomers() {
        if (con == null) return;
        try {
            customerModel.setRowCount(0);
            ResultSet rs = con.createStatement().executeQuery(
                "SELECT * FROM customers ORDER BY id");
            while (rs.next()) {
                customerModel.addRow(new Object[]{
                    rs.getInt("id"), rs.getString("name"),
                    rs.getString("phone"), rs.getString("email"),
                    rs.getString("registered_at") != null ? rs.getString("registered_at") : "-"
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    void searchCustomers(String q) {
        if (con == null) return;
        try {
            customerModel.setRowCount(0);
            PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM customers WHERE name LIKE ? OR phone LIKE ? OR email LIKE ?");
            for (int i = 1; i <= 3; i++) ps.setString(i, "%" + q + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                customerModel.addRow(new Object[]{
                    rs.getInt("id"), rs.getString("name"),
                    rs.getString("phone"), rs.getString("email"), "-"
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    void fillCustomerForm() {
        int row = customerTable.getSelectedRow();
        if (row == -1) return;
        selectedCustomerId = (int) customerModel.getValueAt(row, 0);
        cname.setText(customerModel.getValueAt(row, 1).toString());
        cphone.setText(customerModel.getValueAt(row, 2).toString());
        cemail.setText(customerModel.getValueAt(row, 3).toString());
        cFormTitle.setText("Edit Customer #" + selectedCustomerId);
    }

    void clearCustomerForm() {
        selectedCustomerId = -1;
        cname.setText(""); cphone.setText(""); cemail.setText("");
        cFormTitle.setText("Add Customer");
        customerTable.clearSelection();
    }

    void addCustomer() {
        if (cname.getText().trim().isEmpty()) { toast("Name is required."); return; }
        try {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO customers(name,phone,email) VALUES(?,?,?)");
            ps.setString(1, cname.getText());
            ps.setString(2, cphone.getText());
            ps.setString(3, cemail.getText());
            ps.executeUpdate();
            toast("Customer added."); loadCustomers(); clearCustomerForm();
        } catch (Exception e) { e.printStackTrace(); }
    }

    void updateCustomer() {
        if (selectedCustomerId == -1) { JOptionPane.showMessageDialog(this, "Select a customer first."); return; }
        try {
            PreparedStatement ps = con.prepareStatement(
                "UPDATE customers SET name=?,phone=?,email=? WHERE id=?");
            ps.setString(1, cname.getText());
            ps.setString(2, cphone.getText());
            ps.setString(3, cemail.getText());
            ps.setInt(4, selectedCustomerId);
            ps.executeUpdate();
            toast("Customer updated."); loadCustomers(); clearCustomerForm();
        } catch (Exception e) { e.printStackTrace(); }
    }

    void deleteCustomer() {
        if (selectedCustomerId == -1) { JOptionPane.showMessageDialog(this, "Select a customer first."); return; }
        int c = JOptionPane.showConfirmDialog(this, "Delete customer #" + selectedCustomerId + "?",
            "Confirm", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;
        try {
            PreparedStatement ps = con.prepareStatement("DELETE FROM customers WHERE id=?");
            ps.setInt(1, selectedCustomerId);
            ps.executeUpdate();
            toast("Customer deleted."); loadCustomers(); clearCustomerForm();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BILLING – dropdown selects for customer & product, cart, history
    // ══════════════════════════════════════════════════════════════════════════
    JComboBox<String> billCustomerCombo = new JComboBox<>();

    JPanel billingPanel() {
        JPanel p = new JPanel(new BorderLayout(12, 12));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Billing");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));

        // ── Top: new bill form ────────────────────────────────────────────────
        JPanel formCard = new JPanel(new BorderLayout(10, 10));
        formCard.setBackground(WHITE);
        formCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            new EmptyBorder(16, 16, 12, 16)));

        // Row 1: Customer + Product dropdowns side by side
        JPanel row1 = new JPanel(new GridLayout(1, 2, 12, 0));
        row1.setBackground(WHITE);

        JPanel custBox = new JPanel(new BorderLayout(0, 5));
        custBox.setBackground(WHITE);
        custBox.add(mkLabel("Customer"), BorderLayout.NORTH);
        billCustomerCombo = new JComboBox<>();
        billCustomerCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        billCustomerCombo.setPreferredSize(new Dimension(0, 34));
        custBox.add(billCustomerCombo, BorderLayout.CENTER);

        JPanel prodBox = new JPanel(new BorderLayout(0, 5));
        prodBox.setBackground(WHITE);
        prodBox.add(mkLabel("Product"), BorderLayout.NORTH);
        billProductCombo = new JComboBox<>();
        billProductCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        billProductCombo.setPreferredSize(new Dimension(0, 34));
        prodBox.add(billProductCombo, BorderLayout.CENTER);

        row1.add(custBox);
        row1.add(prodBox);

        // Row 2: Qty + Discount + action buttons
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row2.setBackground(WHITE);

        billQtyField  = new JTextField("1", 4); styleField(billQtyField);
        billDiscField = new JTextField("0", 4); styleField(billDiscField);

        JButton addItemBtn   = mkBtn("＋ Add Item", PRIMARY, WHITE);
        JButton genBillBtn   = mkBtn("Generate Bill", new Color(0x185FA5), WHITE);
        JButton clearBillBtn = mkBtn("Clear Cart", null, TEXT_MAIN);
        addItemBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));

        row2.add(mkLabel("Qty:"));    row2.add(billQtyField);
        row2.add(mkLabel("Disc %:")); row2.add(billDiscField);
        row2.add(Box.createHorizontalStrut(8));
        row2.add(addItemBtn);
        row2.add(genBillBtn);
        row2.add(clearBillBtn);

        JPanel topRows = new JPanel();
        topRows.setLayout(new BoxLayout(topRows, BoxLayout.Y_AXIS));
        topRows.setBackground(WHITE);
        topRows.add(row1);
        topRows.add(Box.createVerticalStrut(10));
        topRows.add(row2);

        // Cart table
        billItemModel = new DefaultTableModel(
            new Object[]{"Product", "Qty", "Unit Price", "Total"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        billItemTable = new JTable(billItemModel);
        styleTable(billItemTable);
        JScrollPane cartScroll = new JScrollPane(billItemTable);
        cartScroll.setPreferredSize(new Dimension(0, 130));

        JPanel cartBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        cartBottom.setBackground(WHITE);
        billTotalLabel = new JLabel("Total: ₹0.00");
        billTotalLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        billTotalLabel.setForeground(PRIMARY_DK);

        JButton removeItemBtn = mkBtn("✕ Remove Item", DANGER, WHITE);
        removeItemBtn.addActionListener(e -> removeCartItem());
        cartBottom.add(removeItemBtn);
        cartBottom.add(billTotalLabel);

        addItemBtn.addActionListener(e -> addCartItem());
        genBillBtn.addActionListener(e -> generateBill());
        clearBillBtn.addActionListener(e -> clearCart());

        formCard.add(topRows,    BorderLayout.NORTH);
        formCard.add(cartScroll,  BorderLayout.CENTER);
        formCard.add(cartBottom,  BorderLayout.SOUTH);

        // ── Bottom: bill history ──────────────────────────────────────────────
        JPanel histCard = new JPanel(new BorderLayout(0, 8));
        histCard.setBackground(WHITE);
        histCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            new EmptyBorder(14, 14, 14, 14)));

        JLabel histTitle = new JLabel("Bill History");
        histTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));

        billModel = new DefaultTableModel(
            new Object[]{"Invoice No", "Customer", "Total (₹)", "Discount (₹)", "Final (₹)", "Date"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        billTable = new JTable(billModel);
        styleTable(billTable);
        JScrollPane histScroll = new JScrollPane(billTable);
        histScroll.setPreferredSize(new Dimension(0, 160));

        JPanel histBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        histBtns.setBackground(WHITE);
        JButton viewBtn   = mkBtn("View Receipt", new Color(0x185FA5), WHITE);
        JButton delBillBtn = mkBtn("Delete Bill",  DANGER, WHITE);
        JButton printBtn  = mkBtn("Print",         null,   TEXT_MAIN);
        histBtns.add(viewBtn); histBtns.add(delBillBtn); histBtns.add(printBtn);

        viewBtn.addActionListener(e -> viewReceipt());
        delBillBtn.addActionListener(e -> deleteBill());
        printBtn.addActionListener(e -> printReceipt());

        histCard.add(histTitle,  BorderLayout.NORTH);
        histCard.add(histScroll, BorderLayout.CENTER);
        histCard.add(histBtns,   BorderLayout.SOUTH);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setBackground(BG);
        center.add(title,    BorderLayout.NORTH);
        center.add(formCard, BorderLayout.CENTER);
        center.add(histCard, BorderLayout.SOUTH);

        // Receipt side panel
        receiptArea = new JTextArea();
        receiptArea.setFont(new Font("Courier New", Font.PLAIN, 12));
        receiptArea.setEditable(false);
        receiptArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        JPanel receiptPanel = new JPanel(new BorderLayout());
        receiptPanel.setBackground(WHITE);
        receiptPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            new EmptyBorder(0, 0, 0, 0)));
        receiptPanel.setPreferredSize(new Dimension(240, 0));
        JLabel rLabel = new JLabel("Receipt Preview");
        rLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        rLabel.setBorder(new EmptyBorder(10, 10, 6, 10));
        receiptPanel.add(rLabel, BorderLayout.NORTH);
        receiptPanel.add(new JScrollPane(receiptArea), BorderLayout.CENTER);

        p.add(center,        BorderLayout.CENTER);
        p.add(receiptPanel,  BorderLayout.EAST);
        return p;
    }

    void loadBillingProducts() {
        if (con == null || billProductCombo == null) return;
        try {
            // Load products
            billProductCombo.removeAllItems();
            ResultSet rs = con.createStatement().executeQuery(
                "SELECT id, name, price FROM products WHERE quantity > 0 ORDER BY name");
            while (rs.next()) {
                billProductCombo.addItem(
                    rs.getInt("id") + " | " + rs.getString("name") + " | ₹" + rs.getDouble("price"));
            }
            // Load customers
            billCustomerCombo.removeAllItems();
            billCustomerCombo.addItem("0 | Walk-in Customer");
            rs = con.createStatement().executeQuery(
                "SELECT id, name, phone FROM customers ORDER BY name");
            while (rs.next()) {
                String phone = rs.getString("phone") != null ? " (" + rs.getString("phone") + ")" : "";
                billCustomerCombo.addItem(rs.getInt("id") + " | " + rs.getString("name") + phone);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    void addCartItem() {
        if (billProductCombo.getSelectedItem() == null) return;
        try {
            String sel = billProductCombo.getSelectedItem().toString();
            String[] parts = sel.split("\\|");
            int pid    = Integer.parseInt(parts[0].trim());
            String pnm = parts[1].trim();
            double price = Double.parseDouble(parts[2].trim().replace("₹", ""));
            int qty = Integer.parseInt(billQtyField.getText().trim());

            // Check stock
            PreparedStatement ps = con.prepareStatement("SELECT quantity FROM products WHERE id=?");
            ps.setInt(1, pid);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) < qty) {
                JOptionPane.showMessageDialog(this, "Insufficient stock.");
                return;
            }

            billItems.add(new int[]{pid, qty, (int)(price * 100)});
            billItemModel.addRow(new Object[]{pnm, qty, "₹" + price, "₹" + String.format("%.2f", price * qty)});
            updateCartTotal();
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Invalid input."); }
    }

    void removeCartItem() {
        int row = billItemTable.getSelectedRow();
        if (row == -1) return;
        billItemModel.removeRow(row);
        billItems.remove(row);
        updateCartTotal();
    }

    void updateCartTotal() {
        double total = 0;
        for (int[] item : billItems) total += (item[1] * item[2]) / 100.0;
        double disc = 0;
        try { disc = Double.parseDouble(billDiscField.getText()) / 100.0 * total; } catch (Exception ignored) {}
        billTotalLabel.setText(String.format("Total: ₹%.2f  |  After Discount: ₹%.2f", total, total - disc));
    }

    void generateBill() {
        if (billItems.isEmpty()) { JOptionPane.showMessageDialog(this, "Cart is empty."); return; }
        if (con == null) return;
        try {
            double total = 0;
            for (int[] item : billItems) total += (item[1] * item[2]) / 100.0;
            double discPct = 0;
            try { discPct = Double.parseDouble(billDiscField.getText()); } catch (Exception ignored) {}
            double discount = total * discPct / 100.0;
            double finalAmt = total - discount;

            String invoice = "INV-" + System.currentTimeMillis();
            String date    = new SimpleDateFormat("dd-MM-yyyy HH:mm").format(new Date());
            String customer = "Walk-in";
            if (billCustomerCombo.getSelectedItem() != null) {
                String sel = billCustomerCombo.getSelectedItem().toString();
                String[] parts = sel.split("\\|", 2);
                customer = parts.length > 1 ? parts[1].trim() : "Walk-in";
            }

            // Insert bill
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO bills(invoice_no, customer_name, total, discount, final_amount, bill_date) VALUES(?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, invoice); ps.setString(2, customer);
            ps.setDouble(3, total);   ps.setDouble(4, discount);
            ps.setDouble(5, finalAmt); ps.setString(6, date);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            int billId = keys.next() ? keys.getInt(1) : -1;

            // Insert bill items and update stock
            for (int[] item : billItems) {
                PreparedStatement pi = con.prepareStatement(
                    "INSERT INTO bill_items(bill_id, product_id, quantity, unit_price) VALUES(?,?,?,?)");
                pi.setInt(1, billId); pi.setInt(2, item[0]);
                pi.setInt(3, item[1]); pi.setDouble(4, item[2] / 100.0);
                pi.executeUpdate();

                PreparedStatement pu = con.prepareStatement(
                    "UPDATE products SET quantity = quantity - ? WHERE id=?");
                pu.setInt(1, item[1]); pu.setInt(2, item[0]);
                pu.executeUpdate();
            }

            // Build receipt
            StringBuilder receipt = new StringBuilder();
            receipt.append("================================\n");
            receipt.append("        SUPERMARKET PRO\n");
            receipt.append("================================\n");
            receipt.append("Invoice : ").append(invoice).append("\n");
            receipt.append("Customer: ").append(customer).append("\n");
            receipt.append("Date    : ").append(date).append("\n");
            receipt.append("--------------------------------\n");
            for (int i = 0; i < billItemModel.getRowCount(); i++) {
                receipt.append(String.format("%-14s %2s x %s\n",
                    billItemModel.getValueAt(i, 0).toString().substring(0, Math.min(14, billItemModel.getValueAt(i, 0).toString().length())),
                    billItemModel.getValueAt(i, 1),
                    billItemModel.getValueAt(i, 3)));
            }
            receipt.append("--------------------------------\n");
            receipt.append(String.format("Subtotal : ₹%.2f\n", total));
            receipt.append(String.format("Discount : ₹%.2f (%.0f%%)\n", discount, discPct));
            receipt.append(String.format("TOTAL    : ₹%.2f\n", finalAmt));
            receipt.append("================================\n");
            receipt.append("      Thank you! Come again\n");
            receipt.append("================================\n");

            receiptArea.setText(receipt.toString());
            toast("Bill generated: " + invoice);
            clearCart();
            loadBills();
            loadBillingProducts(); // refresh stock
        } catch (Exception e) { e.printStackTrace(); }
    }

    void clearCart() {
        billItems.clear();
        billItemModel.setRowCount(0);
        billDiscField.setText("0");
        billQtyField.setText("1");
        if (billCustomerCombo.getItemCount() > 0) billCustomerCombo.setSelectedIndex(0);
        if (billProductCombo.getItemCount() > 0) billProductCombo.setSelectedIndex(0);
        billTotalLabel.setText("Total: ₹0.00");
    }

    void loadBills() {
        if (con == null || billModel == null) return;
        try {
            billModel.setRowCount(0);
            ResultSet rs = con.createStatement().executeQuery(
                "SELECT invoice_no, customer_name, total, discount, final_amount, bill_date FROM bills ORDER BY id DESC");
            while (rs.next()) {
                billModel.addRow(new Object[]{
                    rs.getString(1), rs.getString(2),
                    String.format("%.2f", rs.getDouble(3)),
                    String.format("%.2f", rs.getDouble(4)),
                    String.format("%.2f", rs.getDouble(5)),
                    rs.getString(6)
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    void viewReceipt() {
        int row = billTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select a bill."); return; }
        String inv = billModel.getValueAt(row, 0).toString();
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("================================\n");
            sb.append("        SUPERMARKET PRO\n");
            sb.append("================================\n");
            sb.append("Invoice : ").append(inv).append("\n");
            sb.append("Customer: ").append(billModel.getValueAt(row, 1)).append("\n");
            sb.append("Date    : ").append(billModel.getValueAt(row, 5)).append("\n");
            sb.append("--------------------------------\n");

            PreparedStatement ps = con.prepareStatement(
                "SELECT p.name, bi.quantity, bi.unit_price FROM bill_items bi " +
                "JOIN bills b ON bi.bill_id=b.id JOIN products p ON bi.product_id=p.id " +
                "WHERE b.invoice_no=?");
            ps.setString(1, inv);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                double line = rs.getInt(2) * rs.getDouble(3);
                sb.append(String.format("%-14s %2d x ₹%.2f = ₹%.2f\n",
                    rs.getString(1).substring(0, Math.min(14, rs.getString(1).length())),
                    rs.getInt(2), rs.getDouble(3), line));
            }
            sb.append("--------------------------------\n");
            sb.append(String.format("Subtotal : ₹%s\n", billModel.getValueAt(row, 2)));
            sb.append(String.format("Discount : ₹%s\n", billModel.getValueAt(row, 3)));
            sb.append(String.format("TOTAL    : ₹%s\n", billModel.getValueAt(row, 4)));
            sb.append("================================\n");
            receiptArea.setText(sb.toString());
        } catch (Exception e) { e.printStackTrace(); }
    }

    void deleteBill() {
        int row = billTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select a bill."); return; }
        String inv = billModel.getValueAt(row, 0).toString();
        int c = JOptionPane.showConfirmDialog(this, "Delete bill " + inv + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;
        try {
            PreparedStatement ps = con.prepareStatement(
                "DELETE bi FROM bill_items bi JOIN bills b ON bi.bill_id=b.id WHERE b.invoice_no=?");
            ps.setString(1, inv); ps.executeUpdate();

            ps = con.prepareStatement("DELETE FROM bills WHERE invoice_no=?");
            ps.setString(1, inv); ps.executeUpdate();
            toast("Bill deleted."); loadBills();
        } catch (Exception e) { e.printStackTrace(); }
    }

    void printReceipt() {
        try { receiptArea.print(); }
        catch (Exception e) { JOptionPane.showMessageDialog(this, "Print failed: " + e.getMessage()); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ANALYTICS
    // ══════════════════════════════════════════════════════════════════════════
    JPanel analyticsPanel() {
        JPanel p = new JPanel(new BorderLayout(12, 12));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Analytics");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));

        // Stats row
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 12, 0));
        statsRow.setBackground(BG);

        totalSalesLabel    = new JLabel("0");
        totalRevenueLabel  = new JLabel("₹0");
        totalProductsLabel = new JLabel("0");
        totalCustomersLabel = new JLabel("0");

        statsRow.add(statCard("Total Bills",     totalSalesLabel,    PRIMARY_DK));
        statsRow.add(statCard("Total Revenue",   totalRevenueLabel,  new Color(0x185FA5)));
        statsRow.add(statCard("Products",        totalProductsLabel, WARNING));
        statsRow.add(statCard("Customers",       totalCustomersLabel, DANGER));

        // Top products table
        topProductsModel = new DefaultTableModel(
            new Object[]{"Product", "Units Sold", "Revenue (₹)"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        topProductsTable = new JTable(topProductsModel);
        styleTable(topProductsTable);

        JPanel tableCard = new JPanel(new BorderLayout(0, 8));
        tableCard.setBackground(WHITE);
        tableCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            new EmptyBorder(14, 14, 14, 14)));
        JLabel topTitle = new JLabel("Top Selling Products");
        topTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tableCard.add(topTitle, BorderLayout.NORTH);
        tableCard.add(new JScrollPane(topProductsTable), BorderLayout.CENTER);

        JButton refreshBtn = mkBtn("Refresh Analytics", PRIMARY, WHITE);
        refreshBtn.addActionListener(e -> loadAnalytics());

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setBackground(BG);
        center.add(title,      BorderLayout.NORTH);
        center.add(statsRow,   BorderLayout.CENTER);
        center.add(tableCard,  BorderLayout.SOUTH);

        p.add(center, BorderLayout.CENTER);

        JPanel btmBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btmBar.setBackground(BG);
        btmBar.add(refreshBtn);
        p.add(btmBar, BorderLayout.SOUTH);

        return p;
    }

    void loadAnalytics() {
        if (con == null) return;
        try {
            ResultSet rs;

            rs = con.createStatement().executeQuery("SELECT COUNT(*), SUM(final_amount) FROM bills");
            if (rs.next()) {
                totalSalesLabel.setText(String.valueOf(rs.getInt(1)));
                totalRevenueLabel.setText("₹" + String.format("%.2f", rs.getDouble(2)));
            }

            rs = con.createStatement().executeQuery("SELECT COUNT(*) FROM products");
            if (rs.next()) totalProductsLabel.setText(String.valueOf(rs.getInt(1)));

            rs = con.createStatement().executeQuery("SELECT COUNT(*) FROM customers");
            if (rs.next()) totalCustomersLabel.setText(String.valueOf(rs.getInt(1)));

            topProductsModel.setRowCount(0);
            rs = con.createStatement().executeQuery(
                "SELECT p.name, SUM(bi.quantity) AS units, SUM(bi.quantity * bi.unit_price) AS rev " +
                "FROM bill_items bi JOIN products p ON bi.product_id=p.id " +
                "GROUP BY p.id ORDER BY units DESC LIMIT 10");
            while (rs.next()) {
                topProductsModel.addRow(new Object[]{
                    rs.getString(1), rs.getInt(2), String.format("%.2f", rs.getDouble(3))
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    JLabel mkLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(LABEL_COLOR);
        return l;
    }

    JButton mkBtn(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(6, 14, 6, 14));
        if (bg != null) {
            b.setBackground(bg); b.setForeground(fg);
            b.setOpaque(true);
        } else {
            b.setBackground(new Color(0xEEEEEE));
            b.setForeground(fg); b.setOpaque(true);
        }
        return b;
    }

    JTextField styledField() {
        JTextField f = new JTextField();
        f.setMaximumSize(new Dimension(200, 32));
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            new EmptyBorder(4, 8, 4, 8)));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        return f;
    }

    void styleField(JTextField f) {
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            new EmptyBorder(4, 8, 4, 8)));
    }

    void styleTable(JTable t) {
        t.setRowHeight(28);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.getTableHeader().setBackground(BG);
        t.getTableHeader().setForeground(TEXT_MUTED);
        t.setShowVerticalLines(false);
        t.setGridColor(new Color(0xEEEEEE));
        t.setSelectionBackground(new Color(0xE1F5EE));
        t.setSelectionForeground(TEXT_MAIN);
        t.setFillsViewportHeight(true);
    }

    JPanel formRow(String label, JTextField field) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setBackground(WHITE);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(LABEL_COLOR);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(l);
        row.add(Box.createVerticalStrut(3));
        row.add(field);
        row.add(Box.createVerticalStrut(10));
        return row;
    }

    JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(LABEL_COLOR);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    JPanel statCard(String label, JLabel valueLabel, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 3, 0, 0, accent),
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(14, 16, 14, 16))));
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(LABEL_COLOR);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLabel.setForeground(accent);
        card.add(lbl,        BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    boolean validateFields(JTextField... fields) {
        for (JTextField f : fields) {
            if (f.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required.");
                f.requestFocus(); return false;
            }
        }
        return true;
    }
    void toast(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Success", JOptionPane.INFORMATION_MESSAGE);
    }
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(SuperMarketSystem::new);
    }
}