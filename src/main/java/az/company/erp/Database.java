package az.company.erp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Local PostgreSQL connection and one-time schema setup for the desktop application. */
public final class Database {
    private static final Path CONFIG_FILE = Path.of("config", "database.properties");
    private static Boolean useSqlite = null;

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (Throwable ignore) { }
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (Throwable ignore) { }
    }

    private Database() { }

    public static boolean isSqlite() {
        if (useSqlite == null) {
            try (Connection conn = connect()) { } catch (Exception ignore) { }
        }
        return useSqlite != null && useSqlite;
    }

    public static boolean isConfigured() {
        return true;
    }

    public static Connection connect() throws IOException, SQLException {
        if (CONFIG_FILE.toFile().exists()) {
            try {
                Properties settings = settings();
                String url = settings.getProperty("db.url", "");
                String user = settings.getProperty("db.user", "");
                String pass = settings.getProperty("db.password", "");
                if (!url.isBlank() && url.contains("postgresql")) {
                    DriverManager.setLoginTimeout(2);
                    Connection conn = DriverManager.getConnection(url, user, pass);
                    useSqlite = false;
                    return conn;
                }
            } catch (Exception ignore) { }
        }
        useSqlite = true;
        Files.createDirectories(Path.of("data"));
        return DriverManager.getConnection("jdbc:sqlite:data/biznes_idareetme.db");
    }

    private static String pk() {
        return isSqlite() ? "INTEGER PRIMARY KEY AUTOINCREMENT" : "BIGSERIAL PRIMARY KEY";
    }

    public static String monthExpr(String col) {
        return "SUBSTR(CAST(" + col + " AS VARCHAR), 1, 7)";
    }

    public static void initializeSchema() throws IOException, SQLException {
        String pk = pk();
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS managers (id " + pk + ", code VARCHAR(40) UNIQUE NOT NULL, name VARCHAR(150) NOT NULL, commission_rate NUMERIC(8,2) NOT NULL DEFAULT 0, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS customers (id " + pk + ", code VARCHAR(40) UNIQUE NOT NULL, name VARCHAR(200) NOT NULL, phone VARCHAR(50), opening_debt NUMERIC(14,2) NOT NULL DEFAULT 0, manager_id BIGINT REFERENCES managers(id) ON DELETE SET NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS suppliers (id " + pk + ", code VARCHAR(40) UNIQUE NOT NULL, name VARCHAR(200) NOT NULL, phone VARCHAR(50), opening_debt NUMERIC(14,2) NOT NULL DEFAULT 0, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS raw_materials (id " + pk + ", code VARCHAR(40) UNIQUE NOT NULL, name VARCHAR(200) NOT NULL, unit VARCHAR(30) NOT NULL, quantity NUMERIC(14,3) NOT NULL DEFAULT 0, unit_cost NUMERIC(14,4) NOT NULL DEFAULT 0, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS products (id " + pk + ", code VARCHAR(40) UNIQUE NOT NULL, name VARCHAR(200) NOT NULL, quantity NUMERIC(14,3) NOT NULL DEFAULT 0, delivery_price NUMERIC(14,2) NOT NULL DEFAULT 0, unit_cost NUMERIC(14,4) NOT NULL DEFAULT 0, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS product_recipes (product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE, raw_material_id BIGINT NOT NULL REFERENCES raw_materials(id) ON DELETE CASCADE, quantity_per_product NUMERIC(14,3) NOT NULL, PRIMARY KEY (product_id, raw_material_id))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS stock_movements (id " + pk + ", material_id BIGINT REFERENCES raw_materials(id) ON DELETE SET NULL, product_id BIGINT REFERENCES products(id) ON DELETE SET NULL, movement_type VARCHAR(40) NOT NULL, quantity_change NUMERIC(14,3) NOT NULL, unit_cost NUMERIC(14,4), note TEXT, occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS productions (id " + pk + ", product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE, quantity NUMERIC(14,3) NOT NULL, unit_cost NUMERIC(14,4) NOT NULL, occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS deliveries (id " + pk + ", document_no VARCHAR(40) UNIQUE NOT NULL, customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE, manager_id BIGINT REFERENCES managers(id) ON DELETE SET NULL, total NUMERIC(14,2) NOT NULL, paid_amount NUMERIC(14,2) NOT NULL DEFAULT 0, cost NUMERIC(14,2) NOT NULL DEFAULT 0, occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS delivery_items (id " + pk + ", delivery_id BIGINT NOT NULL REFERENCES deliveries(id) ON DELETE CASCADE, product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE, quantity NUMERIC(14,3) NOT NULL, unit_price NUMERIC(14,2) NOT NULL, unit_cost NUMERIC(14,4) NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS payments (id " + pk + ", payment_type VARCHAR(40) NOT NULL, customer_id BIGINT REFERENCES customers(id) ON DELETE SET NULL, supplier_id BIGINT REFERENCES suppliers(id) ON DELETE SET NULL, manager_id BIGINT REFERENCES managers(id) ON DELETE SET NULL, amount NUMERIC(14,2) NOT NULL, occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS wastes (id " + pk + ", raw_material_id BIGINT NOT NULL REFERENCES raw_materials(id) ON DELETE CASCADE, quantity NUMERIC(14,3) NOT NULL, cost NUMERIC(14,2) NOT NULL, reason TEXT NOT NULL, occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS activity_logs (id " + pk + ", category VARCHAR(40) NOT NULL, description TEXT NOT NULL, occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS cash_movements (id " + pk + ", direction VARCHAR(10) NOT NULL, amount NUMERIC(14,2) NOT NULL, description TEXT NOT NULL, occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS purchases (id " + pk + ", document_no VARCHAR(40) UNIQUE, supplier_id BIGINT REFERENCES suppliers(id) ON DELETE SET NULL, raw_material_id BIGINT REFERENCES raw_materials(id) ON DELETE SET NULL, quantity NUMERIC(14,3) NOT NULL, total_amount NUMERIC(14,2) NOT NULL, paid_amount NUMERIC(14,2) NOT NULL DEFAULT 0, debt_amount NUMERIC(14,2) NOT NULL DEFAULT 0, occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS employees (id " + pk + ", code VARCHAR(40) UNIQUE NOT NULL, name VARCHAR(200) NOT NULL, position VARCHAR(100), phone VARCHAR(50), monthly_salary NUMERIC(14,2) NOT NULL DEFAULT 0, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS salaries (id " + pk + ", employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE, amount NUMERIC(14,2) NOT NULL, payment_type VARCHAR(40) NOT NULL, note TEXT, occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS credits (id " + pk + ", code VARCHAR(40) UNIQUE NOT NULL, name VARCHAR(200) NOT NULL, bank_or_creditor VARCHAR(200) NOT NULL, total_amount NUMERIC(14,2) NOT NULL DEFAULT 0, monthly_amount NUMERIC(14,2) NOT NULL DEFAULT 0, remaining_balance NUMERIC(14,2) NOT NULL DEFAULT 0, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS credit_payments (id " + pk + ", credit_id BIGINT REFERENCES credits(id) ON DELETE SET NULL, credit_name VARCHAR(200) NOT NULL, bank_or_creditor VARCHAR(200), monthly_installment NUMERIC(14,2) NOT NULL DEFAULT 0, paid_amount NUMERIC(14,2) NOT NULL, note TEXT, occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS invoices (id " + pk + ", invoice_no VARCHAR(50) UNIQUE NOT NULL, delivery_doc_no VARCHAR(50), issue_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, seller_name VARCHAR(200) NOT NULL, seller_voen VARCHAR(50) NOT NULL, seller_address VARCHAR(250), seller_bank_details VARCHAR(250), customer_name VARCHAR(200) NOT NULL, customer_voen VARCHAR(50), payment_type VARCHAR(50) NOT NULL, vat_rate NUMERIC(5,2) NOT NULL DEFAULT 18.00, subtotal NUMERIC(14,2) NOT NULL, vat_amount NUMERIC(14,2) NOT NULL, total_amount NUMERIC(14,2) NOT NULL, notes TEXT)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS invoice_items (id " + pk + ", invoice_id BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE, item_name VARCHAR(200) NOT NULL, unit VARCHAR(30) NOT NULL DEFAULT 'ədəd', quantity NUMERIC(14,3) NOT NULL, unit_price NUMERIC(14,2) NOT NULL, line_total NUMERIC(14,2) NOT NULL)");
            try {
                statement.executeUpdate("ALTER TABLE credit_payments ADD COLUMN IF NOT EXISTS credit_id BIGINT REFERENCES credits(id) ON DELETE SET NULL");
            } catch (Exception ignore) {}
            try {
                statement.executeUpdate("ALTER TABLE invoices ADD COLUMN notes TEXT");
            } catch (Exception ignore) {}
            // Indexes for instant local search & monthly report rendering
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_deliveries_occurred ON deliveries (occurred_at)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_payments_occurred ON payments (occurred_at)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_cash_movements_occurred ON cash_movements (occurred_at)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_purchases_occurred ON purchases (occurred_at)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_salaries_occurred ON salaries (occurred_at)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_credit_payments_occurred ON credit_payments (occurred_at)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_wastes_occurred ON wastes (occurred_at)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_activity_logs_occurred ON activity_logs (occurred_at)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_invoices_issue ON invoices (issue_date)");
        }
    }

    public static void wipeAllDatabaseData() {
        if (!isConfigured()) return;
        String[] tables = {
            "invoice_items", "invoices", "credit_payments", "salaries", "purchases", "cash_movements", "payments",
            "deliveries", "wastes", "activity_logs", "productions", "credits",
            "products", "raw_materials", "customers", "suppliers", "managers", "employees"
        };
        try (Connection connection = connect(); Statement stmt = connection.createStatement()) {
            for (String table : tables) {
                try {
                    stmt.executeUpdate("TRUNCATE TABLE " + table + " RESTART IDENTITY CASCADE");
                } catch (Exception ignore) {
                    try {
                        stmt.executeUpdate("DELETE FROM " + table);
                    } catch (Exception ignored) {}
                }
            }
            System.out.println("Bütün verilənlər bazası məlumatları tamamilə silindi və sıfırlandı.");
        } catch (Exception e) {
            System.err.println("Baza sıfırlanarkən xəta: " + e.getMessage());
        }
    }

    public static void cleanupFutureTestRecords() {
        if (!isConfigured()) return;
        String currentMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String[] tables = {"credit_payments", "salaries", "purchases", "cash_movements", "payments", "deliveries", "wastes", "activity_logs"};
        String mExpr = monthExpr("occurred_at");
        try (Connection connection = connect()) {
            for (String table : tables) {
                try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM " + table + " WHERE " + mExpr + " > ?")) {
                    stmt.setString(1, currentMonth);
                    stmt.executeUpdate();
                } catch (Exception ignore) {}
            }
        } catch (Exception e) {
            System.err.println("Gələcək test qeydləri silinmədi: " + e.getMessage());
        }
    }

    public static long addActivityLog(String category, String description, LocalDateTime occurredAt) throws IOException, SQLException {
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO activity_logs (category, description, occurred_at) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, category);
            statement.setString(2, description);
            statement.setTimestamp(3, Timestamp.valueOf(occurredAt));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
            return 0;
        }
    }

    public static List<ActivityLogEntry> loadActivityLogs() throws IOException, SQLException {
        List<ActivityLogEntry> entries = new ArrayList<>();
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(
                "SELECT id, category, description, occurred_at FROM activity_logs ORDER BY occurred_at"); ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                entries.add(new ActivityLogEntry(result.getLong("id"), result.getString("category"), result.getString("description"),
                        result.getTimestamp("occurred_at").toLocalDateTime()));
            }
        }
        return entries;
    }

    public static void deleteActivityLog(long id) throws IOException, SQLException {
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM activity_logs WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    public static void deleteActivityLogByDescription(String category, String descLike) throws IOException, SQLException {
        String sql = "DELETE FROM activity_logs WHERE category = ? AND description LIKE ?";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category);
            statement.setString(2, descLike);
            statement.executeUpdate();
        }
    }

    public static long addCashMovement(String direction, BigDecimal amount, String description, LocalDateTime occurredAt) throws IOException, SQLException {
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO cash_movements (direction, amount, description, occurred_at) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, direction);
            statement.setBigDecimal(2, amount);
            statement.setString(3, description);
            statement.setTimestamp(4, Timestamp.valueOf(occurredAt));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
            return 0;
        }
    }

    public static List<CashMovementEntry> loadCashMovements() throws IOException, SQLException {
        List<CashMovementEntry> entries = new ArrayList<>();
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(
                "SELECT id, direction, amount, description, occurred_at FROM cash_movements ORDER BY occurred_at"); ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                entries.add(new CashMovementEntry(result.getLong("id"), result.getString("direction"), result.getBigDecimal("amount"),
                        result.getString("description"), result.getTimestamp("occurred_at").toLocalDateTime()));
            }
        }
        return entries;
    }

    public static void deleteCashMovement(long id) throws IOException, SQLException {
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM cash_movements WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    public static void deleteCashMovementByDescription(String description, BigDecimal amount) throws IOException, SQLException {
        String sql = "DELETE FROM cash_movements WHERE id = ("
                + "SELECT id FROM cash_movements WHERE description = ? AND amount = ? ORDER BY occurred_at DESC LIMIT 1)";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, description);
            statement.setBigDecimal(2, amount);
            statement.executeUpdate();
        }
    }

    public static void deleteCashMovementLike(String descPattern, BigDecimal amount) throws IOException, SQLException {
        String sql = "DELETE FROM cash_movements WHERE id = ("
                + "SELECT id FROM cash_movements WHERE description LIKE ? AND amount = ? ORDER BY occurred_at DESC LIMIT 1)";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, descPattern);
            statement.setBigDecimal(2, amount);
            statement.executeUpdate();
        }
    }

    public static long recordPayment(String paymentType, String customerCode, String supplierCode, String managerCode,
                                     BigDecimal amount, LocalDateTime occurredAt) throws IOException, SQLException {
        String sql = "INSERT INTO payments (payment_type, customer_id, supplier_id, manager_id, amount, occurred_at) VALUES (?, "
                + "(SELECT id FROM customers WHERE code = ?), "
                + "(SELECT id FROM suppliers WHERE code = ?), "
                + "(SELECT id FROM managers WHERE code = ?), ?, ?)";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, paymentType);
            statement.setString(2, customerCode);
            statement.setString(3, supplierCode);
            statement.setString(4, managerCode);
            statement.setBigDecimal(5, amount);
            statement.setTimestamp(6, Timestamp.valueOf(occurredAt));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
            return 0;
        }
    }

    public static List<PaymentEntry> loadPayments() throws IOException, SQLException {
        List<PaymentEntry> entries = new ArrayList<>();
        String sql = "SELECT p.id, p.payment_type, "
                + "COALESCE(c.name, s.name, '') AS person_name, "
                + "COALESCE(c.code, '') AS customer_code, "
                + "COALESCE(s.code, '') AS supplier_code, "
                + "p.amount, "
                + "COALESCE(m.code, '') AS manager_code, "
                + "p.occurred_at "
                + "FROM payments p "
                + "LEFT JOIN customers c ON c.id = p.customer_id "
                + "LEFT JOIN suppliers s ON s.id = p.supplier_id "
                + "LEFT JOIN managers m ON m.id = p.manager_id "
                + "ORDER BY p.occurred_at";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                entries.add(new PaymentEntry(result.getLong("id"), result.getString("payment_type"),
                        result.getString("person_name"), result.getString("customer_code"), result.getString("supplier_code"),
                        result.getBigDecimal("amount"), result.getString("manager_code"), result.getTimestamp("occurred_at").toLocalDateTime()));
            }
        }
        return entries;
    }

    public static void deletePayment(long id) throws IOException, SQLException {
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM payments WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    public static void deletePaymentByCriteria(String paymentType, String customerCode, String supplierCode, BigDecimal amount) throws IOException, SQLException {
        String sql = "DELETE FROM payments WHERE id = ("
                + "SELECT p.id FROM payments p "
                + "LEFT JOIN customers c ON c.id = p.customer_id "
                + "LEFT JOIN suppliers s ON s.id = p.supplier_id "
                + "WHERE p.payment_type = ? "
                + "AND (c.code = ? OR s.code = ? OR (? IS NULL AND ? IS NULL)) "
                + "AND p.amount = ? ORDER BY p.occurred_at DESC LIMIT 1)";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, paymentType);
            statement.setString(2, customerCode);
            statement.setString(3, supplierCode);
            statement.setString(4, customerCode);
            statement.setString(5, supplierCode);
            statement.setBigDecimal(6, amount);
            statement.executeUpdate();
        }
    }

    public static void recordDelivery(String documentNo, String customerCode, String managerCode, String productCode,
                                      BigDecimal quantity, BigDecimal unitPrice, BigDecimal unitCost, BigDecimal paidAmount,
                                      LocalDateTime occurredAt) throws IOException, SQLException {
        String deliverySql = "INSERT INTO deliveries (document_no, customer_id, manager_id, total, paid_amount, cost, occurred_at) "
                + "VALUES (?, (SELECT id FROM customers WHERE code = ?), (SELECT id FROM managers WHERE code = ?), ?, ?, ?, ?) "
                + "ON CONFLICT (document_no) DO UPDATE SET total = EXCLUDED.total, paid_amount = EXCLUDED.paid_amount, cost = EXCLUDED.cost";
        String itemSql = "INSERT INTO delivery_items (delivery_id, product_id, quantity, unit_price, unit_cost) "
                + "VALUES (?, (SELECT id FROM products WHERE code = ?), ?, ?, ?)";
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try (PreparedStatement delivery = connection.prepareStatement(deliverySql, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement item = connection.prepareStatement(itemSql)) {
                BigDecimal total = unitPrice.multiply(quantity);
                delivery.setString(1, documentNo);
                delivery.setString(2, customerCode);
                delivery.setString(3, managerCode);
                delivery.setBigDecimal(4, total);
                delivery.setBigDecimal(5, paidAmount);
                delivery.setBigDecimal(6, unitCost.multiply(quantity));
                delivery.setTimestamp(7, Timestamp.valueOf(occurredAt));
                delivery.executeUpdate();
                long deliveryId;
                try (ResultSet keys = delivery.getGeneratedKeys()) {
                    if (keys.next()) {
                        deliveryId = keys.getLong(1);
                    } else {
                        try (PreparedStatement findId = connection.prepareStatement("SELECT id FROM deliveries WHERE document_no = ?")) {
                            findId.setString(1, documentNo);
                            try (ResultSet idResult = findId.executeQuery()) {
                                if (idResult.next()) deliveryId = idResult.getLong(1);
                                else throw new SQLException("Təhvil qeydinin identifikatoru tapılmadı.");
                            }
                        }
                    }
                }
                item.setLong(1, deliveryId);
                item.setString(2, productCode);
                item.setBigDecimal(3, quantity);
                item.setBigDecimal(4, unitPrice);
                item.setBigDecimal(5, unitCost);
                item.executeUpdate();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public static List<DeliveryEntry> loadDeliveries() throws IOException, SQLException {
        List<DeliveryEntry> entries = new ArrayList<>();
        String sql = "SELECT d.document_no, "
                + "COALESCE(c.code, '') AS customer_code, "
                + "COALESCE(c.name, '—') AS customer_name, "
                + "COALESCE(p.code, '') AS product_code, "
                + "COALESCE(p.name, '—') AS product_name, "
                + "di.quantity, d.total, d.cost, d.paid_amount, "
                + "(d.total - d.paid_amount) AS remaining_debt, "
                + "COALESCE(m.code, '') AS manager_code, "
                + "COALESCE(m.name, '—') AS manager_name, "
                + "d.occurred_at "
                + "FROM deliveries d "
                + "LEFT JOIN customers c ON c.id = d.customer_id "
                + "LEFT JOIN delivery_items di ON di.delivery_id = d.id "
                + "LEFT JOIN products p ON p.id = di.product_id "
                + "LEFT JOIN managers m ON m.id = d.manager_id "
                + "ORDER BY d.occurred_at";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                entries.add(new DeliveryEntry(
                        result.getString("document_no"),
                        result.getString("customer_code"),
                        result.getString("customer_name"),
                        result.getString("product_code"),
                        result.getString("product_name"),
                        result.getBigDecimal("quantity"),
                        result.getBigDecimal("total"),
                        result.getBigDecimal("cost"),
                        result.getBigDecimal("paid_amount"),
                        result.getBigDecimal("remaining_debt"),
                        result.getString("manager_code"),
                        result.getString("manager_name"),
                        result.getTimestamp("occurred_at").toLocalDateTime()));
            }
        }
        return entries;
    }

    public static void deleteDelivery(String documentNo) throws IOException, SQLException {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try {
                String findSql = "SELECT customer_id, (total - paid_amount) AS debt FROM deliveries WHERE document_no = ?";
                long customerId = 0;
                BigDecimal debt = BigDecimal.ZERO;
                try (PreparedStatement findStmt = connection.prepareStatement(findSql)) {
                    findStmt.setString(1, documentNo);
                    try (ResultSet rs = findStmt.executeQuery()) {
                        if (rs.next()) {
                            customerId = rs.getLong("customer_id");
                            debt = rs.getBigDecimal("debt");
                        }
                    }
                }
                if (customerId > 0 && debt != null && debt.signum() > 0) {
                    try (PreparedStatement custStmt = connection.prepareStatement("UPDATE customers SET opening_debt = CASE WHEN (opening_debt - ?) < 0 THEN 0 ELSE (opening_debt - ?) END WHERE id = ?")) {
                        custStmt.setBigDecimal(1, debt);
                        custStmt.setBigDecimal(2, debt);
                        custStmt.setLong(3, customerId);
                        custStmt.executeUpdate();
                    }
                }
                try (PreparedStatement delStmt = connection.prepareStatement("DELETE FROM deliveries WHERE document_no = ?")) {
                    delStmt.setString(1, documentNo);
                    delStmt.executeUpdate();
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public static void deletePurchaseCascade(long purchaseId) throws IOException, SQLException {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try {
                String findSql = "SELECT supplier_id, raw_material_id, quantity, debt_amount FROM purchases WHERE id = ?";
                long supplierId = 0;
                long rawMaterialId = 0;
                BigDecimal quantity = BigDecimal.ZERO;
                BigDecimal debtAmount = BigDecimal.ZERO;

                try (PreparedStatement findStmt = connection.prepareStatement(findSql)) {
                    findStmt.setLong(1, purchaseId);
                    try (ResultSet rs = findStmt.executeQuery()) {
                        if (rs.next()) {
                            supplierId = rs.getLong("supplier_id");
                            rawMaterialId = rs.getLong("raw_material_id");
                            quantity = rs.getBigDecimal("quantity");
                            debtAmount = rs.getBigDecimal("debt_amount");
                        }
                    }
                }

                if (supplierId > 0 && debtAmount != null && debtAmount.signum() > 0) {
                    try (PreparedStatement suppStmt = connection.prepareStatement("UPDATE suppliers SET opening_debt = CASE WHEN (opening_debt - ?) < 0 THEN 0 ELSE (opening_debt - ?) END WHERE id = ?")) {
                        suppStmt.setBigDecimal(1, debtAmount);
                        suppStmt.setBigDecimal(2, debtAmount);
                        suppStmt.setLong(3, supplierId);
                        suppStmt.executeUpdate();
                    }
                }

                if (rawMaterialId > 0 && quantity != null && quantity.signum() > 0) {
                    try (PreparedStatement matStmt = connection.prepareStatement("UPDATE raw_materials SET quantity = CASE WHEN (quantity - ?) < 0 THEN 0 ELSE (quantity - ?) END WHERE id = ?")) {
                        matStmt.setBigDecimal(1, quantity);
                        matStmt.setBigDecimal(2, quantity);
                        matStmt.setLong(3, rawMaterialId);
                        matStmt.executeUpdate();
                    }
                }

                try (PreparedStatement delStmt = connection.prepareStatement("DELETE FROM purchases WHERE id = ?")) {
                    delStmt.setLong(1, purchaseId);
                    delStmt.executeUpdate();
                }

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public static void updatePurchase(long purchaseId, BigDecimal newTotal, BigDecimal newPaid) throws IOException, SQLException {
        BigDecimal newDebt = newTotal.subtract(newPaid);
        if (newDebt.signum() < 0) newDebt = BigDecimal.ZERO;

        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try {
                String findSql = "SELECT supplier_id, debt_amount FROM purchases WHERE id = ?";
                long supplierId = 0;
                BigDecimal oldDebt = BigDecimal.ZERO;

                try (PreparedStatement findStmt = connection.prepareStatement(findSql)) {
                    findStmt.setLong(1, purchaseId);
                    try (ResultSet rs = findStmt.executeQuery()) {
                        if (rs.next()) {
                            supplierId = rs.getLong("supplier_id");
                            oldDebt = rs.getBigDecimal("debt_amount");
                        }
                    }
                }

                BigDecimal debtDiff = newDebt.subtract(oldDebt == null ? BigDecimal.ZERO : oldDebt);

                if (supplierId > 0 && debtDiff.signum() != 0) {
                    try (PreparedStatement suppStmt = connection.prepareStatement("UPDATE suppliers SET opening_debt = CASE WHEN (opening_debt + ?) < 0 THEN 0 ELSE (opening_debt + ?) END WHERE id = ?")) {
                        suppStmt.setBigDecimal(1, debtDiff);
                        suppStmt.setBigDecimal(2, debtDiff);
                        suppStmt.setLong(3, supplierId);
                        suppStmt.executeUpdate();
                    }
                }

                try (PreparedStatement updateStmt = connection.prepareStatement("UPDATE purchases SET total_amount = ?, paid_amount = ?, debt_amount = ? WHERE id = ?")) {
                    updateStmt.setBigDecimal(1, newTotal);
                    updateStmt.setBigDecimal(2, newPaid);
                    updateStmt.setBigDecimal(3, newDebt);
                    updateStmt.setLong(4, purchaseId);
                    updateStmt.executeUpdate();
                }

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public static List<MonthlyProductReportEntry> loadMonthlyProductSales(String monthKey) throws IOException, SQLException {
        List<MonthlyProductReportEntry> entries = new ArrayList<>();
        String mExpr = monthExpr("d.occurred_at");
        String sql = "SELECT " + mExpr + " AS month_key, "
                + "COALESCE(p.code, '') AS product_code, "
                + "COALESCE(p.name, '') AS product_name, "
                + "SUM(di.quantity) AS total_quantity, "
                + "SUM(di.quantity * di.unit_price) AS total_amount, "
                + "COUNT(DISTINCT d.id) AS delivery_count "
                + "FROM deliveries d "
                + "JOIN delivery_items di ON di.delivery_id = d.id "
                + "JOIN products p ON p.id = di.product_id "
                + "WHERE " + mExpr + " = ? "
                + "GROUP BY " + mExpr + ", p.code, p.name "
                + "ORDER BY total_amount DESC";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, monthKey);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    entries.add(new MonthlyProductReportEntry(
                            result.getString("month_key"),
                            result.getString("product_code"),
                            result.getString("product_name"),
                            result.getBigDecimal("total_quantity"),
                            result.getBigDecimal("total_amount"),
                            result.getInt("delivery_count")
                    ));
                }
            }
        }
        return entries;
    }

    public static List<MonthlyRawMaterialReportEntry> loadMonthlyRawMaterialPurchases(String monthKey) throws IOException, SQLException {
        List<MonthlyRawMaterialReportEntry> entries = new ArrayList<>();
        String mExpr = monthExpr("pu.occurred_at");
        String sql = "SELECT " + mExpr + " AS month_key, "
                + "COALESCE(r.code, '') AS material_code, "
                + "COALESCE(r.name, '') AS material_name, "
                + "COALESCE(r.unit, '') AS unit, "
                + "SUM(pu.quantity) AS total_quantity, "
                + "SUM(pu.total_amount) AS total_amount, "
                + "SUM(pu.paid_amount) AS paid_amount, "
                + "SUM(pu.debt_amount) AS debt_amount, "
                + "COUNT(pu.id) AS purchase_count "
                + "FROM purchases pu "
                + "JOIN raw_materials r ON r.id = pu.raw_material_id "
                + "WHERE " + mExpr + " = ? "
                + "GROUP BY " + mExpr + ", r.code, r.name, r.unit "
                + "ORDER BY debt_amount DESC";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, monthKey);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    entries.add(new MonthlyRawMaterialReportEntry(
                            result.getString("month_key"),
                            result.getString("material_code"),
                            result.getString("material_name"),
                            result.getString("unit"),
                            result.getBigDecimal("total_quantity"),
                            result.getBigDecimal("total_amount"),
                            result.getBigDecimal("paid_amount"),
                            result.getBigDecimal("debt_amount"),
                            result.getInt("purchase_count")
                    ));
                }
            }
        }
        return entries;
    }

    public static List<String> loadAllEventMonths() throws IOException, SQLException {
        List<String> months = new ArrayList<>();
        String currentMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String mD = monthExpr("occurred_at");
        String sql = "SELECT DISTINCT month_key FROM ("
                + "SELECT " + mD + " AS month_key FROM deliveries UNION "
                + "SELECT " + mD + " AS month_key FROM payments UNION "
                + "SELECT " + mD + " AS month_key FROM cash_movements UNION "
                + "SELECT " + mD + " AS month_key FROM wastes UNION "
                + "SELECT " + mD + " AS month_key FROM productions UNION "
                + "SELECT " + mD + " AS month_key FROM purchases UNION "
                + "SELECT " + mD + " AS month_key FROM salaries UNION "
                + "SELECT " + mD + " AS month_key FROM credit_payments UNION "
                + "SELECT " + mD + " AS month_key FROM activity_logs"
                + ") all_events WHERE month_key IS NOT NULL AND month_key <= ? ORDER BY month_key DESC";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, currentMonth);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    months.add(result.getString("month_key"));
                }
            }
        }
        return months;
    }

    public static long recordPurchase(String documentNo, String supplierCode, String rawMaterialCode,
                                      BigDecimal quantity, BigDecimal totalAmount, BigDecimal paidAmount,
                                      LocalDateTime occurredAt) throws IOException, SQLException {
        BigDecimal debtAmount = totalAmount.subtract(paidAmount);
        if (debtAmount.signum() < 0) debtAmount = BigDecimal.ZERO;
        String sql = "INSERT INTO purchases (document_no, supplier_id, raw_material_id, quantity, total_amount, paid_amount, debt_amount, occurred_at) "
                + "VALUES (?, (SELECT id FROM suppliers WHERE code = ?), (SELECT id FROM raw_materials WHERE code = ?), ?, ?, ?, ?, ?)";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, documentNo);
            statement.setString(2, supplierCode);
            statement.setString(3, rawMaterialCode);
            statement.setBigDecimal(4, quantity);
            statement.setBigDecimal(5, totalAmount);
            statement.setBigDecimal(6, paidAmount);
            statement.setBigDecimal(7, debtAmount);
            statement.setTimestamp(8, Timestamp.valueOf(occurredAt));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
            return 0;
        }
    }

    public static List<PurchaseEntry> loadPurchases() throws IOException, SQLException {
        List<PurchaseEntry> entries = new ArrayList<>();
        String sql = "SELECT pu.id, pu.document_no, "
                + "COALESCE(s.code, '') AS supplier_code, "
                + "COALESCE(s.name, '—') AS supplier_name, "
                + "COALESCE(r.code, '') AS material_code, "
                + "COALESCE(r.name, '—') AS material_name, "
                + "COALESCE(r.unit, '') AS unit, "
                + "pu.quantity, pu.total_amount, pu.paid_amount, pu.debt_amount, "
                + "pu.occurred_at "
                + "FROM purchases pu "
                + "LEFT JOIN suppliers s ON s.id = pu.supplier_id "
                + "LEFT JOIN raw_materials r ON r.id = pu.raw_material_id "
                + "ORDER BY pu.occurred_at";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                entries.add(new PurchaseEntry(
                        result.getLong("id"),
                        result.getString("document_no"),
                        result.getString("supplier_code"),
                        result.getString("supplier_name"),
                        result.getString("material_code"),
                        result.getString("material_name"),
                        result.getString("unit"),
                        result.getBigDecimal("quantity"),
                        result.getBigDecimal("total_amount"),
                        result.getBigDecimal("paid_amount"),
                        result.getBigDecimal("debt_amount"),
                        result.getTimestamp("occurred_at").toLocalDateTime()
                ));
            }
        }
        return entries;
    }

    public static void deletePurchase(long id) throws IOException, SQLException {
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement("DELETE FROM purchases WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    public static List<ManagerEntry> loadManagers() throws IOException, SQLException {
        List<ManagerEntry> entries = new ArrayList<>();
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(
                "SELECT code, name, commission_rate FROM managers ORDER BY name"); ResultSet result = statement.executeQuery()) {
            while (result.next()) entries.add(new ManagerEntry(result.getString("code"), result.getString("name"), result.getBigDecimal("commission_rate")));
        }
        return entries;
    }

    public static List<CustomerEntry> loadCustomers() throws IOException, SQLException {
        List<CustomerEntry> entries = new ArrayList<>();
        String sql = "SELECT c.code, c.name, c.phone, c.opening_debt, COALESCE(m.code, '') AS manager_code FROM customers c LEFT JOIN managers m ON m.id = c.manager_id ORDER BY c.name";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet result = statement.executeQuery()) {
            while (result.next()) entries.add(new CustomerEntry(result.getString("code"), result.getString("name"), result.getString("phone"),
                    result.getBigDecimal("opening_debt"), result.getString("manager_code")));
        }
        return entries;
    }

    public static List<SupplierEntry> loadSuppliers() throws IOException, SQLException {
        List<SupplierEntry> entries = new ArrayList<>();
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(
                "SELECT code, name, phone, opening_debt FROM suppliers ORDER BY name"); ResultSet result = statement.executeQuery()) {
            while (result.next()) entries.add(new SupplierEntry(result.getString("code"), result.getString("name"), result.getString("phone"), result.getBigDecimal("opening_debt")));
        }
        return entries;
    }

    public static List<EmployeeEntry> loadEmployees() throws IOException, SQLException {
        List<EmployeeEntry> entries = new ArrayList<>();
        String sql = "SELECT code, name, position, phone, monthly_salary FROM employees ORDER BY name";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                entries.add(new EmployeeEntry(result.getString("code"), result.getString("name"),
                        result.getString("position"), result.getString("phone"), result.getBigDecimal("monthly_salary")));
            }
        }
        return entries;
    }

    public static void upsertEmployee(EmployeeEntry entry) throws IOException, SQLException {
        String sql = "INSERT INTO employees (code, name, position, phone, monthly_salary) VALUES (?, ?, ?, ?, ?) "
                + "ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, position = EXCLUDED.position, phone = EXCLUDED.phone, monthly_salary = EXCLUDED.monthly_salary";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, entry.code());
            statement.setString(2, entry.name());
            statement.setString(3, entry.position());
            statement.setString(4, entry.phone());
            statement.setBigDecimal(5, entry.monthlySalary());
            statement.executeUpdate();
        }
    }

    public static void deleteEmployee(String code) throws IOException, SQLException {
        deleteByCode("employees", code);
    }

    public static long recordSalary(String employeeCode, BigDecimal amount, String paymentType, String note, LocalDateTime occurredAt) throws IOException, SQLException {
        String sql = "INSERT INTO salaries (employee_id, amount, payment_type, note, occurred_at) VALUES ("
                + "(SELECT id FROM employees WHERE code = ?), ?, ?, ?, ?)";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, employeeCode);
            statement.setBigDecimal(2, amount);
            statement.setString(3, paymentType);
            statement.setString(4, note);
            statement.setTimestamp(5, Timestamp.valueOf(occurredAt));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
            return 0;
        }
    }

    public static List<SalaryEntry> loadSalaries() throws IOException, SQLException {
        List<SalaryEntry> entries = new ArrayList<>();
        String sql = "SELECT s.id, COALESCE(e.code, '') AS employee_code, COALESCE(e.name, '—') AS employee_name, "
                + "COALESCE(e.position, '') AS position, s.amount, s.payment_type, s.note, s.occurred_at "
                + "FROM salaries s LEFT JOIN employees e ON e.id = s.employee_id ORDER BY s.occurred_at";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                entries.add(new SalaryEntry(result.getLong("id"), result.getString("employee_code"),
                        result.getString("employee_name"), result.getString("position"),
                        result.getBigDecimal("amount"), result.getString("payment_type"),
                        result.getString("note"), result.getTimestamp("occurred_at").toLocalDateTime()));
            }
        }
        return entries;
    }

    public static void deleteSalary(long id) throws IOException, SQLException {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try {
                String findSql = "SELECT e.name AS emp_name, s.amount FROM salaries s JOIN employees e ON e.id = s.employee_id WHERE s.id = ?";
                String empName = null;
                BigDecimal amount = null;
                try (PreparedStatement findStmt = connection.prepareStatement(findSql)) {
                    findStmt.setLong(1, id);
                    try (ResultSet rs = findStmt.executeQuery()) {
                        if (rs.next()) {
                            empName = rs.getString("emp_name");
                            amount = rs.getBigDecimal("amount");
                        }
                    }
                }

                try (PreparedStatement delSalary = connection.prepareStatement("DELETE FROM salaries WHERE id = ?")) {
                    delSalary.setLong(1, id);
                    delSalary.executeUpdate();
                }

                if (empName != null && amount != null) {
                    String delCashSql = "DELETE FROM cash_movements WHERE id = ("
                            + "SELECT id FROM cash_movements WHERE description LIKE ? AND amount = ? ORDER BY occurred_at DESC LIMIT 1)";
                    try (PreparedStatement delCash = connection.prepareStatement(delCashSql)) {
                        delCash.setString(1, "%" + empName + "%");
                        delCash.setBigDecimal(2, amount);
                        delCash.executeUpdate();
                    }
                }

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public static List<MonthlySalaryReportEntry> loadMonthlySalaries(String monthKey) throws IOException, SQLException {
        List<MonthlySalaryReportEntry> entries = new ArrayList<>();
        String mExpr = monthExpr("s.occurred_at");
        String sql = "SELECT " + mExpr + " AS month_key, "
                + "COALESCE(e.code, '') AS employee_code, "
                + "COALESCE(e.name, '') AS employee_name, "
                + "COALESCE(e.position, '') AS position, "
                + "COALESCE(e.monthly_salary, 0) AS monthly_salary, "
                + "SUM(s.amount) AS total_paid, "
                + "SUM(CASE WHEN s.payment_type LIKE '%Tez%' OR s.payment_type LIKE '%Avans%' THEN s.amount ELSE 0 END) AS advance_paid, "
                + "SUM(CASE WHEN s.payment_type LIKE '%Əlavə%' OR s.payment_type LIKE '%Bonus%' OR s.payment_type LIKE '%Mükafat%' THEN s.amount ELSE 0 END) AS bonus_paid, "
                + "COUNT(s.id) AS payment_count "
                + "FROM salaries s "
                + "JOIN employees e ON e.id = s.employee_id "
                + "WHERE " + mExpr + " = ? "
                + "GROUP BY " + mExpr + ", e.code, e.name, e.position, e.monthly_salary "
                + "ORDER BY total_paid DESC";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, monthKey);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    entries.add(new MonthlySalaryReportEntry(
                            result.getString("month_key"),
                            result.getString("employee_code"),
                            result.getString("employee_name"),
                            result.getString("position"),
                            result.getBigDecimal("monthly_salary"),
                            result.getBigDecimal("total_paid"),
                            result.getBigDecimal("advance_paid"),
                            result.getBigDecimal("bonus_paid"),
                            result.getInt("payment_count")
                    ));
                }
            }
        }
        return entries;
    }

    public static List<CreditEntry> loadCredits() throws IOException, SQLException {
        List<CreditEntry> entries = new ArrayList<>();
        String sql = "SELECT code, name, bank_or_creditor, total_amount, monthly_amount, remaining_balance FROM credits ORDER BY name";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                entries.add(new CreditEntry(
                        result.getString("code"),
                        result.getString("name"),
                        result.getString("bank_or_creditor"),
                        result.getBigDecimal("total_amount"),
                        result.getBigDecimal("monthly_amount"),
                        result.getBigDecimal("remaining_balance")
                ));
            }
        }
        return entries;
    }

    public static void upsertCredit(CreditEntry entry) throws IOException, SQLException {
        String sql = "INSERT INTO credits (code, name, bank_or_creditor, total_amount, monthly_amount, remaining_balance) "
                + "VALUES (?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, bank_or_creditor = EXCLUDED.bank_or_creditor, "
                + "total_amount = EXCLUDED.total_amount, monthly_amount = EXCLUDED.monthly_amount, remaining_balance = EXCLUDED.remaining_balance";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, entry.code());
            statement.setString(2, entry.name());
            statement.setString(3, entry.bankOrCreditor());
            statement.setBigDecimal(4, entry.totalAmount());
            statement.setBigDecimal(5, entry.monthlyAmount());
            statement.setBigDecimal(6, entry.remainingBalance());
            statement.executeUpdate();
        }
    }

    public static void deleteCredit(String code) throws IOException, SQLException {
        deleteByCode("credits", code);
    }

    public static long recordCreditPayment(String creditCode, String creditName, String bankOrCreditor, BigDecimal monthlyInstallment, BigDecimal paidAmount, String note, LocalDateTime occurredAt) throws IOException, SQLException {
        String sql = "INSERT INTO credit_payments (credit_id, credit_name, bank_or_creditor, monthly_installment, paid_amount, note, occurred_at) "
                + "VALUES ((SELECT id FROM credits WHERE code = ?), ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, creditCode);
            statement.setString(2, creditName);
            statement.setString(3, bankOrCreditor);
            statement.setBigDecimal(4, monthlyInstallment);
            statement.setBigDecimal(5, paidAmount);
            statement.setString(6, note);
            statement.setTimestamp(7, Timestamp.valueOf(occurredAt));
            statement.executeUpdate();

            // Update remaining balance on credit contract
            if (creditCode != null && !creditCode.isBlank()) {
                String sub = isSqlite() ? "MAX(0, remaining_balance - ?)" : "GREATEST(0, remaining_balance - ?)";
                try (PreparedStatement update = connection.prepareStatement("UPDATE credits SET remaining_balance = " + sub + " WHERE code = ?")) {
                    update.setBigDecimal(1, paidAmount);
                    update.setString(2, creditCode);
                    update.executeUpdate();
                }
            }

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
            return 0;
        }
    }

    public static List<CreditPaymentEntry> loadCreditPayments() throws IOException, SQLException {
        List<CreditPaymentEntry> entries = new ArrayList<>();
        String sql = "SELECT cp.id, COALESCE(c.code, '') AS credit_code, cp.credit_name, cp.bank_or_creditor, cp.monthly_installment, cp.paid_amount, cp.note, cp.occurred_at "
                + "FROM credit_payments cp LEFT JOIN credits c ON c.id = cp.credit_id ORDER BY cp.occurred_at DESC";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                entries.add(new CreditPaymentEntry(
                        result.getLong("id"),
                        result.getString("credit_code"),
                        result.getString("credit_name"),
                        result.getString("bank_or_creditor"),
                        result.getBigDecimal("monthly_installment"),
                        result.getBigDecimal("paid_amount"),
                        result.getString("note"),
                        result.getTimestamp("occurred_at").toLocalDateTime()
                ));
            }
        }
        return entries;
    }

    public static void deleteCreditPayment(long id) throws IOException, SQLException {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try {
                long creditId = 0;
                String creditName = null;
                BigDecimal amount = null;
                String selectSql = "SELECT credit_id, credit_name, paid_amount FROM credit_payments WHERE id = ?";
                try (PreparedStatement select = connection.prepareStatement(selectSql)) {
                    select.setLong(1, id);
                    try (ResultSet rs = select.executeQuery()) {
                        if (rs.next()) {
                            creditId = rs.getLong("credit_id");
                            creditName = rs.getString("credit_name");
                            amount = rs.getBigDecimal("paid_amount");
                        }
                    }
                }

                if (creditId > 0 && amount != null) {
                    try (PreparedStatement update = connection.prepareStatement("UPDATE credits SET remaining_balance = remaining_balance + ? WHERE id = ?")) {
                        update.setBigDecimal(1, amount);
                        update.setLong(2, creditId);
                        update.executeUpdate();
                    }
                }

                try (PreparedStatement delStmt = connection.prepareStatement("DELETE FROM credit_payments WHERE id = ?")) {
                    delStmt.setLong(1, id);
                    delStmt.executeUpdate();
                }

                if (creditName != null && amount != null) {
                    String delCashSql = "DELETE FROM cash_movements WHERE id = ("
                            + "SELECT id FROM cash_movements WHERE description LIKE ? AND amount = ? ORDER BY occurred_at DESC LIMIT 1)";
                    try (PreparedStatement delCash = connection.prepareStatement(delCashSql)) {
                        delCash.setString(1, "%" + creditName + "%");
                        delCash.setBigDecimal(2, amount);
                        delCash.executeUpdate();
                    }
                }

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public static List<MonthlyCreditReportEntry> loadMonthlyCreditPayments(String monthKey) throws IOException, SQLException {
        List<MonthlyCreditReportEntry> entries = new ArrayList<>();
        String mExpr = monthExpr("cp.occurred_at");
        String sql = "SELECT " + mExpr + " AS month_key, "
                + "COALESCE(c.code, '') AS credit_code, "
                + "cp.credit_name, cp.bank_or_creditor, "
                + "COALESCE(c.monthly_amount, MAX(cp.monthly_installment)) AS monthly_installment, "
                + "SUM(cp.paid_amount) AS total_paid, "
                + "COALESCE(c.remaining_balance, 0) AS remaining_balance, "
                + "COUNT(cp.id) AS payment_count "
                + "FROM credit_payments cp "
                + "LEFT JOIN credits c ON c.id = cp.credit_id "
                + "WHERE " + mExpr + " = ? "
                + "GROUP BY " + mExpr + ", c.code, cp.credit_name, cp.bank_or_creditor, c.monthly_amount, c.remaining_balance "
                + "ORDER BY total_paid DESC";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, monthKey);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    entries.add(new MonthlyCreditReportEntry(
                            result.getString("month_key"),
                            result.getString("credit_code"),
                            result.getString("credit_name"),
                            result.getString("bank_or_creditor"),
                            result.getBigDecimal("monthly_installment"),
                            result.getBigDecimal("total_paid"),
                            result.getBigDecimal("remaining_balance"),
                            result.getInt("payment_count")
                    ));
                }
            }
        }
        return entries;
    }

    public static void upsertManager(ManagerEntry entry) throws IOException, SQLException {
        String sql = "INSERT INTO managers (code, name, commission_rate) VALUES (?, ?, ?) ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, commission_rate = EXCLUDED.commission_rate";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, entry.code());
            statement.setString(2, entry.name());
            statement.setBigDecimal(3, entry.commissionRate());
            statement.executeUpdate();
        }
    }

    public static void upsertCustomer(CustomerEntry entry) throws IOException, SQLException {
        String sql = "INSERT INTO customers (code, name, phone, opening_debt, manager_id) VALUES (?, ?, ?, ?, (SELECT id FROM managers WHERE code = ?)) "
                + "ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, phone = EXCLUDED.phone, opening_debt = EXCLUDED.opening_debt, manager_id = (SELECT id FROM managers WHERE code = ?)";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, entry.code());
            statement.setString(2, entry.name());
            statement.setString(3, entry.phone());
            statement.setBigDecimal(4, entry.debt());
            statement.setString(5, entry.managerCode());
            statement.setString(6, entry.managerCode());
            statement.executeUpdate();
        }
    }

    public static void upsertSupplier(SupplierEntry entry) throws IOException, SQLException {
        String sql = "INSERT INTO suppliers (code, name, phone, opening_debt) VALUES (?, ?, ?, ?) ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, phone = EXCLUDED.phone, opening_debt = EXCLUDED.opening_debt";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, entry.code());
            statement.setString(2, entry.name());
            statement.setString(3, entry.phone());
            statement.setBigDecimal(4, entry.debt());
            statement.executeUpdate();
        }
    }

    public static void deleteCustomer(String code) throws IOException, SQLException { deleteByCode("customers", code); }
    public static void deleteSupplier(String code) throws IOException, SQLException { deleteByCode("suppliers", code); }
    public static void deleteManager(String code) throws IOException, SQLException { deleteByCode("managers", code); }
    public static void deleteRawMaterial(String code) throws IOException, SQLException { deleteByCode("raw_materials", code); }
    public static void deleteProduct(String code) throws IOException, SQLException { deleteByCode("products", code); }

    public static List<RawMaterialEntry> loadRawMaterials() throws IOException, SQLException {
        List<RawMaterialEntry> entries = new ArrayList<>();
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(
                "SELECT code, name, unit, quantity, unit_cost FROM raw_materials ORDER BY name"); ResultSet result = statement.executeQuery()) {
            while (result.next()) entries.add(new RawMaterialEntry(result.getString("code"), result.getString("name"), result.getString("unit"),
                    result.getBigDecimal("quantity"), result.getBigDecimal("unit_cost")));
        }
        return entries;
    }

    public static List<ProductEntry> loadProducts() throws IOException, SQLException {
        List<ProductEntry> entries = new ArrayList<>();
        String sql = "SELECT p.code, p.name, p.quantity, p.delivery_price, p.unit_cost, r.code AS raw_code, pr.quantity_per_product "
                + "FROM products p "
                + "LEFT JOIN product_recipes pr ON pr.product_id = p.id "
                + "LEFT JOIN raw_materials r ON r.id = pr.raw_material_id "
                + "ORDER BY p.name";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet result = statement.executeQuery()) {
            java.util.LinkedHashMap<String, ProductEntryBuilder> map = new java.util.LinkedHashMap<>();
            while (result.next()) {
                String code = result.getString("code");
                ProductEntryBuilder builder = map.get(code);
                if (builder == null) {
                    builder = new ProductEntryBuilder(code, result.getString("name"), result.getBigDecimal("quantity"),
                            result.getBigDecimal("delivery_price"), result.getBigDecimal("unit_cost"));
                    map.put(code, builder);
                }
                if (result.getString("raw_code") != null && result.getBigDecimal("quantity_per_product") != null) {
                    builder.recipe.add(new RecipeEntry(result.getString("raw_code"), result.getBigDecimal("quantity_per_product")));
                }
            }
            map.values().forEach(builder -> entries.add(new ProductEntry(builder.code, builder.name, builder.quantity, builder.deliveryPrice, builder.unitCost, List.copyOf(builder.recipe))));
        }
        return entries;
    }

    public static void upsertRawMaterial(RawMaterialEntry entry) throws IOException, SQLException {
        String sql = "INSERT INTO raw_materials (code, name, unit, quantity, unit_cost) VALUES (?, ?, ?, ?, ?) "
                + "ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, unit = EXCLUDED.unit, quantity = EXCLUDED.quantity, unit_cost = EXCLUDED.unit_cost";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, entry.code());
            statement.setString(2, entry.name());
            statement.setString(3, entry.unit());
            statement.setBigDecimal(4, entry.quantity());
            statement.setBigDecimal(5, entry.unitCost());
            statement.executeUpdate();
        }
    }

    public static void upsertProduct(ProductEntry entry) throws IOException, SQLException {
        String productSql = "INSERT INTO products (code, name, quantity, delivery_price, unit_cost) VALUES (?, ?, ?, ?, ?) "
                + "ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, quantity = EXCLUDED.quantity, delivery_price = EXCLUDED.delivery_price, unit_cost = EXCLUDED.unit_cost";
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try (PreparedStatement product = connection.prepareStatement(productSql);
                 PreparedStatement deleteRecipe = connection.prepareStatement("DELETE FROM product_recipes WHERE product_id = (SELECT id FROM products WHERE code = ?)");
                 PreparedStatement insertRecipe = connection.prepareStatement("INSERT INTO product_recipes (product_id, raw_material_id, quantity_per_product) VALUES ((SELECT id FROM products WHERE code = ?), (SELECT id FROM raw_materials WHERE code = ?), ?)")) {
                product.setString(1, entry.code());
                product.setString(2, entry.name());
                product.setBigDecimal(3, entry.quantity());
                product.setBigDecimal(4, entry.deliveryPrice());
                product.setBigDecimal(5, entry.unitCost());
                product.executeUpdate();

                deleteRecipe.setString(1, entry.code());
                deleteRecipe.executeUpdate();

                for (RecipeEntry recipe : entry.recipe()) {
                    insertRecipe.setString(1, entry.code());
                    insertRecipe.setString(2, recipe.rawMaterialCode());
                    insertRecipe.setBigDecimal(3, recipe.quantityPerProduct());
                    insertRecipe.addBatch();
                }
                insertRecipe.executeBatch();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public static void recordProduction(String productCode, BigDecimal quantity, BigDecimal unitCost, LocalDateTime occurredAt) throws IOException, SQLException {
        String prodSql = "INSERT INTO productions (product_id, quantity, unit_cost, occurred_at) VALUES ((SELECT id FROM products WHERE code = ?), ?, ?, ?)";
        String updateProductSql = "UPDATE products SET quantity = quantity + ? WHERE code = ?";

        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(prodSql)) {
                    statement.setString(1, productCode);
                    statement.setBigDecimal(2, quantity);
                    statement.setBigDecimal(3, unitCost);
                    statement.setTimestamp(4, Timestamp.valueOf(occurredAt));
                    statement.executeUpdate();
                }

                try (PreparedStatement pStmt = connection.prepareStatement(updateProductSql)) {
                    pStmt.setBigDecimal(1, quantity);
                    pStmt.setString(2, productCode);
                    pStmt.executeUpdate();
                }

                String recipeSql = "SELECT r.code AS raw_code, pr.quantity_per_product FROM product_recipes pr "
                        + "JOIN products p ON p.id = pr.product_id "
                        + "JOIN raw_materials r ON r.id = pr.raw_material_id WHERE p.code = ?";
                try (PreparedStatement rStmt = connection.prepareStatement(recipeSql)) {
                    rStmt.setString(1, productCode);
                    try (ResultSet rs = rStmt.executeQuery()) {
                        while (rs.next()) {
                            String rawCode = rs.getString("raw_code");
                            BigDecimal perUnit = rs.getBigDecimal("quantity_per_product");
                            BigDecimal totalRequired = perUnit.multiply(quantity);
                            try (PreparedStatement deductStmt = connection.prepareStatement(
                                    "UPDATE raw_materials SET quantity = CASE WHEN (quantity - ?) < 0 THEN 0 ELSE (quantity - ?) END WHERE code = ?")) {
                                deductStmt.setBigDecimal(1, totalRequired);
                                deductStmt.setBigDecimal(2, totalRequired);
                                deductStmt.setString(3, rawCode);
                                deductStmt.executeUpdate();
                            }
                        }
                    }
                }

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public static long recordWaste(String rawMaterialCode, BigDecimal quantity, BigDecimal cost, String reason, LocalDateTime occurredAt) throws IOException, SQLException {
        String sql = "INSERT INTO wastes (raw_material_id, quantity, cost, reason, occurred_at) VALUES ((SELECT id FROM raw_materials WHERE code = ?), ?, ?, ?, ?)";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, rawMaterialCode);
            statement.setBigDecimal(2, quantity);
            statement.setBigDecimal(3, cost);
            statement.setString(4, reason);
            statement.setTimestamp(5, Timestamp.valueOf(occurredAt));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
            throw new SQLException("İtki qeydinin identifikatoru alınmadı.");
        }
    }

    public static List<WasteEntry> loadWastes() throws IOException, SQLException {
        List<WasteEntry> entries = new ArrayList<>();
        String sql = "SELECT w.id, r.code AS material_code, r.name AS material_name, r.unit, w.quantity, w.cost, w.reason, w.occurred_at "
                + "FROM wastes w JOIN raw_materials r ON r.id = w.raw_material_id ORDER BY w.occurred_at";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                entries.add(new WasteEntry(result.getLong("id"), result.getString("material_code"), result.getString("material_name"), result.getString("unit"),
                        result.getBigDecimal("quantity"), result.getBigDecimal("cost"), result.getString("reason"), result.getTimestamp("occurred_at").toLocalDateTime()));
            }
        }
        return entries;
    }

    public static void deleteWaste(long id) throws IOException, SQLException {
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement("DELETE FROM wastes WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    private static void deleteByCode(String table, String code) throws IOException, SQLException {
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement("DELETE FROM " + table + " WHERE code = ?")) {
            statement.setString(1, code);
            statement.executeUpdate();
        }
    }

    private static final class ProductEntryBuilder {
        private final String code;
        private final String name;
        private final BigDecimal quantity;
        private final BigDecimal deliveryPrice;
        private final BigDecimal unitCost;
        private final List<RecipeEntry> recipe = new ArrayList<>();
        private ProductEntryBuilder(String code, String name, BigDecimal quantity, BigDecimal deliveryPrice, BigDecimal unitCost) {
            this.code = code;
            this.name = name;
            this.quantity = quantity;
            this.deliveryPrice = deliveryPrice;
            this.unitCost = unitCost;
        }
    }

    private static Properties settings() throws IOException {
        if (!Files.exists(CONFIG_FILE)) throw new IOException("Database sazlama faylı tapılmadı: " + CONFIG_FILE);
        Properties settings = new Properties();
        try (InputStream input = Files.newInputStream(CONFIG_FILE)) {
            settings.load(input);
        }
        return settings;
    }

    public static long recordInvoice(InvoiceEntry entry) throws IOException, SQLException {
        String sql = "INSERT INTO invoices (invoice_no, delivery_doc_no, issue_date, seller_name, seller_voen, seller_address, seller_bank_details, customer_name, customer_voen, payment_type, vat_rate, subtotal, vat_amount, total_amount, notes) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, entry.invoiceNo());
                stmt.setString(2, entry.deliveryDocNo());
                stmt.setTimestamp(3, Timestamp.valueOf(entry.issueDate()));
                stmt.setString(4, entry.sellerName());
                stmt.setString(5, entry.sellerVoen());
                stmt.setString(6, entry.sellerAddress());
                stmt.setString(7, entry.sellerBankDetails());
                stmt.setString(8, entry.customerName());
                stmt.setString(9, entry.customerVoen());
                stmt.setString(10, entry.paymentType());
                stmt.setBigDecimal(11, entry.vatRate());
                stmt.setBigDecimal(12, entry.subtotal());
                stmt.setBigDecimal(13, entry.vatAmount());
                stmt.setBigDecimal(14, entry.totalAmount());
                stmt.setString(15, entry.notes());
                stmt.executeUpdate();

                long invoiceId;
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        invoiceId = keys.getLong(1);
                    } else {
                        try (PreparedStatement find = connection.prepareStatement("SELECT id FROM invoices WHERE invoice_no = ?")) {
                            find.setString(1, entry.invoiceNo());
                            try (ResultSet rs = find.executeQuery()) {
                                if (rs.next()) invoiceId = rs.getLong(1);
                                else throw new SQLException("Fatura ID tapılmadı.");
                            }
                        }
                    }
                }

                if (entry.items() != null) {
                    String itemSql = "INSERT INTO invoice_items (invoice_id, item_name, unit, quantity, unit_price, line_total) VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement itemStmt = connection.prepareStatement(itemSql)) {
                        for (InvoiceEntry.InvoiceItem item : entry.items()) {
                            itemStmt.setLong(1, invoiceId);
                            itemStmt.setString(2, item.itemName());
                            itemStmt.setString(3, item.unit() == null ? "ədəd" : item.unit());
                            itemStmt.setBigDecimal(4, item.quantity());
                            itemStmt.setBigDecimal(5, item.unitPrice());
                            itemStmt.setBigDecimal(6, item.lineTotal());
                            itemStmt.executeUpdate();
                        }
                    }
                }
                connection.commit();
                return invoiceId;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public static List<InvoiceEntry> loadInvoices() throws IOException, SQLException {
        List<InvoiceEntry> list = new ArrayList<>();
        String sql = "SELECT id, invoice_no, delivery_doc_no, issue_date, seller_name, seller_voen, seller_address, seller_bank_details, customer_name, customer_voen, payment_type, vat_rate, subtotal, vat_amount, total_amount, notes FROM invoices ORDER BY issue_date DESC";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql); ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                long invoiceId = rs.getLong("id");
                List<InvoiceEntry.InvoiceItem> items = new ArrayList<>();
                try (PreparedStatement itemStmt = connection.prepareStatement("SELECT id, item_name, unit, quantity, unit_price, line_total FROM invoice_items WHERE invoice_id = ? ORDER BY id")) {
                    itemStmt.setLong(1, invoiceId);
                    try (ResultSet itemRs = itemStmt.executeQuery()) {
                        while (itemRs.next()) {
                            items.add(new InvoiceEntry.InvoiceItem(
                                    itemRs.getLong("id"),
                                    itemRs.getString("item_name"),
                                    itemRs.getString("unit"),
                                    itemRs.getBigDecimal("quantity"),
                                    itemRs.getBigDecimal("unit_price"),
                                    itemRs.getBigDecimal("line_total")
                            ));
                        }
                    }
                }
                list.add(new InvoiceEntry(
                        invoiceId,
                        rs.getString("invoice_no"),
                        rs.getString("delivery_doc_no"),
                        rs.getTimestamp("issue_date").toLocalDateTime(),
                        rs.getString("seller_name"),
                        rs.getString("seller_voen"),
                        rs.getString("seller_address"),
                        rs.getString("seller_bank_details"),
                        rs.getString("customer_name"),
                        rs.getString("customer_voen"),
                        rs.getString("payment_type"),
                        rs.getBigDecimal("vat_rate"),
                        rs.getBigDecimal("subtotal"),
                        rs.getBigDecimal("vat_amount"),
                        rs.getBigDecimal("total_amount"),
                        rs.getString("notes"),
                        items
                ));
            }
        }
        return list;
    }

    public static void deleteInvoice(long id) throws IOException, SQLException {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try (PreparedStatement itemStmt = connection.prepareStatement("DELETE FROM invoice_items WHERE invoice_id = ?");
                 PreparedStatement invStmt = connection.prepareStatement("DELETE FROM invoices WHERE id = ?")) {
                itemStmt.setLong(1, id);
                itemStmt.executeUpdate();
                invStmt.setLong(1, id);
                invStmt.executeUpdate();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public record ActivityLogEntry(long id, String category, String description, LocalDateTime occurredAt) { }
    public record CashMovementEntry(long id, String direction, BigDecimal amount, String description, LocalDateTime occurredAt) { }
    public record PaymentEntry(long id, String paymentType, String personName, String customerCode, String supplierCode, BigDecimal amount, String managerCode, LocalDateTime occurredAt) { }
    public record ManagerEntry(String code, String name, BigDecimal commissionRate) { }
    public record CustomerEntry(String code, String name, String phone, BigDecimal debt, String managerCode) { }
    public record SupplierEntry(String code, String name, String phone, BigDecimal debt) { }
    public record RawMaterialEntry(String code, String name, String unit, BigDecimal quantity, BigDecimal unitCost) { }
    public record RecipeEntry(String rawMaterialCode, BigDecimal quantityPerProduct) { }
    public record ProductEntry(String code, String name, BigDecimal quantity, BigDecimal deliveryPrice, BigDecimal unitCost, List<RecipeEntry> recipe) { }
    public record WasteEntry(long id, String rawMaterialCode, String materialName, String unit, BigDecimal quantity, BigDecimal cost, String reason, LocalDateTime occurredAt) { }
    public record DeliveryEntry(String documentNo, String customerCode, String customerName, String productCode, String productName,
                                BigDecimal quantity, BigDecimal total, BigDecimal cost, BigDecimal paidAmount,
                                BigDecimal remainingDebt, String managerCode, String managerName, LocalDateTime occurredAt) { }
    public record MonthlyProductReportEntry(String monthKey, String productCode, String productName, BigDecimal totalQuantity,
                                           BigDecimal totalAmount, int deliveryCount) { }
    public record MonthlyRawMaterialReportEntry(String monthKey, String materialCode, String materialName, String unit,
                                               BigDecimal totalQuantity, BigDecimal totalAmount, BigDecimal paidAmount,
                                               BigDecimal debtAmount, int purchaseCount) { }
    public record PurchaseEntry(long id, String documentNo, String supplierCode, String supplierName,
                               String materialCode, String materialName, String unit,
                               BigDecimal quantity, BigDecimal totalAmount, BigDecimal paidAmount,
                               BigDecimal debtAmount, LocalDateTime occurredAt) { }
    public record EmployeeEntry(String code, String name, String position, String phone, BigDecimal monthlySalary) { }
    public record SalaryEntry(long id, String employeeCode, String employeeName, String position, BigDecimal amount, String paymentType, String note, LocalDateTime occurredAt) { }
    public record MonthlySalaryReportEntry(String monthKey, String employeeCode, String employeeName, String position, BigDecimal monthlySalary, BigDecimal totalPaid, BigDecimal advancePaid, BigDecimal bonusPaid, int paymentCount) { }
    public record CreditEntry(String code, String name, String bankOrCreditor, BigDecimal totalAmount, BigDecimal monthlyAmount, BigDecimal remainingBalance) { }
    public record CreditPaymentEntry(long id, String creditCode, String creditName, String bankOrCreditor, BigDecimal monthlyInstallment, BigDecimal paidAmount, String note, LocalDateTime occurredAt) { }
    public record MonthlyCreditReportEntry(String monthKey, String creditCode, String creditName, String bankOrCreditor, BigDecimal monthlyInstallment, BigDecimal totalPaid, BigDecimal remainingBalance, int paymentCount) { }
}
