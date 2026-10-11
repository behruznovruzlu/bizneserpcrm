package az.company.erp;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ErpCrmApplication extends Application {
    private final BorderPane layout = new BorderPane();
    private final Label pageTitle = new Label();
    private final Label dateTimeLabel = new Label();
    private Timeline clock;

    private final ObservableList<Manager> managers = FXCollections.observableArrayList();
    private final ObservableList<Customer> customers = FXCollections.observableArrayList();
    private final ObservableList<RawMaterial> rawMaterials = FXCollections.observableArrayList();
    private final ObservableList<Product> products = FXCollections.observableArrayList();
    private final ObservableList<Supplier> suppliers = FXCollections.observableArrayList();
    private final ObservableList<Sale> sales = FXCollections.observableArrayList();
    private final ObservableList<Payment> payments = FXCollections.observableArrayList();
    private final ObservableList<WasteRecord> wastes = FXCollections.observableArrayList();
    private final ObservableList<ActivityLog> activityLogs = FXCollections.observableArrayList();
    private final ObservableList<CashMovement> cashMovements = FXCollections.observableArrayList();
    private final ObservableList<Purchase> purchases = FXCollections.observableArrayList();
    private final ObservableList<Employee> employees = FXCollections.observableArrayList();
    private final ObservableList<SalaryRecord> salaryRecords = FXCollections.observableArrayList();
    private final ObservableList<CreditContract> credits = FXCollections.observableArrayList();
    private final ObservableList<CreditPaymentRecord> creditPayments = FXCollections.observableArrayList();

    @Override
    public void start(Stage stage) {
        layout.setLeft(createSidebar());
        layout.setTop(createHeader());
        showDashboard();

        Scene scene = new Scene(layout, 1300, 780);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());

        stage.setTitle("Biznes İdarəetmə Sistemi");
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.setScene(scene);
        stage.show();
        startClock();
        initializeDatabase();
    }

    private ScrollPane createSidebar() {
        Label logo = new Label("Biznes İdarəetmə");
        logo.getStyleClass().add("logo");
        Label subtitle = new Label("Mal təhvili • İstehsal • Anbar");
        subtitle.getStyleClass().add("logo-subtitle");

        VBox sidebar = new VBox(8, logo, subtitle);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(28, 18, 28, 18));

        String[][] sections = {
                {"İDARƏETMƏ", "İdarə paneli", "Aylıq hesabatlar", "Əməliyyat jurnalı"},
                {"TƏRƏFDAŞLAR", "Müştərilər", "Menecerlər", "Təchizatçılar", "İşçilər"},
                {"ANBAR VƏ İSTEHSAL", "Xammallar", "Məhsullar", "İstehsal", "Anbar", "İtkilər"},
                {"MALİYYƏ", "Mal təhvili", "Ödənişlər", "Faturalar (İnvoice)", "Maaşlar", "Kredit ödənişləri"}
        };
        for (String[] section : sections) {
            Label sectionTitle = new Label(section[0]);
            sectionTitle.getStyleClass().add("nav-section-title");
            sidebar.getChildren().add(sectionTitle);
            for (int i = 1; i < section.length; i++) {
                String item = section[i];
                Button button = new Button(item);
                button.getStyleClass().add("nav-button");
                button.setMaxWidth(Double.MAX_VALUE);
                button.setOnAction(event -> openSection(item));
                sidebar.getChildren().add(button);
            }
        }
        ScrollPane scroll = new ScrollPane(sidebar);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("sidebar-scroll");
        return scroll;
    }

    private void openSection(String item) {
        if (item.equals("İdarə paneli")) showDashboard();
        else if (item.equals("Aylıq hesabatlar")) showMonthlyReports();
        else if (item.equals("Müştərilər")) showCustomers();
        else if (item.equals("Menecerlər")) showManagers();
        else if (item.equals("Təchizatçılar")) showSuppliers();
        else if (item.equals("İşçilər")) showEmployees();
        else if (item.equals("Xammallar")) showRawMaterials();
        else if (item.equals("Məhsullar")) showProducts();
        else if (item.equals("İstehsal")) showProduction();
        else if (item.equals("Mal təhvili")) showSales();
        else if (item.equals("Ödənişlər")) showPayments();
        else if (item.equals("Faturalar (İnvoice)")) showInvoices();
        else if (item.equals("Maaşlar")) showSalaries();
        else if (item.equals("Kredit ödənişləri")) showCreditPayments();
        else if (item.equals("Anbar")) showWarehouse();
        else if (item.equals("İtkilər")) showWastes();
        else if (item.equals("Əməliyyat jurnalı")) showActivityLog();
        else showPlaceholder(item);
    }

    private void setPageContent(Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        if (content instanceof Region region) {
            region.setMaxHeight(Double.MAX_VALUE);
        }
        scroll.getStyleClass().add("page-scroll");
        layout.setCenter(scroll);
    }

    private HBox createHeader() {
        pageTitle.getStyleClass().add("page-title");
        Label userTitle = new Label("İstifadəçi");
        userTitle.getStyleClass().add("user-title");
        Label user = new Label("İstifadəçi");
        user.getStyleClass().add("user-label");
        VBox userInfo = new VBox(3, userTitle, user);
        userInfo.setAlignment(Pos.CENTER_RIGHT);
        dateTimeLabel.getStyleClass().add("date-time-label");
        VBox dateInfo = new VBox(dateTimeLabel);
        dateInfo.setAlignment(Pos.CENTER_RIGHT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(28, pageTitle, spacer, dateInfo, userInfo);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 30, 20, 30));
        header.getStyleClass().add("header");
        return header;
    }

    private void startClock() {
        clock = new Timeline(new KeyFrame(Duration.ZERO, event -> updateClock()),
                new KeyFrame(Duration.seconds(1), event -> updateClock()));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    private void updateClock() {
        String[] months = {"yanvar", "fevral", "mart", "aprel", "may", "iyun", "iyul", "avqust", "sentyabr", "oktyabr", "noyabr", "dekabr"};
        LocalDateTime now = LocalDateTime.now();
        dateTimeLabel.setText(now.getDayOfMonth() + " " + months[now.getMonthValue() - 1] + " " + now.getYear()
                + "  •  " + now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    private void initializeDatabase() {
        if (!Database.isConfigured()) return;
        try {
            Database.initializeSchema();
            loadPersistentPartners();
            loadPersistentInventory();
            loadPersistentHistory();
            showDashboard();
            if (Database.isSqlite()) {
                System.out.println("Verilenler bazasi (SQLite) hazirdir.");
            } else {
                System.out.println("Verilenler bazasi (PostgreSQL) hazirdir.");
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            System.err.println("Baza baglantisi qurulmadi: " + exception.getMessage());
        }
    }

    private void loadPersistentHistory() throws Exception {
        activityLogs.clear();
        for (Database.ActivityLogEntry entry : Database.loadActivityLogs()) {
            activityLogs.add(new ActivityLog(entry.id(), entry.category(), entry.description(), formatTimestamp(entry.occurredAt())));
        }
        cashMovements.clear();
        for (Database.CashMovementEntry entry : Database.loadCashMovements()) {
            cashMovements.add(new CashMovement(entry.id(), CashDirection.valueOf(entry.direction()), entry.amount(), entry.description(),
                    formatTimestamp(entry.occurredAt())));
        }
        sales.clear();
        for (Database.DeliveryEntry entry : Database.loadDeliveries()) {
            BigDecimal profit = entry.total().subtract(entry.cost());
            sales.add(new Sale(
                    entry.documentNo(),
                    entry.customerCode(),
                    entry.customerName(),
                    entry.productCode(),
                    entry.productName(),
                    entry.quantity(),
                    entry.total(),
                    entry.cost(),
                    entry.paidAmount(),
                    entry.remainingDebt(),
                    entry.managerCode(),
                    entry.managerName(),
                    formatTimestamp(entry.occurredAt())));
        }
        payments.clear();
        for (Database.PaymentEntry entry : Database.loadPayments()) {
            payments.add(new Payment(
                    entry.id(),
                    entry.paymentType(),
                    entry.customerCode(),
                    entry.supplierCode(),
                    entry.personName(),
                    entry.amount(),
                    entry.managerCode(),
                    formatTimestamp(entry.occurredAt())));
        }
        purchases.clear();
        for (Database.PurchaseEntry entry : Database.loadPurchases()) {
            purchases.add(new Purchase(
                    entry.id(),
                    entry.documentNo(),
                    entry.supplierCode(),
                    entry.supplierName(),
                    entry.materialCode(),
                    entry.materialName(),
                    entry.unit(),
                    entry.quantity(),
                    entry.totalAmount(),
                    entry.paidAmount(),
                    entry.debtAmount(),
                    formatTimestamp(entry.occurredAt())));
        }
        salaryRecords.clear();
        for (Database.SalaryEntry entry : Database.loadSalaries()) {
            salaryRecords.add(new SalaryRecord(
                    entry.id(),
                    entry.employeeCode(),
                    entry.employeeName(),
                    entry.position(),
                    entry.amount(),
                    entry.paymentType(),
                    entry.note(),
                    formatTimestamp(entry.occurredAt())
            ));
        }
        creditPayments.clear();
        for (Database.CreditPaymentEntry entry : Database.loadCreditPayments()) {
            creditPayments.add(new CreditPaymentRecord(
                    entry.id(),
                    entry.creditCode(),
                    entry.creditName(),
                    entry.bankOrCreditor(),
                    entry.monthlyInstallment(),
                    entry.paidAmount(),
                    entry.note(),
                    formatTimestamp(entry.occurredAt())
            ));
        }
    }

    private void loadPersistentPartners() throws Exception {
        List<Database.ManagerEntry> savedManagers = Database.loadManagers();
        List<Database.CustomerEntry> savedCustomers = Database.loadCustomers();
        List<Database.SupplierEntry> savedSuppliers = Database.loadSuppliers();
        List<Database.EmployeeEntry> savedEmployees = Database.loadEmployees();
        List<Database.CreditEntry> savedCredits = Database.loadCredits();

        managers.clear();
        savedManagers.forEach(entry -> managers.add(new Manager(entry.code(), entry.name(), entry.commissionRate())));

        customers.clear();
        savedCustomers.forEach(entry -> customers.add(new Customer(entry.code(), entry.name(), entry.phone(),
                formatMoney(entry.debt()), entry.managerCode())));

        suppliers.clear();
        savedSuppliers.forEach(entry -> suppliers.add(new Supplier(entry.code(), entry.name(), entry.phone(), entry.debt())));

        employees.clear();
        savedEmployees.forEach(entry -> employees.add(new Employee(entry.code(), entry.name(), entry.position(), entry.phone(), entry.monthlySalary())));

        credits.clear();
        savedCredits.forEach(entry -> credits.add(new CreditContract(entry.code(), entry.name(), entry.bankOrCreditor(), entry.totalAmount(), entry.monthlyAmount(), entry.remainingBalance())));
    }

    private void persistAllPartners() throws Exception {
        for (Manager manager : managers) Database.upsertManager(new Database.ManagerEntry(manager.code(), manager.name(), manager.commissionRate()));
        for (Customer customer : customers) Database.upsertCustomer(new Database.CustomerEntry(customer.code(), customer.name(), customer.phone(), customerDebt(customer), customer.managerCode()));
        for (Supplier supplier : suppliers) Database.upsertSupplier(new Database.SupplierEntry(supplier.code(), supplier.name(), supplier.phone(), supplier.debt()));
        for (Employee employee : employees) Database.upsertEmployee(new Database.EmployeeEntry(employee.code(), employee.name(), employee.position(), employee.phone(), employee.monthlySalary()));
        for (CreditContract credit : credits) Database.upsertCredit(new Database.CreditEntry(credit.code(), credit.name(), credit.bankOrCreditor(), credit.totalAmount(), credit.monthlyAmount(), credit.remainingBalance()));
    }

    private void persistCredit(CreditContract credit) {
        if (!Database.isConfigured()) return;
        try {
            Database.upsertCredit(new Database.CreditEntry(credit.code(), credit.name(), credit.bankOrCreditor(), credit.totalAmount(), credit.monthlyAmount(), credit.remainingBalance()));
        } catch (Exception exception) {
            System.err.println("Kredit bazaya yazılmadı: " + exception.getMessage());
        }
    }

    private void persistEmployee(Employee employee) {
        if (!Database.isConfigured()) return;
        try {
            Database.upsertEmployee(new Database.EmployeeEntry(employee.code(), employee.name(), employee.position(), employee.phone(), employee.monthlySalary()));
        } catch (Exception exception) {
            System.err.println("İşçi bazaya yazılmadı: " + exception.getMessage());
        }
    }

    private void persistCustomer(Customer customer) {
        try {
            Database.upsertCustomer(new Database.CustomerEntry(customer.code(), customer.name(), customer.phone(), customerDebt(customer), customer.managerCode()));
        } catch (Exception exception) {
            System.err.println("Müştəri bazaya yazılmadı: " + exception.getMessage());
        }
    }

    private void persistSupplier(Supplier supplier) {
        try {
            Database.upsertSupplier(new Database.SupplierEntry(supplier.code(), supplier.name(), supplier.phone(), supplier.debt()));
        } catch (Exception exception) {
            System.err.println("Təchizatçı bazaya yazılmadı: " + exception.getMessage());
        }
    }

    private void loadPersistentInventory() throws Exception {
        List<Database.RawMaterialEntry> savedMaterials = Database.loadRawMaterials();
        List<Database.ProductEntry> savedProducts = Database.loadProducts();

        rawMaterials.clear();
        savedMaterials.forEach(entry -> rawMaterials.add(new RawMaterial(entry.code(), entry.name(), entry.unit(), entry.quantity(), entry.unitCost())));

        products.clear();
        savedProducts.forEach(entry -> {
            List<FormulaItem> recipe = entry.recipe().stream().map(item -> new FormulaItem(item.rawMaterialCode(), item.quantityPerProduct())).toList();
            products.add(new Product(entry.code(), entry.name(), recipe, entry.quantity(), entry.deliveryPrice(), entry.unitCost()));
        });

        wastes.clear();
        Database.loadWastes().forEach(entry -> wastes.add(new WasteRecord(entry.id(), entry.rawMaterialCode(), entry.materialName(), entry.unit(), entry.quantity(),
                entry.cost(), entry.reason(), formatTimestamp(entry.occurredAt()))));
    }

    private void persistInventory() throws Exception {
        for (RawMaterial material : rawMaterials) {
            Database.upsertRawMaterial(new Database.RawMaterialEntry(material.code(), material.name(), material.unit(), material.stock(), material.unitCost()));
        }
        for (Product product : products) {
            List<Database.RecipeEntry> recipe = product.recipe().stream().map(item -> new Database.RecipeEntry(item.materialCode(), item.amountPerProduct())).toList();
            Database.upsertProduct(new Database.ProductEntry(product.code(), product.name(), product.stock(), product.salePrice(), product.unitCost(), recipe));
        }
    }

    private void persistInventoryQuietly() {
        try {
            persistInventory();
        } catch (Exception exception) {
            System.err.println("Anbar məlumatı bazaya yazılmadı: " + exception.getMessage());
        }
    }

    private void showDashboard() {
        pageTitle.setText("İdarə paneli");
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        // 1. Təhvil göstəriciləri
        BigDecimal todaySales = sales.stream()
                .filter(sale -> sale.createdAt().startsWith(today))
                .map(Sale::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSalesValue = sales.stream()
                .map(Sale::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Pul Axını və Mənfəət göstəriciləri (Əsas Mənfəət = Daxil olan pul - Çıxan pul)
        BigDecimal totalCashIncome = cashMovements.stream()
                .filter(movement -> movement.direction() == CashDirection.INCOME)
                .map(CashMovement::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCashExpense = cashMovements.stream()
                .filter(movement -> movement.direction() == CashDirection.EXPENSE)
                .map(CashMovement::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalProfit = totalCashIncome.subtract(totalCashExpense);

        // 3. Borc və Müştəri göstəriciləri
        BigDecimal totalCustomerDebt = customers.stream()
                .map(this::customerDebt)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSupplierDebt = suppliers.stream()
                .map(Supplier::debt)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. Cari Ay Göstəriciləri
        String currentMonth = currentMonthKey();
        BigDecimal monthlyIncome = cashMovements.stream()
                .filter(movement -> monthKey(movement).equals(currentMonth) && movement.direction() == CashDirection.INCOME)
                .map(CashMovement::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthlyExpense = cashMovements.stream()
                .filter(movement -> monthKey(movement).equals(currentMonth) && movement.direction() == CashDirection.EXPENSE)
                .map(CashMovement::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthlyDeliveryValue = sales.stream()
                .filter(sale -> monthKey(sale.createdAt()).equals(currentMonth))
                .map(Sale::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        VBox content = new VBox(22);
        content.getStyleClass().add("content");

        Label sectionHeader = new Label("Şirkətin Maliyyə və Cari Vəziyyəti");
        sectionHeader.getStyleClass().add("section-title");

        HBox primaryCards = new HBox(16,
                createStatCard("Ümumi Mənfəət", formatMoney(totalProfit), "Daxil: +" + formatMoney(totalCashIncome) + " | Çıxan: -" + formatMoney(totalCashExpense)),
                createStatCard("Cəmi Daxil Olan Pul", formatMoney(totalCashIncome), "Kassaya gələn ödənişlər"),
                createStatCard("Cəmi Çıxan Pul", formatMoney(totalCashExpense), "Kassadan çıxan xərclər"),
                createStatCard("Ümumi Təhvil Dəyəri", formatMoney(totalSalesValue), "Bugünkü təhvil: " + formatMoney(todaySales)),
                createStatCard("Müştəri Borcları", formatMoney(totalCustomerDebt), "Alınacaq borc"),
                createStatCard("Bizim Borcumuz", formatMoney(totalSupplierDebt), "Təchizatçıya borc"));

        Label monthlyTitle = new Label(currentMonthTitle() + " — aylıq fəaliyyət və pul axını");
        monthlyTitle.getStyleClass().add("section-title");

        HBox monthlyCards = new HBox(16,
                createStatCard("Aylıq Mənfəət", formatMoney(monthlyIncome.subtract(monthlyExpense)), "Ayın xalis pul qalığı"),
                createStatCard("Aylıq Daxil Olan Pul", formatMoney(monthlyIncome)),
                createStatCard("Aylıq Çıxan Pul", formatMoney(monthlyExpense)),
                createStatCard("Aylıq Təhvil Dəyəri", formatMoney(monthlyDeliveryValue)));

        Label explanation = new Label("• Ümumi Mənfəət birbaşa daxil olan ödənişlərlə çıxan pulların fərqidir (Daxil olan pul − Çıxan pul). Hər yeni ödəniş daxil olduqda və ya xərc qeydə alındıqda mənfəət anında yenilənir.");
        explanation.getStyleClass().add("transaction-detail");
        explanation.setWrapText(true);

        content.getChildren().addAll(sectionHeader, primaryCards, monthlyTitle, monthlyCards, explanation);
        setPageContent(content);
    }

    private VBox createStatCard(String title, String value) {
        return createStatCard(title, value, null);
    }

    private VBox createStatCard(String title, String value, String subtitle) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("card-value");

        VBox card = new VBox(8, titleLabel, valueLabel);
        if (subtitle != null && !subtitle.isBlank()) {
            Label subLabel = new Label(subtitle);
            subLabel.getStyleClass().add("card-subtitle");
            card.getChildren().add(subLabel);
        }
        card.getStyleClass().add("stat-card");
        card.setPrefWidth(240);
        return card;
    }

    private void showMonthlyReports() {
        String currentMonth = currentMonthKey();
        List<String> months = availableReportMonths();
        String selectedMonth = months.contains(currentMonth) ? currentMonth : (months.isEmpty() ? currentMonth : months.getFirst());
        showMonthlyReports(selectedMonth);
    }

    private void showMonthlyReports(String month) {
        pageTitle.setText("Aylıq hesabatlar");
        ComboBox<String> selector = new ComboBox<>();
        selector.getItems().addAll(availableReportMonths());
        if (!selector.getItems().contains(month)) selector.getItems().add(month);
        selector.setValue(month);
        selector.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(String item) { return item == null ? "" : monthLabel(item); }
            @Override public String fromString(String text) { return text; }
        });
        selector.setOnAction(event -> showMonthlyReports(selector.getValue()));

        // Cash flow for selected month
        BigDecimal income = cashMovements.stream()
                .filter(movement -> monthKey(movement).equals(month) && movement.direction() == CashDirection.INCOME)
                .map(CashMovement::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expense = cashMovements.stream()
                .filter(movement -> monthKey(movement).equals(month) && movement.direction() == CashDirection.EXPENSE)
                .map(CashMovement::amount).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Deliveries for selected month
        List<Sale> monthSales = sales.stream()
                .filter(sale -> monthKey(sale.createdAt()).equals(month))
                .toList();

        BigDecimal deliveriesTotal = monthSales.stream()
                .map(Sale::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        long deliveriesCount = monthSales.size();

        long productionsCount = activityLogs.stream()
                .filter(log -> monthKey(log).equals(month) && log.type().equals("İstehsal")).count();

        // Active wastes for the selected month
        long lossesCount = wastes.stream()
                .filter(waste -> monthKey(waste.createdAt()).equals(month)).count();
        BigDecimal lossesCost = wastes.stream()
                .filter(waste -> monthKey(waste.createdAt()).equals(month))
                .map(WasteRecord::cost).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Purchases for selected month (Borcla və nağd alınan xammallar)
        List<Purchase> monthPurchases = purchases.stream()
                .filter(purchase -> monthKey(purchase.createdAt()).equals(month))
                .toList();

        BigDecimal totalPurchasesAmount = monthPurchases.stream()
                .map(Purchase::totalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPurchasesDebt = monthPurchases.stream()
                .map(Purchase::debtAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPurchasesPaid = monthPurchases.stream()
                .map(Purchase::paidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Salary payments for selected month
        List<SalaryRecord> monthSalaries = salaryRecords.stream()
                .filter(salary -> monthKey(salary.createdAt()).equals(month))
                .toList();

        BigDecimal totalSalariesAmount = monthSalaries.stream()
                .map(SalaryRecord::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAdvanceSalaries = monthSalaries.stream()
                .filter(s -> s.type().contains("Tez") || s.type().contains("Avans"))
                .map(SalaryRecord::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalBonusSalaries = monthSalaries.stream()
                .filter(s -> s.type().contains("Əlavə") || s.type().contains("Bonus") || s.type().contains("Mükafat"))
                .map(SalaryRecord::amount).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Credit payments for selected month
        List<CreditPaymentRecord> monthCreditPayments = creditPayments.stream()
                .filter(cp -> monthKey(cp.createdAt()).equals(month))
                .toList();
        BigDecimal totalCreditPaid = monthCreditPayments.stream()
                .map(CreditPaymentRecord::paidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalMonthlyCreditObligation = credits.stream()
                .map(CreditContract::monthlyAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        Label title = new Label(monthLabel(month) + " üçün maliyyə və əməliyyat hesabatı");
        title.getStyleClass().add("section-title");

        HBox cashCards = new HBox(16,
                createStatCard("Aylıq Mənfəət", formatMoney(income.subtract(expense)), "Daxil olan − Çıxan pul"),
                createStatCard("Daxil olan pul", formatMoney(income)),
                createStatCard("Çıxan pul", formatMoney(expense)),
                createStatCard("Təhvil dövriyyəsi", formatMoney(deliveriesTotal), deliveriesCount + " əməliyyat"));

        HBox operationCards = new HBox(16,
                createStatCard("Borclu Xammal Alışı", formatMoney(totalPurchasesDebt), "Ümumi xammal alışı: " + formatMoney(totalPurchasesAmount)),
                createStatCard("Maaş, Avans Və Bonus", formatMoney(totalSalariesAmount), "Avans: " + formatMoney(totalAdvanceSalaries) + " • Bonus: " + formatMoney(totalBonusSalaries)),
                createStatCard("Aylıq Kredit Ödənişi", formatMoney(totalCreditPaid), "Aylıq öhdəlik: " + formatMoney(totalMonthlyCreditObligation)),
                createStatCard("İstehsal sayı", String.valueOf(productionsCount) + " əməliyyat"),
                createStatCard("İtki qeydləri", formatMoney(lossesCost), lossesCount + " qeyd"));

        // Product-wise sales breakdown for this month (Direct PostgreSQL Query + in-memory fallback)
        List<MonthlyProductSale> productSales = new ArrayList<>();
        if (Database.isConfigured()) {
            try {
                for (Database.MonthlyProductReportEntry entry : Database.loadMonthlyProductSales(month)) {
                    productSales.add(new MonthlyProductSale(
                            entry.productCode(),
                            entry.productName(),
                            entry.totalQuantity(),
                            entry.totalAmount(),
                            entry.deliveryCount()
                    ));
                }
            } catch (Exception exception) {
                System.err.println("Aylıq məhsul bölgüsü bazadan oxunmadı: " + exception.getMessage());
            }
        }
        if (productSales.isEmpty() && !monthSales.isEmpty()) {
            java.util.Map<String, List<Sale>> salesByProduct = monthSales.stream()
                    .collect(java.util.stream.Collectors.groupingBy(Sale::productName));
            for (java.util.Map.Entry<String, List<Sale>> entry : salesByProduct.entrySet()) {
                String pName = entry.getKey();
                List<Sale> list = entry.getValue();
                String pCode = list.getFirst().productCode();
                BigDecimal totalQty = list.stream().map(Sale::quantity).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalAmt = list.stream().map(Sale::total).reduce(BigDecimal.ZERO, BigDecimal::add);
                productSales.add(new MonthlyProductSale(pCode, pName, totalQty, totalAmt, list.size()));
            }
            productSales.sort(Comparator.comparing(MonthlyProductSale::totalAmount).reversed());
        }

        ObservableList<MonthlyProductSale> observableProductSales = FXCollections.observableArrayList(productSales);

        Label productSalesTitle = new Label("Bu ay satılan məhsulların icmalı (" + productSales.size() + " növ məhsul)");
        productSalesTitle.getStyleClass().add("section-title");

        TextField searchProduct = new TextField();
        searchProduct.setPromptText("Məhsul adı və ya kodu ilə süzün...");
        searchProduct.getStyleClass().add("search-field");
        searchProduct.textProperty().addListener((obs, oldVal, newVal) -> {
            String q = newVal == null ? "" : newVal.trim().toLowerCase();
            observableProductSales.clear();
            if (q.isEmpty()) {
                observableProductSales.addAll(productSales);
            } else {
                for (MonthlyProductSale ps : productSales) {
                    if (ps.productName().toLowerCase().contains(q) || ps.productCode().toLowerCase().contains(q)) {
                        observableProductSales.add(ps);
                    }
                }
            }
        });

        TableView<MonthlyProductSale> productTable = new TableView<>(observableProductSales);
        productTable.setPlaceholder(new Label("Bu ay üçün heç bir mal təhvili edilməyib."));
        productTable.getColumns().addAll(
                column("Kod", MonthlyProductSale::productCode),
                column("Məhsul adı", MonthlyProductSale::productName),
                column("Satılan say", ps -> formatQuantity(ps.totalQuantity()) + " ədəd"),
                column("Təhvil məbləği", ps -> formatMoney(ps.totalAmount())),
                column("Əməliyyat sayı", ps -> ps.deliveryCount() + " təhvil")
        );
        productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        productTable.setPrefHeight(280);
        productTable.setMinHeight(200);
        productTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && productTable.getSelectionModel().getSelectedItem() != null) {
                manageProductDeliveries(productTable.getSelectionModel().getSelectedItem(), month);
            }
        });
        VBox.setVgrow(productTable, Priority.ALWAYS);

        Button manageProductBtn = new Button("🛠️ Əməliyyat tarixçəsi / Səhv təhvili sil");
        manageProductBtn.getStyleClass().add("secondary-button");
        manageProductBtn.disableProperty().bind(productTable.getSelectionModel().selectedItemProperty().isNull());
        manageProductBtn.setOnAction(event -> manageProductDeliveries(productTable.getSelectionModel().getSelectedItem(), month));

        HBox productHeaderRow = new HBox(12, productSalesTitle, manageProductBtn);
        HBox.setHgrow(productSalesTitle, Priority.ALWAYS);
        productHeaderRow.setAlignment(Pos.CENTER_LEFT);

        BigDecimal totalMonthQuantity = productSales.stream().map(MonthlyProductSale::totalQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        Label summaryFooter = new Label("Cəmi satılan məhsul miqdarı: " + formatQuantity(totalMonthQuantity) + " ədəd  •  Cəmi təhvil məbləği: " + formatMoney(deliveriesTotal));
        summaryFooter.getStyleClass().add("transaction-detail");

        VBox productSection = new VBox(10, productHeaderRow, searchProduct, productTable, summaryFooter);

        // Raw material purchases breakdown for this month (Direct PostgreSQL Query + in-memory fallback)
        List<MonthlyRawMaterialPurchase> rawMaterialPurchases = new ArrayList<>();
        if (Database.isConfigured()) {
            try {
                for (Database.MonthlyRawMaterialReportEntry entry : Database.loadMonthlyRawMaterialPurchases(month)) {
                    rawMaterialPurchases.add(new MonthlyRawMaterialPurchase(
                            entry.materialCode(),
                            entry.materialName(),
                            entry.unit(),
                            entry.totalQuantity(),
                            entry.totalAmount(),
                            entry.paidAmount(),
                            entry.debtAmount(),
                            entry.purchaseCount()
                    ));
                }
            } catch (Exception exception) {
                System.err.println("Aylıq xammal alışı bazadan oxunmadı: " + exception.getMessage());
            }
        }
        if (rawMaterialPurchases.isEmpty() && !monthPurchases.isEmpty()) {
            java.util.Map<String, List<Purchase>> purchasesByMaterial = monthPurchases.stream()
                    .collect(java.util.stream.Collectors.groupingBy(Purchase::materialName));
            for (java.util.Map.Entry<String, List<Purchase>> entry : purchasesByMaterial.entrySet()) {
                String mName = entry.getKey();
                List<Purchase> list = entry.getValue();
                String mCode = list.getFirst().materialCode();
                String unit = list.getFirst().unit();
                BigDecimal totalQty = list.stream().map(Purchase::quantity).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalAmt = list.stream().map(Purchase::totalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalPaid = list.stream().map(Purchase::paidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalDebt = list.stream().map(Purchase::debtAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                rawMaterialPurchases.add(new MonthlyRawMaterialPurchase(mCode, mName, unit, totalQty, totalAmt, totalPaid, totalDebt, list.size()));
            }
            rawMaterialPurchases.sort(Comparator.comparing(MonthlyRawMaterialPurchase::debtAmount).reversed());
        }

        ObservableList<MonthlyRawMaterialPurchase> observableRawMaterialPurchases = FXCollections.observableArrayList(rawMaterialPurchases);

        Label rawMaterialTitle = new Label("Bu ay alınan xammalların və borcların icmalı (" + rawMaterialPurchases.size() + " növ xammal)");
        rawMaterialTitle.getStyleClass().add("section-title");

        TableView<MonthlyRawMaterialPurchase> rawMaterialTable = new TableView<>(observableRawMaterialPurchases);
        rawMaterialTable.setPlaceholder(new Label("Bu ay üçün heç bir xammal alışı edilməyib."));
        rawMaterialTable.getColumns().addAll(
                column("Kod", MonthlyRawMaterialPurchase::materialCode),
                column("Xammal adı", MonthlyRawMaterialPurchase::materialName),
                column("Alınan miqdar", mp -> formatQuantity(mp.totalQuantity()) + " " + mp.unit()),
                column("Ümumi alış dəyəri", mp -> formatMoney(mp.totalAmount())),
                column("İlkin nağd ödəniş", mp -> formatMoney(mp.paidAmount())),
                column("Yaranan borc", mp -> formatMoney(mp.debtAmount())),
                column("Alış sayı", mp -> mp.purchaseCount() + " əməliyyat")
        );
        rawMaterialTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        rawMaterialTable.setPrefHeight(280);
        rawMaterialTable.setMinHeight(200);
        rawMaterialTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && rawMaterialTable.getSelectionModel().getSelectedItem() != null) {
                managePurchasesForMaterial(rawMaterialTable.getSelectionModel().getSelectedItem(), month);
            }
        });
        VBox.setVgrow(rawMaterialTable, Priority.ALWAYS);

        Button manageRawBtn = new Button("🛠️ Əməliyyat tarixçəsi / Səhv alışı sil / Düzəliş et");
        manageRawBtn.getStyleClass().add("secondary-button");
        manageRawBtn.disableProperty().bind(rawMaterialTable.getSelectionModel().selectedItemProperty().isNull());
        manageRawBtn.setOnAction(event -> managePurchasesForMaterial(rawMaterialTable.getSelectionModel().getSelectedItem(), month));

        HBox rawHeaderRow = new HBox(12, rawMaterialTitle, manageRawBtn);
        HBox.setHgrow(rawMaterialTitle, Priority.ALWAYS);
        rawHeaderRow.setAlignment(Pos.CENTER_LEFT);

        Label rawMaterialFooter = new Label("Cəmi xammal alış dəyəri: " + formatMoney(totalPurchasesAmount)
                + "  •  İlkin ödənilən nağd pul: " + formatMoney(totalPurchasesPaid)
                + "  •  Yaranan təchizatçı borcu: " + formatMoney(totalPurchasesDebt));
        rawMaterialFooter.getStyleClass().add("transaction-detail");

        VBox rawMaterialSection = new VBox(10, rawHeaderRow, rawMaterialTable, rawMaterialFooter);

        // Employee-wise salary breakdown for this month (Direct PostgreSQL Query + in-memory fallback)
        List<MonthlySalaryReport> salaryReports = new ArrayList<>();
        if (Database.isConfigured()) {
            try {
                for (Database.MonthlySalaryReportEntry entry : Database.loadMonthlySalaries(month)) {
                    salaryReports.add(new MonthlySalaryReport(
                            entry.employeeCode(),
                            entry.employeeName(),
                            entry.position(),
                            entry.monthlySalary(),
                            entry.totalPaid(),
                            entry.advancePaid(),
                            entry.bonusPaid(),
                            entry.paymentCount()
                    ));
                }
            } catch (Exception exception) {
                System.err.println("Aylıq maaş hesabatı bazadan oxunmadı: " + exception.getMessage());
            }
        }
        if (salaryReports.isEmpty() && !monthSalaries.isEmpty()) {
            java.util.Map<String, List<SalaryRecord>> salariesByEmp = monthSalaries.stream()
                    .collect(java.util.stream.Collectors.groupingBy(SalaryRecord::employeeName));
            for (java.util.Map.Entry<String, List<SalaryRecord>> entry : salariesByEmp.entrySet()) {
                String empName = entry.getKey();
                List<SalaryRecord> list = entry.getValue();
                String empCode = list.getFirst().employeeCode();
                Employee emp = findEmployee(empCode);
                String pos = emp == null ? list.getFirst().position() : emp.position();
                BigDecimal fixedSalary = emp == null ? BigDecimal.ZERO : emp.monthlySalary();
                BigDecimal totalPaid = list.stream().map(SalaryRecord::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal advPaid = list.stream().filter(s -> s.type().contains("Tez") || s.type().contains("Avans"))
                        .map(SalaryRecord::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal bnsPaid = list.stream().filter(s -> s.type().contains("Əlavə") || s.type().contains("Bonus") || s.type().contains("Mükafat"))
                        .map(SalaryRecord::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
                salaryReports.add(new MonthlySalaryReport(empCode, empName, pos, fixedSalary, totalPaid, advPaid, bnsPaid, list.size()));
            }
            salaryReports.sort(Comparator.comparing(MonthlySalaryReport::totalPaid).reversed());
        }

        ObservableList<MonthlySalaryReport> observableSalaryReports = FXCollections.observableArrayList(salaryReports);

        Label salaryTitle = new Label("Bu ay ödənilən maaşların, avansların və bonusların icmalı (" + salaryReports.size() + " işçi)");
        salaryTitle.getStyleClass().add("section-title");

        TableView<MonthlySalaryReport> salaryTable = new TableView<>(observableSalaryReports);
        salaryTable.setPlaceholder(new Label("Bu ay üçün heç bir maaş, avans və ya bonus ödənişi edilməyib."));
        salaryTable.getColumns().addAll(
                column("Kod", MonthlySalaryReport::employeeCode),
                column("İşçi adı", MonthlySalaryReport::employeeName),
                column("Vəzifə", MonthlySalaryReport::position),
                column("Sabit aylıq maaş", ms -> formatMoney(ms.monthlySalary())),
                column("Bu ay ödənilən cəmi", ms -> formatMoney(ms.totalPaid())),
                column("Tez götürülən maaş (Avans)", ms -> formatMoney(ms.advancePaid())),
                column("Əlavə ödəniş (Bonus)", ms -> formatMoney(ms.bonusPaid())),
                column("Ödəniş sayı", ms -> ms.paymentCount() + " dəfə")
        );
        salaryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        salaryTable.setPrefHeight(280);
        salaryTable.setMinHeight(200);
        salaryTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && salaryTable.getSelectionModel().getSelectedItem() != null) {
                manageSalaryPayments(salaryTable.getSelectionModel().getSelectedItem(), month);
            }
        });
        VBox.setVgrow(salaryTable, Priority.ALWAYS);

        Button manageSalaryBtn = new Button("🛠️ Əməliyyat tarixçəsi / Səhv ödənişi sil");
        manageSalaryBtn.getStyleClass().add("secondary-button");
        manageSalaryBtn.disableProperty().bind(salaryTable.getSelectionModel().selectedItemProperty().isNull());
        manageSalaryBtn.setOnAction(event -> manageSalaryPayments(salaryTable.getSelectionModel().getSelectedItem(), month));

        HBox salaryHeaderRow = new HBox(12, salaryTitle, manageSalaryBtn);
        HBox.setHgrow(salaryTitle, Priority.ALWAYS);
        salaryHeaderRow.setAlignment(Pos.CENTER_LEFT);

        Label salaryFooter = new Label("Cəmi ödənilən maaş: " + formatMoney(totalSalariesAmount)
                + "  •  Avans: " + formatMoney(totalAdvanceSalaries)
                + "  •  Əlavə ödəniş (Bonus): " + formatMoney(totalBonusSalaries));
        salaryFooter.getStyleClass().add("transaction-detail");

        VBox salarySection = new VBox(10, salaryHeaderRow, salaryTable, salaryFooter);

        // Credit payments breakdown for this month (Direct PostgreSQL Query + in-memory fallback)
        List<MonthlyCreditReport> creditReports = new ArrayList<>();
        if (Database.isConfigured()) {
            try {
                for (Database.MonthlyCreditReportEntry entry : Database.loadMonthlyCreditPayments(month)) {
                    creditReports.add(new MonthlyCreditReport(
                            entry.creditCode(),
                            entry.creditName(),
                            entry.bankOrCreditor(),
                            entry.monthlyInstallment(),
                            entry.totalPaid(),
                            entry.remainingBalance(),
                            entry.paymentCount()
                    ));
                }
            } catch (Exception exception) {
                System.err.println("Aylıq kredit hesabatı bazadan oxunmadı: " + exception.getMessage());
            }
        }
        if (creditReports.isEmpty() && !monthCreditPayments.isEmpty()) {
            java.util.Map<String, List<CreditPaymentRecord>> creditsByName = monthCreditPayments.stream()
                    .collect(java.util.stream.Collectors.groupingBy(CreditPaymentRecord::creditName));
            for (java.util.Map.Entry<String, List<CreditPaymentRecord>> entry : creditsByName.entrySet()) {
                String cName = entry.getKey();
                List<CreditPaymentRecord> list = entry.getValue();
                String cCode = list.getFirst().creditCode();
                String bank = list.getFirst().bankOrCreditor();
                BigDecimal inst = list.getFirst().monthlyInstallment();
                BigDecimal paid = list.stream().map(CreditPaymentRecord::paidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                CreditContract contract = credits.stream().filter(c -> c.code().equals(cCode)).findFirst().orElse(null);
                BigDecimal remBal = contract == null ? BigDecimal.ZERO : contract.remainingBalance();
                creditReports.add(new MonthlyCreditReport(cCode, cName, bank, inst, paid, remBal, list.size()));
            }
            creditReports.sort(Comparator.comparing(MonthlyCreditReport::totalPaid).reversed());
        }

        ObservableList<MonthlyCreditReport> observableCreditReports = FXCollections.observableArrayList(creditReports);

        Label creditTitle = new Label("Bu ayın kredit ödənişləri və öhdəlikləri (" + creditReports.size() + " ödənilən kredit)");
        creditTitle.getStyleClass().add("section-title");

        TableView<MonthlyCreditReport> creditTable = new TableView<>(observableCreditReports);
        creditTable.setPlaceholder(new Label("Bu ay üçün heç bir kredit ödənişi edilməyib."));
        creditTable.getColumns().addAll(
                column("Kod", MonthlyCreditReport::creditCode),
                column("Kredit / Borc adı", MonthlyCreditReport::creditName),
                column("Bank / Kredit verən", MonthlyCreditReport::bankOrCreditor),
                column("Aylıq müqavilə ödənişi", cr -> formatMoney(cr.monthlyInstallment())),
                column("Bu ay ödənilən cəmi", cr -> formatMoney(cr.totalPaid())),
                column("Bu ay üzrə qalıq borc", cr -> {
                    BigDecimal rem = cr.monthlyInstallment().subtract(cr.totalPaid());
                    return formatMoney(rem.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : rem);
                }),
                column("Ümumi Kredit Qalığı", cr -> formatMoney(cr.remainingBalance())),
                column("Ödəniş sayı", cr -> cr.paymentCount() + " dəfə")
        );
        creditTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        creditTable.setPrefHeight(280);
        creditTable.setMinHeight(200);
        creditTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && creditTable.getSelectionModel().getSelectedItem() != null) {
                manageCreditPayments(creditTable.getSelectionModel().getSelectedItem(), month);
            }
        });
        VBox.setVgrow(creditTable, Priority.ALWAYS);

        Button manageCreditBtn = new Button("🛠️ Əməliyyat tarixçəsi / Səhv ödənişi sil");
        manageCreditBtn.getStyleClass().add("secondary-button");
        manageCreditBtn.disableProperty().bind(creditTable.getSelectionModel().selectedItemProperty().isNull());
        manageCreditBtn.setOnAction(event -> manageCreditPayments(creditTable.getSelectionModel().getSelectedItem(), month));

        HBox creditHeaderRow = new HBox(12, creditTitle, manageCreditBtn);
        HBox.setHgrow(creditTitle, Priority.ALWAYS);
        creditHeaderRow.setAlignment(Pos.CENTER_LEFT);

        BigDecimal totalRemainingCreditDebt = credits.stream().map(CreditContract::remainingBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        Label creditFooter = new Label("Cəmi müqavilə öhdəliyi: " + formatMoney(totalMonthlyCreditObligation)
                + "  •  Bu ay ödənilən cəmi: " + formatMoney(totalCreditPaid)
                + "  •  Cəmi qalıq kredit borcu: " + formatMoney(totalRemainingCreditDebt));
        creditFooter.getStyleClass().add("transaction-detail");

        VBox creditSection = new VBox(10, creditHeaderRow, creditTable, creditFooter);

        HBox monthSelectorRow = new HBox(10, new Label("Hesabat ayı:"), selector);
        monthSelectorRow.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(18, monthSelectorRow, title, cashCards, operationCards, productSection, rawMaterialSection, salarySection, creditSection);
        content.getStyleClass().add("content");
        setPageContent(content);
    }

    private void managePurchasesForMaterial(MonthlyRawMaterialPurchase report, String month) {
        if (report == null) return;
        List<Purchase> matching = purchases.stream()
                .filter(p -> p.materialCode().equals(report.materialCode()) && monthKey(p.createdAt()).equals(month))
                .toList();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Xammal Alış Əməliyyatları — " + report.materialName());
        dialog.setHeaderText(monthLabel(month) + " ayı üzrə “" + report.materialName() + "” alış tarixçəsi");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        ObservableList<Purchase> obs = FXCollections.observableArrayList(matching);
        TableView<Purchase> table = new TableView<>(obs);
        table.setPlaceholder(new Label("Bu ay üzrə alış tapılmadı."));
        table.getColumns().addAll(
                column("Sənəd / Tarix", Purchase::createdAt),
                column("Təchizatçı", Purchase::supplierName),
                column("Alınan miqdar", p -> formatQuantity(p.quantity()) + " " + p.unit()),
                column("Ümumi məbləğ", p -> formatMoney(p.totalAmount())),
                column("Ödənilən nağd", p -> formatMoney(p.paidAmount())),
                column("Yaranan borc", p -> formatMoney(p.debtAmount()))
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(250);

        Button deleteBtn = new Button("Səhv alışı sil");
        deleteBtn.getStyleClass().add("danger-button");
        deleteBtn.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        Button editBtn = new Button("Məbləğ / Borc düzəlişi et");
        editBtn.getStyleClass().add("secondary-button");
        editBtn.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        deleteBtn.setOnAction(event -> {
            Purchase sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "“" + sel.supplierName() + "” təchizatçısından alınan " + formatMoney(sel.totalAmount()) + " dəyərindəki alış tamamilə silinsin?\n"
                    + "Yaranmış borc (" + formatMoney(sel.debtAmount()) + ") və anbardan xammal miqdarı avtomatik geri çıxılacaq.",
                    ButtonType.CANCEL, ButtonType.OK);
            confirm.setTitle("Alışı sil");
            confirm.setHeaderText("Alış əməliyyatı ləğv edilir");
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    if (sel.id() > 0 && Database.isConfigured()) {
                        Database.deletePurchaseCascade(sel.id());
                    }
                    purchases.remove(sel);
                    obs.remove(sel);

                    // Update local memory supplier debt
                    Supplier s = findSupplier(sel.supplierCode());
                    if (s != null && sel.debtAmount().signum() > 0) {
                        BigDecimal newDebt = s.debt().subtract(sel.debtAmount());
                        if (newDebt.signum() < 0) newDebt = BigDecimal.ZERO;
                        int sIdx = suppliers.indexOf(s);
                        if (sIdx >= 0) suppliers.set(sIdx, new Supplier(s.code(), s.name(), s.phone(), newDebt));
                    }

                    // Update local memory raw material stock
                    RawMaterial rm = findMaterial(sel.materialCode());
                    if (rm != null && sel.quantity().signum() > 0) {
                        BigDecimal newStock = rm.stock().subtract(sel.quantity());
                        if (newStock.signum() < 0) newStock = BigDecimal.ZERO;
                        int mIdx = rawMaterials.indexOf(rm);
                        if (mIdx >= 0) rawMaterials.set(mIdx, new RawMaterial(rm.code(), rm.name(), rm.unit(), newStock, rm.unitCost()));
                    }

                    addLog("Alış silindi", sel.supplierName() + " — " + sel.materialName() + " (" + formatMoney(sel.totalAmount()) + ")");
                    dialog.close();
                    showMonthlyReports(month);
                } catch (Exception ex) {
                    showMessage(Alert.AlertType.ERROR, "Xəta", "Alış silinmədi: " + ex.getMessage());
                }
            }
        });

        editBtn.setOnAction(event -> {
            Purchase sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            openEditPurchaseDialog(sel, month, dialog);
        });

        HBox actions = new HBox(12, editBtn, deleteBtn);
        VBox box = new VBox(10, table, actions);
        box.setPrefWidth(650);
        dialog.getDialogPane().setContent(box);
        dialog.showAndWait();
    }

    private void openEditPurchaseDialog(Purchase sel, String month, Dialog<?> parentDialog) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Alış Məbləğinə Və Borca Düzəliş");
        dialog.setHeaderText("“" + sel.supplierName() + " — " + sel.materialName() + "” alışı üçün düzgün məbləğləri daxil edin");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        TextField totalF = new TextField(sel.totalAmount().toPlainString());
        TextField paidF = new TextField(sel.paidAmount().toPlainString());
        Label error = new Label(); error.getStyleClass().add("error-label");

        VBox form = new VBox(8,
                new Label("Düzgün Ümumi Alış Məbləği (₼) *"), totalF,
                new Label("İlkin Ödənilən Nağd Pul (₼) *"), paidF,
                error);
        form.setPrefWidth(380);
        dialog.getDialogPane().setContent(form);

        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                BigDecimal newTotal = positiveOrZero(totalF.getText());
                BigDecimal newPaid = positiveOrZero(paidF.getText());
                if (newPaid.compareTo(newTotal) > 0) throw new IllegalArgumentException("Nağd ödəniş ümumi məbləğdən çox ola bilməz.");

                BigDecimal newDebt = newTotal.subtract(newPaid);
                BigDecimal debtDiff = newDebt.subtract(sel.debtAmount());

                if (sel.id() > 0 && Database.isConfigured()) {
                    Database.updatePurchase(sel.id(), newTotal, newPaid);
                }

                // Update memory supplier debt
                Supplier s = findSupplier(sel.supplierCode());
                if (s != null && debtDiff.signum() != 0) {
                    BigDecimal updatedDebt = s.debt().add(debtDiff);
                    if (updatedDebt.signum() < 0) updatedDebt = BigDecimal.ZERO;
                    int sIdx = suppliers.indexOf(s);
                    if (sIdx >= 0) suppliers.set(sIdx, new Supplier(s.code(), s.name(), s.phone(), updatedDebt));
                }

                // Update memory purchase item
                Purchase updatedPurchase = new Purchase(sel.id(), sel.documentNo(), sel.supplierCode(), sel.supplierName(),
                        sel.materialCode(), sel.materialName(), sel.unit(), sel.quantity(), newTotal, newPaid, newDebt, sel.createdAt());
                int pIdx = purchases.indexOf(sel);
                if (pIdx >= 0) purchases.set(pIdx, updatedPurchase);

                addLog("Alış dəyişdirildi", sel.supplierName() + " — Yeni borc: " + formatMoney(newDebt));
                dialog.close();
                if (parentDialog != null) parentDialog.close();
                showMonthlyReports(month);
            } catch (Exception ex) {
                error.setText(ex.getMessage() == null ? "Məlumatı düzgün daxil edin." : ex.getMessage());
                event.consume();
            }
        });
        dialog.showAndWait();
    }

    private void manageProductDeliveries(MonthlyProductSale report, String month) {
        if (report == null) return;
        List<Sale> matching = sales.stream()
                .filter(s -> s.productCode().equals(report.productCode()) && monthKey(s.createdAt()).equals(month))
                .toList();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Məhsul Təhvil Əməliyyatları — " + report.productName());
        dialog.setHeaderText(monthLabel(month) + " ayı üzrə “" + report.productName() + "” təhvil tarixçəsi");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        ObservableList<Sale> obs = FXCollections.observableArrayList(matching);
        TableView<Sale> table = new TableView<>(obs);
        table.setPlaceholder(new Label("Bu ay üzrə təhvil tapılmadı."));
        table.getColumns().addAll(
                column("Sənəd No / Tarix", Sale::createdAt),
                column("Müştəri", Sale::customerName),
                column("Satılan say", s -> formatQuantity(s.quantity()) + " ədəd"),
                column("Məbləğ", s -> formatMoney(s.total())),
                column("Ödənilən nağd", s -> formatMoney(s.paidAmount())),
                column("Qalan borc", s -> formatMoney(s.remainingDebt()))
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(250);

        Button deleteBtn = new Button("Səhv təhvili sil");
        deleteBtn.getStyleClass().add("danger-button");
        deleteBtn.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        deleteBtn.setOnAction(event -> {
            Sale sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "“" + sel.customerName() + "” müştərisinə edilən " + formatMoney(sel.total()) + " məbləğindəki mal təhvili silinsin?\n"
                    + "Müştəri borcu (" + formatMoney(sel.remainingDebt()) + ") və hazır məhsul sayları avtomatik bərpa olunacaq.",
                    ButtonType.CANCEL, ButtonType.OK);
            confirm.setTitle("Təhvili sil");
            confirm.setHeaderText("Mal təhvili əməliyyatı ləğv edilir");
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    if (Database.isConfigured()) {
                        Database.deleteDelivery(sel.code());
                    }
                    sales.remove(sel);
                    obs.remove(sel);

                    // Update memory customer debt
                    Customer c = findCustomer(sel.customerCode());
                    if (c != null && sel.remainingDebt().signum() > 0) {
                        BigDecimal oldDebt = customerDebt(c);
                        BigDecimal newDebt = oldDebt.subtract(sel.remainingDebt());
                        if (newDebt.signum() < 0) newDebt = BigDecimal.ZERO;
                        int cIdx = customers.indexOf(c);
                        if (cIdx >= 0) customers.set(cIdx, new Customer(c.code(), c.name(), c.phone(), formatMoney(newDebt), c.managerCode()));
                    }

                    // Restore product stock in memory
                    Product p = findProduct(sel.productCode());
                    if (p != null && sel.quantity().signum() > 0) {
                        int pIdx = products.indexOf(p);
                        if (pIdx >= 0) products.set(pIdx, new Product(p.code(), p.name(), p.recipe(), p.stock().add(sel.quantity()), p.salePrice(), p.unitCost()));
                    }

                    addLog("Təhvil silindi", sel.customerName() + " — " + sel.productName() + " (" + formatMoney(sel.total()) + ")");
                    dialog.close();
                    showMonthlyReports(month);
                } catch (Exception ex) {
                    showMessage(Alert.AlertType.ERROR, "Xəta", "Təhvil silinmədi: " + ex.getMessage());
                }
            }
        });

        HBox actions = new HBox(12, deleteBtn);
        VBox box = new VBox(10, table, actions);
        box.setPrefWidth(650);
        dialog.getDialogPane().setContent(box);
        dialog.showAndWait();
    }

    private void manageSalaryPayments(MonthlySalaryReport report, String month) {
        if (report == null) return;
        List<SalaryRecord> matching = salaryRecords.stream()
                .filter(s -> s.employeeCode().equals(report.employeeCode()) && monthKey(s.createdAt()).equals(month))
                .toList();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Maaş / Avans Ödənişləri — " + report.employeeName());
        dialog.setHeaderText(monthLabel(month) + " ayı üzrə “" + report.employeeName() + "” ödəniş tarixçəsi");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        ObservableList<SalaryRecord> obs = FXCollections.observableArrayList(matching);
        TableView<SalaryRecord> table = new TableView<>(obs);
        table.setPlaceholder(new Label("Bu ay üzrə ödəniş tapılmadı."));
        table.getColumns().addAll(
                column("Tarix", SalaryRecord::createdAt),
                column("İşçi adı", SalaryRecord::employeeName),
                column("Vəzifə", SalaryRecord::position),
                column("Ödənilən məbləğ", s -> formatMoney(s.amount())),
                column("Ödəniş növü", SalaryRecord::type),
                column("Qeyd", SalaryRecord::note)
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(250);

        Button deleteBtn = new Button("Səhv ödənişi sil");
        deleteBtn.getStyleClass().add("danger-button");
        deleteBtn.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        deleteBtn.setOnAction(event -> {
            SalaryRecord sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "“" + sel.employeeName() + "” üçün edilən " + formatMoney(sel.amount()) + " məbləğindəki " + sel.type() + " silinsin?",
                    ButtonType.CANCEL, ButtonType.OK);
            confirm.setTitle("Maaş ödənişini sil");
            confirm.setHeaderText("Maaş ödənişi silinir");
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    if (sel.id() > 0 && Database.isConfigured()) {
                        Database.deleteSalary(sel.id());
                    }
                    obs.remove(sel);

                    // Remove from main memory salaryRecords list by ID or matching fields
                    salaryRecords.removeIf(s -> (sel.id() > 0 && s.id() == sel.id())
                            || (s.employeeCode().equals(sel.employeeCode())
                                && s.amount().compareTo(sel.amount()) == 0
                                && (sel.id() == 0 || s.id() == sel.id())));

                    // Reverse cash expense movement for salary in memory once
                    CashMovement cmMatch = cashMovements.stream()
                            .filter(cm -> cm.direction() == CashDirection.EXPENSE
                                    && cm.amount().compareTo(sel.amount()) == 0
                                    && cm.description().contains(sel.employeeName()))
                            .findFirst().orElse(null);
                    if (cmMatch != null) {
                        cashMovements.remove(cmMatch);
                    }

                    addLog("Maaş ödənişi silindi", sel.employeeName() + " — " + formatMoney(sel.amount()));
                    dialog.close();
                    showMonthlyReports(month);
                } catch (Exception ex) {
                    showMessage(Alert.AlertType.ERROR, "Xəta", "Ödəniş silinmədi: " + ex.getMessage());
                }
            }
        });

        HBox actions = new HBox(12, deleteBtn);
        VBox box = new VBox(10, table, actions);
        box.setPrefWidth(650);
        dialog.getDialogPane().setContent(box);
        dialog.showAndWait();
    }

    private void manageCreditPayments(MonthlyCreditReport report, String month) {
        if (report == null) return;
        List<CreditPaymentRecord> matching = creditPayments.stream()
                .filter(c -> c.creditCode().equals(report.creditCode()) && monthKey(c.createdAt()).equals(month))
                .toList();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Kredit Ödənişləri — " + report.creditName());
        dialog.setHeaderText(monthLabel(month) + " ayı üzrə “" + report.creditName() + "” kredit ödənişləri");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        ObservableList<CreditPaymentRecord> obs = FXCollections.observableArrayList(matching);
        TableView<CreditPaymentRecord> table = new TableView<>(obs);
        table.setPlaceholder(new Label("Bu ay üzrə kredit ödənişi tapılmadı."));
        table.getColumns().addAll(
                column("Tarix", CreditPaymentRecord::createdAt),
                column("Kredit adı", CreditPaymentRecord::creditName),
                column("Bank", CreditPaymentRecord::bankOrCreditor),
                column("Ödənilən məbləğ", c -> formatMoney(c.paidAmount())),
                column("Qeyd", CreditPaymentRecord::note)
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(250);

        Button deleteBtn = new Button("Səhv ödənişi sil");
        deleteBtn.getStyleClass().add("danger-button");
        deleteBtn.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        deleteBtn.setOnAction(event -> {
            CreditPaymentRecord sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "“" + sel.creditName() + "” üzrə verilən " + formatMoney(sel.paidAmount()) + " məbləğindəki kredit ödənişi silinsin?\n"
                    + "Qalıq kredit borcu avtomatik bərpa olunacaq.",
                    ButtonType.CANCEL, ButtonType.OK);
            confirm.setTitle("Kredit ödənişini sil");
            confirm.setHeaderText("Kredit ödənişi ləğv edilir");
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    if (sel.id() > 0 && Database.isConfigured()) {
                        Database.deleteCreditPayment(sel.id());
                    }
                    obs.remove(sel);

                    // Remove from main memory creditPayments list by ID or matching fields
                    creditPayments.removeIf(c -> (sel.id() > 0 && c.id() == sel.id())
                            || (c.creditCode().equals(sel.creditCode())
                                && c.paidAmount().compareTo(sel.paidAmount()) == 0
                                && (sel.id() == 0 || c.id() == sel.id())));

                    // Restore memory credit contract remaining balance
                    CreditContract cc = credits.stream().filter(c -> c.code().equals(sel.creditCode())).findFirst().orElse(null);
                    if (cc != null) {
                        int cIdx = credits.indexOf(cc);
                        if (cIdx >= 0) credits.set(cIdx, new CreditContract(cc.code(), cc.name(), cc.bankOrCreditor(), cc.totalAmount(), cc.monthlyAmount(), cc.remainingBalance().add(sel.paidAmount())));
                    }

                    // Reverse cash expense movement for credit payment in memory once
                    CashMovement cmMatch = cashMovements.stream()
                            .filter(cm -> cm.direction() == CashDirection.EXPENSE
                                    && cm.amount().compareTo(sel.paidAmount()) == 0
                                    && cm.description().contains(sel.creditName()))
                            .findFirst().orElse(null);
                    if (cmMatch != null) {
                        cashMovements.remove(cmMatch);
                    }

                    addLog("Kredit ödənişi silindi", sel.creditName() + " — " + formatMoney(sel.paidAmount()));
                    dialog.close();
                    showMonthlyReports(month);
                } catch (Exception ex) {
                    showMessage(Alert.AlertType.ERROR, "Xəta", "Kredit ödənişi silinmədi: " + ex.getMessage());
                }
            }
        });

        HBox actions = new HBox(12, deleteBtn);
        VBox box = new VBox(10, table, actions);
        box.setPrefWidth(650);
        dialog.getDialogPane().setContent(box);
        dialog.showAndWait();
    }

    private List<String> availableReportMonths() {
        String cur = currentMonthKey();
        if (Database.isConfigured()) {
            try {
                List<String> dbMonths = Database.loadAllEventMonths().stream()
                        .filter(m -> m != null && m.compareTo(cur) <= 0)
                        .collect(java.util.stream.Collectors.toList());
                if (!dbMonths.contains(cur)) dbMonths.addFirst(cur);
                return dbMonths;
            } catch (Exception exception) {
                System.err.println("Aylar bazadan oxunmadı: " + exception.getMessage());
            }
        }
        List<String> months = java.util.stream.Stream.of(
                        sales.stream().map(s -> monthKey(s.createdAt())),
                        activityLogs.stream().map(this::monthKey),
                        cashMovements.stream().map(this::monthKey),
                        wastes.stream().map(w -> monthKey(w.createdAt())),
                        purchases.stream().map(this::monthKey),
                        salaryRecords.stream().map(this::monthKey),
                        creditPayments.stream().map(this::monthKey))
                .flatMap(java.util.function.Function.identity())
                .filter(s -> s != null && !s.isBlank() && s.compareTo(cur) <= 0)
                .distinct().sorted(Comparator.reverseOrder()).collect(java.util.stream.Collectors.toList());
        if (!months.contains(cur)) months.addFirst(cur);
        return months;
    }

    private void showCustomers() {
        pageTitle.setText("Müştərilər");

        TextField search = new TextField();
        search.setPromptText("Ad, telefon və ya kod ilə axtarın...");
        search.getStyleClass().add("search-field");
        Button add = new Button("+ Yeni müştəri");
        add.getStyleClass().add("primary-button");
        Button edit = new Button("Düzəliş et");
        edit.getStyleClass().add("secondary-button");
        Button delete = new Button("Sil");
        delete.getStyleClass().add("danger-button");
        HBox toolbar = new HBox(12, search, add, edit, delete);
        HBox.setHgrow(search, Priority.ALWAYS);

        ObservableList<Customer> filteredCustomers = FXCollections.observableArrayList(customers);
        search.textProperty().addListener((obs, oldVal, newVal) -> {
            String query = newVal == null ? "" : newVal.trim().toLowerCase();
            filteredCustomers.clear();
            if (query.isEmpty()) {
                filteredCustomers.addAll(customers);
            } else {
                for (Customer c : customers) {
                    if (c.name().toLowerCase().contains(query) || c.code().toLowerCase().contains(query) || c.phone().toLowerCase().contains(query)) {
                        filteredCustomers.add(c);
                    }
                }
            }
        });

        TableView<Customer> table = new TableView<>(filteredCustomers);
        table.setPlaceholder(new Label("Hələ müştəri yoxdur"));
        table.getColumns().addAll(column("Kod", Customer::code), column("Ad", Customer::name),
                column("Telefon", Customer::phone), column("Menecer", customer -> managerName(customer.managerCode())),
                column("Cari borc", Customer::balance));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        add.setOnAction(event -> openCustomerDialog(null, table));
        edit.setOnAction(event -> openCustomerDialog(table.getSelectionModel().getSelectedItem(), table));
        delete.setOnAction(event -> deleteCustomer(table.getSelectionModel().getSelectedItem()));
        edit.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        delete.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && table.getSelectionModel().getSelectedItem() != null) {
                openCustomerDialog(table.getSelectionModel().getSelectedItem(), table);
            }
        });
        table.setMinHeight(420);
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox content = new VBox(18, toolbar, table);
        content.getStyleClass().add("content");
        setPageContent(content);
    }

    private void openCustomerDialog(Customer existing, TableView<Customer> table) {
        boolean editing = existing != null;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(editing ? "Müştəriyə düzəliş" : "Yeni müştəri");
        dialog.setHeaderText(editing ? "Müştəri məlumatlarını yeniləyin" : "Yeni müştəri məlumatlarını daxil edin");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        TextField code = new TextField(editing ? existing.code() : "");
        code.setDisable(editing);
        code.setPromptText("Məsələn: M-002");
        TextField name = new TextField(editing ? existing.name() : "");
        name.setPromptText("Şirkət və ya şəxs adı");
        TextField phone = new TextField(editing ? existing.phone() : "");
        phone.setPromptText("+994 50 123 45 67");
        TextField balance = new TextField(editing ? existing.balance().replace(" ₼", "") : "0.00");
        balance.setPromptText("0.00");
        ComboBox<Manager> manager = new ComboBox<>(managers);
        manager.setPromptText("Meneceri seçin");
        manager.setMaxWidth(Double.MAX_VALUE);
        if (editing) manager.setValue(findManager(existing.managerCode()));
        Label error = new Label();
        error.getStyleClass().add("error-label");

        VBox form = new VBox(8,
                new Label("Müştəri kodu *"), code,
                new Label("Ad *"), name,
                new Label("Telefon"), phone,
                new Label("Cavabdeh menecer *"), manager,
                new Label("Cari borc (₼)"), balance,
                error);
        form.setPrefWidth(390);
        dialog.getDialogPane().setContent(form);

        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String normalizedCode = code.getText().trim();
            String normalizedName = name.getText().trim();
            if (normalizedCode.isEmpty() || normalizedName.isEmpty() || manager.getValue() == null) {
                error.setText("Kod, ad və menecer mütləq daxil edilməlidir.");
                event.consume();
                return;
            }
            boolean duplicateCode = customers.stream().anyMatch(customer ->
                    customer.code().equalsIgnoreCase(normalizedCode) && customer != existing);
            if (duplicateCode) {
                error.setText("Bu müştəri kodu artıq istifadə olunur.");
                event.consume();
                return;
            }

            BigDecimal debt;
            try {
                debt = new BigDecimal(balance.getText().trim().replace(',', '.'));
                if (debt.signum() < 0) throw new NumberFormatException();
            } catch (NumberFormatException exception) {
                error.setText("Cari borcu düzgün rəqəm kimi daxil edin. Məsələn: 125.50");
                event.consume();
                return;
            }

            Customer saved = new Customer(normalizedCode, normalizedName, phone.getText().trim(),
                    formatMoney(debt), manager.getValue().code());
            if (editing) {
                int idx = customers.indexOf(existing);
                customers.set(idx, saved);
            } else {
                customers.add(saved);
            }
            persistCustomer(saved);
            table.getSelectionModel().select(saved);
        });
        dialog.showAndWait();
        showCustomers();
    }

    private void deleteCustomer(Customer customer) {
        if (customer == null) return;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Müştərini sil");
        confirmation.setHeaderText("“" + customer.name() + "” silinsin?");
        confirmation.setContentText("Bu əməliyyatı geri qaytarmaq mümkün deyil.");
        confirmation.showAndWait().ifPresent(answer -> {
            if (answer == ButtonType.OK) {
                try {
                    Database.deleteCustomer(customer.code());
                    customers.remove(customer);
                    addLog("Müştəri silindi", "Müştəri: " + customer.name() + " (" + customer.code() + ")");
                    showCustomers();
                } catch (Exception exception) {
                    showMessage(Alert.AlertType.ERROR, "Müştəri silinmədi", "Baza xətası: " + exception.getMessage());
                }
            }
        });
    }

    private void showManagers() {
        pageTitle.setText("Menecerlər");
        Button add = new Button("+ Menecer əlavə et");
        add.getStyleClass().add("primary-button");
        add.setOnAction(event -> openManagerDialog(null));
        Label description = new Label("Hər müştəri bir menecerə bağlı ola bilər. Mal təhvili və komissiya hesablamaları burada idarə olunur.");
        description.setWrapText(true);
        VBox managerCards = new VBox(12);
        for (Manager manager : managers) managerCards.getChildren().add(createManagerCard(manager));
        VBox content = new VBox(16, description, add, managerCards);
        content.getStyleClass().add("content");
        setPageContent(content);
    }

    private VBox createManagerCard(Manager manager) {
        BigDecimal salesAmount = sales.stream().filter(sale -> sale.managerCode().equals(manager.code())).map(Sale::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal soldQuantity = sales.stream().filter(sale -> sale.managerCode().equals(manager.code())).map(Sale::quantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal customerDebt = customers.stream().filter(customer -> customer.managerCode().equals(manager.code()))
                .map(this::customerDebt).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal collectedCash = payments.stream().filter(payment -> payment.managerCode().equals(manager.code())
                        && payment.type().contains("Müştəri")).map(Payment::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commission = salesAmount.multiply(manager.commissionRate()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        Label name = new Label(manager.name()); name.getStyleClass().add("manager-name");
        Label code = new Label(manager.code() + "  •  Komissiya: " + formatQuantity(manager.commissionRate()) + "%");
        code.getStyleClass().add("transaction-detail");
        VBox summary = new VBox(4, name, code);
        HBox figures = new HBox(24,
                managerStat("Satdığı məhsul", formatQuantity(soldQuantity) + " ədəd"),
                managerStat("Təhvil dəyəri", formatMoney(salesAmount)),
                managerStat("Komissiya qazancı", formatMoney(commission)),
                managerStat("Müştərilərin borcu", formatMoney(customerDebt)),
                managerStat("Yığdığı nağd pul", formatMoney(collectedCash)));
        Button edit = new Button("Düzəliş et");
        edit.getStyleClass().add("secondary-button");
        edit.setOnAction(event -> openManagerDialog(manager));
        Button delete = new Button("Sil");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(event -> deleteManager(manager));
        HBox actions = new HBox(10, edit, delete);
        VBox card = new VBox(16, summary, figures, actions);
        card.getStyleClass().add("manager-card");
        return card;
    }

    private VBox managerStat(String label, String value) {
        Label labelView = new Label(label); labelView.getStyleClass().add("transaction-detail");
        Label valueView = new Label(value); valueView.getStyleClass().add("manager-stat-value");
        return new VBox(3, labelView, valueView);
    }

    private void openManagerDialog(Manager existing) {
        boolean editing = existing != null;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(editing ? "Menecerə düzəliş" : "Menecer əlavə et");
        dialog.setHeaderText(editing ? "Menecer məlumatlarını yeniləyin" : "Menecer məlumatlarını daxil edin");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        TextField code = new TextField(editing ? existing.code() : ""); code.setPromptText("Məsələn: MN-002");
        code.setDisable(editing);
        TextField name = new TextField(editing ? existing.name() : ""); name.setPromptText("Məsələn: Aysel Məmmədova");
        TextField rate = new TextField(editing ? existing.commissionRate().toPlainString() : "0");
        Label error = new Label(); error.getStyleClass().add("error-label");
        VBox form = new VBox(8, new Label("Menecer kodu *"), code, new Label("Ad və soyad *"), name,
                new Label("Komissiya faizi (%)"), rate, error);
        form.setPrefWidth(390);
        dialog.getDialogPane().setContent(form);
        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                if (code.getText().isBlank() || name.getText().isBlank()) throw new IllegalArgumentException("Kod və ad mütləq daxil edilməlidir.");
                if (managers.stream().anyMatch(manager -> manager.code().equalsIgnoreCase(code.getText().trim()) && manager != existing)) throw new IllegalArgumentException("Bu menecer kodu artıq istifadə olunur.");
                Manager saved = new Manager(code.getText().trim(), name.getText().trim(), positiveOrZero(rate.getText()));
                if (editing) {
                    managers.set(managers.indexOf(existing), saved);
                    for (int i = 0; i < customers.size(); i++) {
                        Customer customer = customers.get(i);
                        if (customer.managerCode().equals(existing.code())) {
                            customers.set(i, new Customer(customer.code(), customer.name(), customer.phone(), customer.balance(), saved.code()));
                        }
                    }
                } else {
                    managers.add(saved);
                }
                Database.upsertManager(new Database.ManagerEntry(saved.code(), saved.name(), saved.commissionRate()));
            } catch (Exception exception) {
                error.setText(exception.getMessage() == null ? "Məlumatları düzgün daxil edin." : exception.getMessage());
                event.consume();
            }
        });
        dialog.showAndWait();
        showManagers();
    }

    private void deleteManager(Manager manager) {
        if (manager == null) return;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Meneceri sil");
        confirmation.setHeaderText("“" + manager.name() + "” silinsin?");
        confirmation.setContentText("Bu menecer silindikdə bağlı müştərilərin menecer təyinatı sıfırlanacaq.");
        confirmation.showAndWait().ifPresent(answer -> {
            if (answer == ButtonType.OK) {
                try {
                    Database.deleteManager(manager.code());
                    managers.remove(manager);
                    for (int i = 0; i < customers.size(); i++) {
                        Customer c = customers.get(i);
                        if (manager.code().equals(c.managerCode())) {
                            Customer updated = new Customer(c.code(), c.name(), c.phone(), c.balance(), "");
                            customers.set(i, updated);
                            persistCustomer(updated);
                        }
                    }
                    addLog("Menecer silindi", "Menecer: " + manager.name() + " (" + manager.code() + ")");
                    showManagers();
                } catch (Exception exception) {
                    showMessage(Alert.AlertType.ERROR, "Menecer silinmədi", "Baza xətası: " + exception.getMessage());
                }
            }
        });
    }

    private void showRawMaterials() {
        pageTitle.setText("Xammallar");
        Button add = new Button("+ Xammal əlavə et");
        add.getStyleClass().add("primary-button");
        Button edit = new Button("Düzəliş et");
        edit.getStyleClass().add("secondary-button");
        Button delete = new Button("Sil");
        delete.getStyleClass().add("danger-button");
        HBox toolbar = new HBox(12, add, edit, delete);

        TableView<RawMaterial> table = new TableView<>(rawMaterials);
        table.getColumns().addAll(column("Kod", RawMaterial::code), column("Xammal", RawMaterial::name),
                column("Vahid", RawMaterial::unit), column("Anbardakı miqdar", material -> formatQuantity(material.stock())),
                column("1 vahidin maya dəyəri", material -> formatMoney(material.unitCost())));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Hələ xammal yoxdur"));

        add.setOnAction(event -> openRawMaterialDialog(null, table));
        edit.setOnAction(event -> openRawMaterialDialog(table.getSelectionModel().getSelectedItem(), table));
        delete.setOnAction(event -> deleteRawMaterial(table.getSelectionModel().getSelectedItem()));
        edit.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        delete.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        table.setMinHeight(420);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox content = new VBox(18, toolbar, table);
        content.getStyleClass().add("content");
        setPageContent(content);
    }

    private void showSuppliers() {
        pageTitle.setText("Təchizatçılar");
        Button add = new Button("+ Təchizatçı əlavə et");
        add.getStyleClass().add("primary-button");
        Button purchase = new Button("Xammal alışı");
        purchase.getStyleClass().add("secondary-button");
        Button edit = new Button("Düzəliş et");
        edit.getStyleClass().add("secondary-button");
        Button delete = new Button("Sil");
        delete.getStyleClass().add("danger-button");
        HBox toolbar = new HBox(12, add, purchase, edit, delete);
        TableView<Supplier> table = new TableView<>(suppliers);
        table.getColumns().addAll(column("Kod", Supplier::code), column("Təchizatçı", Supplier::name),
                column("Telefon", Supplier::phone), column("Bizim borcumuz", supplier -> formatMoney(supplier.debt())));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Hələ təchizatçı yoxdur"));
        add.setOnAction(event -> openSupplierDialog(null, table));
        purchase.setOnAction(event -> openPurchaseDialog());
        edit.setOnAction(event -> openSupplierDialog(table.getSelectionModel().getSelectedItem(), table));
        delete.setOnAction(event -> deleteSupplier(table.getSelectionModel().getSelectedItem()));
        edit.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        delete.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && table.getSelectionModel().getSelectedItem() != null) {
                openSupplierDialog(table.getSelectionModel().getSelectedItem(), table);
            }
        });
        table.setMinHeight(420);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox content = new VBox(12, toolbar,
                new Label("“Xammal alışı” anbar miqdarını artırır, ödənilməmiş hissəni təchizatçı borcuna əlavə edir."), table);
        content.getStyleClass().add("content");
        setPageContent(content);
    }

    private void openSupplierDialog(Supplier existing, TableView<Supplier> table) {
        boolean editing = existing != null;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(editing ? "Təchizatçıya düzəliş" : "Təchizatçı əlavə et");
        dialog.setHeaderText(editing ? "Təchizatçı məlumatlarını yeniləyin" : "Xammalı satan şirkət və ya şəxsi daxil edin");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        TextField code = new TextField(editing ? existing.code() : "");
        code.setDisable(editing);
        TextField name = new TextField(editing ? existing.name() : "");
        TextField phone = new TextField(editing ? existing.phone() : "");
        TextField debt = new TextField(editing ? existing.debt().toPlainString() : "0.00");
        Label error = new Label();
        error.getStyleClass().add("error-label");
        VBox form = new VBox(8, new Label("Təchizatçı kodu *"), code, new Label("Ad *"), name,
                new Label("Telefon"), phone, new Label("Bizim borcumuz (₼)"), debt, error);
        form.setPrefWidth(390);
        dialog.getDialogPane().setContent(form);
        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (code.getText().isBlank() || name.getText().isBlank()) {
                error.setText("Kod və ad mütləq daxil edilməlidir."); event.consume(); return;
            }
            if (suppliers.stream().anyMatch(supplier -> supplier.code().equalsIgnoreCase(code.getText().trim()) && supplier != existing)) {
                error.setText("Bu təchizatçı kodu artıq istifadə olunur."); event.consume(); return;
            }
            BigDecimal debtAmount;
            try {
                debtAmount = positiveOrZero(debt.getText());
            } catch (IllegalArgumentException exception) {
                error.setText("Borcu 0 və ya müsbət rəqəm kimi yazın."); event.consume(); return;
            }
            Supplier supplier = new Supplier(code.getText().trim(), name.getText().trim(), phone.getText().trim(), debtAmount);
            if (editing) suppliers.set(suppliers.indexOf(existing), supplier);
            else suppliers.add(supplier);
            persistSupplier(supplier);
            table.getSelectionModel().select(supplier);
        });
        dialog.showAndWait();
        showSuppliers();
    }

    private void deleteSupplier(Supplier supplier) {
        if (supplier == null) return;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Təchizatçını sil");
        confirmation.setHeaderText("“" + supplier.name() + "” silinsin?");
        confirmation.setContentText("Bu əməliyyatı geri qaytarmaq mümkün deyil.");
        confirmation.showAndWait().ifPresent(answer -> {
            if (answer == ButtonType.OK) {
                try {
                    Database.deleteSupplier(supplier.code());
                    suppliers.remove(supplier);
                    addLog("Təchizatçı silindi", "Təchizatçı: " + supplier.name() + " (" + supplier.code() + ")");
                    showSuppliers();
                } catch (Exception exception) {
                    showMessage(Alert.AlertType.ERROR, "Təchizatçı silinmədi", "Baza xətası: " + exception.getMessage());
                }
            }
        });
    }

    private void showEmployees() {
        pageTitle.setText("İşçilər");
        Button add = new Button("+ Yeni işçi");
        add.getStyleClass().add("primary-button");
        Button edit = new Button("Düzəliş et");
        edit.getStyleClass().add("secondary-button");
        Button delete = new Button("Sil");
        delete.getStyleClass().add("danger-button");
        HBox toolbar = new HBox(12, add, edit, delete);

        TableView<Employee> table = new TableView<>(employees);
        table.getColumns().addAll(
                column("Kod", Employee::code),
                column("Ad və soyad", Employee::name),
                column("Vəzifə", Employee::position),
                column("Telefon", Employee::phone),
                column("Sabit aylıq maaş", emp -> formatMoney(emp.monthlySalary()))
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Hələ işçi yoxdur"));

        add.setOnAction(event -> openEmployeeDialog(null, table));
        edit.setOnAction(event -> openEmployeeDialog(table.getSelectionModel().getSelectedItem(), table));
        delete.setOnAction(event -> deleteEmployee(table.getSelectionModel().getSelectedItem()));
        edit.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        delete.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        table.setMinHeight(420);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox content = new VBox(18, toolbar, table);
        content.getStyleClass().add("content");
        setPageContent(content);
    }

    private void openEmployeeDialog(Employee existing, TableView<Employee> table) {
        boolean editing = existing != null;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(editing ? "İşçiyə düzəliş" : "Yeni işçi əlavə et");
        dialog.setHeaderText(editing ? "İşçi məlumatlarını yeniləyin" : "Maaş verilməsi üçün yeni işçi məlumatlarını daxil edin");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        TextField code = new TextField(editing ? existing.code() : "");
        code.setDisable(editing);
        code.setPromptText("Məsələn: I-002");
        TextField name = new TextField(editing ? existing.name() : "");
        name.setPromptText("Ad və soyad");
        TextField position = new TextField(editing ? existing.position() : "");
        position.setPromptText("Məsələn: Usta / Sürücü / Operator");
        TextField phone = new TextField(editing ? existing.phone() : "");
        phone.setPromptText("+994 50 123 45 67");
        TextField salary = new TextField(editing ? existing.monthlySalary().toPlainString() : "0.00");
        salary.setPromptText("0.00");
        Label error = new Label();
        error.getStyleClass().add("error-label");

        VBox form = new VBox(8,
                new Label("İşçi kodu *"), code,
                new Label("Ad və soyad *"), name,
                new Label("Vəzifə"), position,
                new Label("Telefon"), phone,
                new Label("Sabit aylıq maaş (₼) *"), salary,
                error);
        form.setPrefWidth(390);
        dialog.getDialogPane().setContent(form);

        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String nCode = code.getText().trim();
            String nName = name.getText().trim();
            if (nCode.isEmpty() || nName.isEmpty()) {
                error.setText("Kod və ad mütləq daxil edilməlidir.");
                event.consume();
                return;
            }
            if (employees.stream().anyMatch(e -> e.code().equalsIgnoreCase(nCode) && e != existing)) {
                error.setText("Bu işçi kodu artıq istifadə olunur.");
                event.consume();
                return;
            }
            BigDecimal salaryAmount;
            try {
                salaryAmount = positiveOrZero(salary.getText());
            } catch (Exception ex) {
                error.setText("Maaşı düzgün müsbət rəqəm kimi yazın.");
                event.consume();
                return;
            }
            Employee saved = new Employee(nCode, nName, position.getText().trim(), phone.getText().trim(), salaryAmount);
            if (editing) employees.set(employees.indexOf(existing), saved);
            else employees.add(saved);
            persistEmployee(saved);
            table.getSelectionModel().select(saved);
        });
        dialog.showAndWait();
        showEmployees();
    }

    private void deleteEmployee(Employee employee) {
        if (employee == null) return;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("İşçini sil");
        confirmation.setHeaderText("“" + employee.name() + "” silinsin?");
        confirmation.setContentText("Bu əməliyyatı geri qaytarmaq mümkün deyil.");
        confirmation.showAndWait().ifPresent(answer -> {
            if (answer == ButtonType.OK) {
                try {
                    Database.deleteEmployee(employee.code());
                    employees.remove(employee);
                    addLog("İşçi silindi", "İşçi: " + employee.name() + " (" + employee.code() + ")");
                    showEmployees();
                } catch (Exception exception) {
                    showMessage(Alert.AlertType.ERROR, "İşçi silinmədi", "Baza xətası: " + exception.getMessage());
                }
            }
        });
    }

    private void showSalaries() {
        pageTitle.setText("Maaşlar və Avanslar");
        Label intro = new Label("İşçilərə ödənilən müntəzəm maaş və tez maaş (avans) ödənişlərini buradan qeyd edin. Hər bir ödəniş avtomatik olaraq PostgreSQL bazasına yazılır və aylıq maliyyə hesabatında əks olunur.");
        intro.setWrapText(true);

        Button paySalary = new Button("+ Maaş ödənişi / Avans");
        paySalary.getStyleClass().add("primary-button");
        paySalary.setOnAction(event -> openSalaryPaymentDialog());

        Button addEmp = new Button("+ İşçi əlavə et");
        addEmp.getStyleClass().add("secondary-button");
        addEmp.setOnAction(event -> showEmployees());

        TextField search = new TextField();
        search.setPromptText("İşçi adı, kodu və ya vəzifə ilə axtarın...");
        search.getStyleClass().add("search-field");
        HBox toolbar = new HBox(12, search, paySalary, addEmp);
        HBox.setHgrow(search, Priority.ALWAYS);

        ObservableList<SalaryRecord> filteredSalaries = FXCollections.observableArrayList(salaryRecords);
        search.textProperty().addListener((obs, oldVal, newVal) -> {
            String query = newVal == null ? "" : newVal.trim().toLowerCase();
            filteredSalaries.clear();
            if (query.isEmpty()) {
                filteredSalaries.addAll(salaryRecords);
            } else {
                for (SalaryRecord s : salaryRecords) {
                    if (s.employeeName().toLowerCase().contains(query) || s.employeeCode().toLowerCase().contains(query) || s.position().toLowerCase().contains(query) || s.type().toLowerCase().contains(query)) {
                        filteredSalaries.add(s);
                    }
                }
            }
        });

        TableView<SalaryRecord> table = new TableView<>(filteredSalaries);
        table.setPlaceholder(new Label("Hələ heç bir maaş və ya avans ödənişi qeydə alınmayıb"));
        table.getColumns().addAll(
                column("Kod", SalaryRecord::employeeCode),
                column("İşçi", SalaryRecord::employeeName),
                column("Vəzifə", SalaryRecord::position),
                column("Ödəniş növü", SalaryRecord::type),
                column("Məbləğ", sr -> formatMoney(sr.amount())),
                column("Qeyd", SalaryRecord::note),
                column("Tarix", SalaryRecord::createdAt)
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Button deleteBtn = new Button("Sil");
        deleteBtn.getStyleClass().add("danger-button");
        deleteBtn.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        deleteBtn.setOnAction(event -> deleteSalaryRecord(table.getSelectionModel().getSelectedItem()));

        HBox actionsRow = new HBox(12, deleteBtn);

        table.setMinHeight(420);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox content = new VBox(16, intro, toolbar, table, actionsRow);
        content.getStyleClass().add("content");
        setPageContent(content);
    }

    private void openSalaryPaymentDialog() {
        if (employees.isEmpty()) {
            showMessage(Alert.AlertType.INFORMATION, "İşçi tapılmadı", "Əvvəl “İşçilər” bölməsindən işçi əlavə edin.");
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Maaş və ya Avans Ödənişi");
        dialog.setHeaderText("İşçiyə verilən maaşı və ya tez ödənilən avans məbləğini daxil edin");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        ComboBox<Employee> employeeCombo = new ComboBox<>(employees);
        employeeCombo.setPromptText("İşçini seçin");
        employeeCombo.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("Tez maaş (Avans)", "Aylıq Maaş", "Əlavə ödəniş (Mükafat / Bonus)");
        typeCombo.setValue("Tez maaş (Avans)");
        typeCombo.setMaxWidth(Double.MAX_VALUE);

        TextField amount = new TextField();
        amount.setPromptText("Məsələn: 250.00");

        TextField note = new TextField();
        note.setPromptText("Məsələn: Ay ortasında təcili ehtiyac üçün tez götürülən avans");

        Label infoLabel = new Label();
        infoLabel.getStyleClass().add("transaction-detail");

        employeeCombo.setOnAction(event -> {
            Employee emp = employeeCombo.getValue();
            if (emp != null) {
                infoLabel.setText("Sabit aylıq maaş: " + formatMoney(emp.monthlySalary()));
            }
        });

        Label error = new Label();
        error.getStyleClass().add("error-label");

        VBox form = new VBox(8,
                new Label("İşçi *"), employeeCombo,
                infoLabel,
                new Label("Ödəniş növü *"), typeCombo,
                new Label("Ödəniş məbləği (₼) *"), amount,
                new Label("Qeyd"), note,
                error);
        form.setPrefWidth(420);
        dialog.getDialogPane().setContent(form);

        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                if (employeeCombo.getValue() == null) throw new IllegalArgumentException("İşçi seçilməlidir.");
                Employee selectedEmp = employeeCombo.getValue();
                BigDecimal payAmount = strictlyPositive(amount.getText());
                String pType = typeCombo.getValue();
                String pNote = note.getText().trim();

                LocalDateTime occurredAt = LocalDateTime.now();
                long salaryId = 0;
                if (Database.isConfigured()) {
                    try {
                        salaryId = Database.recordSalary(selectedEmp.code(), payAmount, pType, pNote, occurredAt);
                    } catch (Exception dbEx) {
                        System.err.println("Maaş bazaya yazılmadı: " + dbEx.getMessage());
                    }
                }

                SalaryRecord record = new SalaryRecord(
                        salaryId,
                        selectedEmp.code(),
                        selectedEmp.name(),
                        selectedEmp.position(),
                        payAmount,
                        pType,
                        pNote,
                        formatTimestamp(occurredAt)
                );
                salaryRecords.add(record);

                // Add cash movement expense
                addCashMovement(CashDirection.EXPENSE, payAmount, pType + ": " + selectedEmp.name() + (pNote.isEmpty() ? "" : " (" + pNote + ")"));
                addLog("Maaş ödənişi", pType + " — " + selectedEmp.name() + ": " + formatMoney(payAmount));

                showMessage(Alert.AlertType.INFORMATION, "Ödəniş qeydə alındı",
                        selectedEmp.name() + " üçün " + formatMoney(payAmount) + " " + pType.toLowerCase() + " ödənişi qeyd edildi.");
            } catch (IllegalArgumentException exception) {
                error.setText(exception.getMessage() == null ? "Məlumatları düzgün doldurun." : exception.getMessage());
                event.consume();
            }
        });
        dialog.showAndWait();
        showSalaries();
    }

    private void deleteSalaryRecord(SalaryRecord record) {
        if (record == null) return;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Maaş qeydini sil");
        confirmation.setHeaderText("“" + record.employeeName() + " — " + formatMoney(record.amount()) + "” ödəniş silinsin?");
        confirmation.setContentText("Bu əməliyyatı geri qaytarmaq mümkün deyil.");
        confirmation.showAndWait().ifPresent(answer -> {
            if (answer == ButtonType.OK) {
                try {
                    if (record.id() > 0 && Database.isConfigured()) {
                        Database.deleteSalary(record.id());
                    }

                    // Remove from main memory salaryRecords list by ID or matching fields
                    salaryRecords.removeIf(s -> (record.id() > 0 && s.id() == record.id())
                            || (s.employeeCode().equals(record.employeeCode())
                                && s.amount().compareTo(record.amount()) == 0
                                && (record.id() == 0 || s.id() == record.id())));

                    // Reverse cash expense movement for salary in memory once
                    CashMovement cmMatch = cashMovements.stream()
                            .filter(cm -> cm.direction() == CashDirection.EXPENSE
                                    && cm.amount().compareTo(record.amount()) == 0
                                    && cm.description().contains(record.employeeName()))
                            .findFirst().orElse(null);
                    if (cmMatch != null) {
                        cashMovements.remove(cmMatch);
                    }

                    addLog("Maaş ödənişi silindi", record.type() + ": " + record.employeeName() + " — " + formatMoney(record.amount()));
                    showSalaries();
                } catch (Exception exception) {
                    showMessage(Alert.AlertType.ERROR, "Ödəniş silinmədi", "Baza xətası: " + exception.getMessage());
                }
            }
        });
    }

    private void showCreditPayments() {
        pageTitle.setText("Kreditlər Və Aylıq Kredit Ödənişləri");
        Label intro = new Label("Bank kreditləri, maşın/avadanlıq lizinqləri və ya müntəzəm borclarınızı müqavilə kimi əlavə edib, hər ay ödənilməli olan məbləğləri və ödəniş tarixçəsini idarə edə bilərsiniz. Bütün məlumatlar PostgreSQL bazasında saxlanılır və aylıq hesabatda əks olunur.");
        intro.setWrapText(true);

        Button addContract = new Button("+ Yeni Kredit Təyinatı");
        addContract.getStyleClass().add("primary-button");
        addContract.setOnAction(event -> openCreditContractDialog(null));

        Button addPayment = new Button("+ Aylıq Kredit Ödənişi Et");
        addPayment.getStyleClass().add("primary-button");
        addPayment.setOnAction(event -> openCreditPaymentDialog());

        TextField searchContract = new TextField();
        searchContract.setPromptText("Kredit adı, bank və ya kod ilə axtarın...");
        searchContract.getStyleClass().add("search-field");
        HBox contractToolbar = new HBox(12, searchContract, addContract, addPayment);
        HBox.setHgrow(searchContract, Priority.ALWAYS);

        ObservableList<CreditContract> filteredContracts = FXCollections.observableArrayList(credits);
        searchContract.textProperty().addListener((obs, oldVal, newVal) -> {
            String query = newVal == null ? "" : newVal.trim().toLowerCase();
            filteredContracts.clear();
            if (query.isEmpty()) {
                filteredContracts.addAll(credits);
            } else {
                for (CreditContract c : credits) {
                    if (c.code().toLowerCase().contains(query) || c.name().toLowerCase().contains(query) || c.bankOrCreditor().toLowerCase().contains(query)) {
                        filteredContracts.add(c);
                    }
                }
            }
        });

        Label contractsSectionTitle = new Label("Kredit Müqavilələri Və Öhdəliklər (" + credits.size() + " kredit)");
        contractsSectionTitle.getStyleClass().add("section-title");

        TableView<CreditContract> contractTable = new TableView<>(filteredContracts);
        contractTable.setPlaceholder(new Label("Hələ heç bir kredit müqaviləsi əlavə olunmayıb"));
        contractTable.getColumns().addAll(
                column("Kod", CreditContract::code),
                column("Kredit / Borc Adı", CreditContract::name),
                column("Bank / Kredit Verən", CreditContract::bankOrCreditor),
                column("Ümumi Kredit Dəyəri", c -> formatMoney(c.totalAmount())),
                column("Aylıq Sabit Ödəniş", c -> formatMoney(c.monthlyAmount())),
                column("Cari Qalıq Borc", c -> formatMoney(c.remainingBalance()))
        );
        contractTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        contractTable.setMinHeight(260);
        contractTable.setPrefHeight(320);

        Button editContractBtn = new Button("Düzəliş Et");
        editContractBtn.disableProperty().bind(contractTable.getSelectionModel().selectedItemProperty().isNull());
        editContractBtn.setOnAction(event -> openCreditContractDialog(contractTable.getSelectionModel().getSelectedItem()));

        Button deleteContractBtn = new Button("Sil");
        deleteContractBtn.getStyleClass().add("danger-button");
        deleteContractBtn.disableProperty().bind(contractTable.getSelectionModel().selectedItemProperty().isNull());
        deleteContractBtn.setOnAction(event -> deleteCreditContract(contractTable.getSelectionModel().getSelectedItem()));

        HBox contractActionsRow = new HBox(12, editContractBtn, deleteContractBtn);

        // Payment history table
        Label historySectionTitle = new Label("Kredit Ödənişləri Tarixçəsi");
        historySectionTitle.getStyleClass().add("section-title");

        TableView<CreditPaymentRecord> paymentTable = new TableView<>(creditPayments);
        paymentTable.setPlaceholder(new Label("Hələ heç bir kredit ödənişi edilməyib"));
        paymentTable.getColumns().addAll(
                column("Kredit Kodu", CreditPaymentRecord::creditCode),
                column("Kredit / Borc adı", CreditPaymentRecord::creditName),
                column("Bank / Kredit verən", CreditPaymentRecord::bankOrCreditor),
                column("Aylıq Müqavilə Ödənişi", cp -> formatMoney(cp.monthlyInstallment())),
                column("Bu dəfə Ödənilən Məbləğ", cp -> formatMoney(cp.paidAmount())),
                column("Qeyd", CreditPaymentRecord::note),
                column("Tarix", CreditPaymentRecord::createdAt)
        );
        paymentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        paymentTable.setMinHeight(300);
        paymentTable.setPrefHeight(380);

        Button deletePaymentBtn = new Button("Ödəniş Qeydini Sil");
        deletePaymentBtn.getStyleClass().add("danger-button");
        deletePaymentBtn.disableProperty().bind(paymentTable.getSelectionModel().selectedItemProperty().isNull());
        deletePaymentBtn.setOnAction(event -> deleteCreditPaymentRecord(paymentTable.getSelectionModel().getSelectedItem()));

        HBox paymentActionsRow = new HBox(12, deletePaymentBtn);

        VBox.setVgrow(contractTable, Priority.ALWAYS);
        VBox.setVgrow(paymentTable, Priority.ALWAYS);
        VBox content = new VBox(16, intro, contractToolbar, contractsSectionTitle, contractTable, contractActionsRow, historySectionTitle, paymentTable, paymentActionsRow);
        content.getStyleClass().add("content");
        setPageContent(content);
    }

    private void openCreditContractDialog(CreditContract existing) {
        Dialog<ButtonType> dialog = new Dialog<>();
        boolean isEdit = existing != null;
        dialog.setTitle(isEdit ? "Kredit Müqaviləsinə Düzəliş" : "Yeni Kredit Müqaviləsi");
        dialog.setHeaderText(isEdit ? "Kredit müqaviləsinin parametrini dəyişin" : "Yeni kredit borcunu və aylıq ödəniş şərtlərini daxil edin");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        TextField code = new TextField(isEdit ? existing.code() : ("K-" + String.format("%03d", credits.size() + 1)));
        code.setPromptText("Kredit kodu");
        if (isEdit) code.setDisable(true);

        TextField name = new TextField(isEdit ? existing.name() : "");
        name.setPromptText("Məsələn: ABB Bank İstehsalat Krediti");

        TextField bank = new TextField(isEdit ? existing.bankOrCreditor() : "");
        bank.setPromptText("Məsələn: ABB Bank / Kapital Bank / Lizinq MMC");

        TextField totalAmount = new TextField(isEdit ? existing.totalAmount().toPlainString() : "");
        totalAmount.setPromptText("Məsələn: 12000.00 (Ümumi borc məbləği)");

        TextField monthlyAmount = new TextField(isEdit ? existing.monthlyAmount().toPlainString() : "");
        monthlyAmount.setPromptText("Məsələn: 1000.00 (Aylıq ödənilməli olan xərc)");

        TextField remainingBalance = new TextField(isEdit ? existing.remainingBalance().toPlainString() : "");
        remainingBalance.setPromptText("Məsələn: 12000.00 (Cari qalıq borc)");

        Label error = new Label();
        error.getStyleClass().add("error-label");

        VBox form = new VBox(8,
                new Label("Kredit kodu *"), code,
                new Label("Kredit / Borc adı *"), name,
                new Label("Bank və ya Kredit verən *"), bank,
                new Label("Ümumi Kredit Məbləği (₼) *"), totalAmount,
                new Label("Aylıq Ödənilməli Sabit Məbləğ (₼) *"), monthlyAmount,
                new Label("Cari Qalıq Borc (₼) *"), remainingBalance,
                error);
        form.setPrefWidth(420);
        dialog.getDialogPane().setContent(form);

        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                String cCode = code.getText().trim();
                String cName = name.getText().trim();
                String cBank = bank.getText().trim();
                if (cCode.isEmpty() || cName.isEmpty() || cBank.isEmpty()) {
                    throw new IllegalArgumentException("Kod, kredit adı və bank adı mütləq daxil edilməlidir.");
                }
                BigDecimal total = strictlyPositive(totalAmount.getText());
                BigDecimal monthly = strictlyPositive(monthlyAmount.getText());
                BigDecimal remaining = positiveOrZero(remainingBalance.getText());

                CreditContract contract = new CreditContract(cCode, cName, cBank, total, monthly, remaining);
                if (isEdit) {
                    int index = credits.indexOf(existing);
                    if (index >= 0) credits.set(index, contract);
                } else {
                    for (CreditContract existingCredit : credits) {
                        if (existingCredit.code().equalsIgnoreCase(cCode)) {
                            throw new IllegalArgumentException("Bu kodla (" + cCode + ") kredit artıq mövcuddur.");
                        }
                    }
                    credits.add(contract);
                }

                persistCredit(contract);
                addLog(isEdit ? "Kredit dəyişdirildi" : "Yeni kredit əlavə edildi", cCode + " — " + cName);

                showMessage(Alert.AlertType.INFORMATION, "Uğurlu", "Kredit müqaviləsi uğurla saxlanıldı.");
            } catch (IllegalArgumentException exception) {
                error.setText(exception.getMessage() == null ? "Məlumatları düzgün doldurun." : exception.getMessage());
                event.consume();
            }
        });
        dialog.showAndWait();
        showCreditPayments();
    }

    private void deleteCreditContract(CreditContract contract) {
        if (contract == null) return;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Kredit müqaviləsini sil");
        confirmation.setHeaderText("“" + contract.name() + " (" + contract.code() + ")” müqaviləsi silinsin?");
        confirmation.setContentText("Bu kredit müqaviləsi bazadan silinəcək.");
        confirmation.showAndWait().ifPresent(answer -> {
            if (answer == ButtonType.OK) {
                try {
                    if (Database.isConfigured()) {
                        Database.deleteCredit(contract.code());
                    }
                    credits.remove(contract);
                    addLog("Kredit silindi", contract.code() + " — " + contract.name());
                    showCreditPayments();
                } catch (Exception exception) {
                    showMessage(Alert.AlertType.ERROR, "Kredit silinmədi", "Baza xətası: " + exception.getMessage());
                }
            }
        });
    }

    private void openCreditPaymentDialog() {
        if (credits.isEmpty()) {
            showMessage(Alert.AlertType.INFORMATION, "Kredit tapılmadı", "Əvvəl “+ Yeni Kredit Təyinatı” düyməsi ilə kredit müqaviləsi daxil edin.");
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Aylıq Kredit Ödənişi");
        dialog.setHeaderText("Seçilmiş kredit üzrə aylıq ödənişi daxil edin");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        ComboBox<CreditContract> creditCombo = new ComboBox<>(credits);
        creditCombo.setPromptText("Kredit müqaviləsini seçin");
        creditCombo.setMaxWidth(Double.MAX_VALUE);

        TextField paidAmount = new TextField();
        paidAmount.setPromptText("Məsələn: 1000.00 (Bu ay ödənilən nağd pul)");

        TextField note = new TextField();
        note.setPromptText("Məsələn: Avqust ayı kredit ödənişi");

        Label infoLabel = new Label();
        infoLabel.getStyleClass().add("transaction-detail");

        creditCombo.setOnAction(event -> {
            CreditContract c = creditCombo.getValue();
            if (c != null) {
                paidAmount.setText(c.monthlyAmount().toPlainString());
                infoLabel.setText("Aylıq müqavilə ödənişi: " + formatMoney(c.monthlyAmount()) + "  •  Cari qalıq borc: " + formatMoney(c.remainingBalance()));
            }
        });

        Label error = new Label();
        error.getStyleClass().add("error-label");

        VBox form = new VBox(8,
                new Label("Kredit Müqaviləsi *"), creditCombo,
                infoLabel,
                new Label("Bu ay ödənilən məbləğ (₼) *"), paidAmount,
                new Label("Qeyd"), note,
                error);
        form.setPrefWidth(420);
        dialog.getDialogPane().setContent(form);

        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                CreditContract selectedCredit = creditCombo.getValue();
                if (selectedCredit == null) throw new IllegalArgumentException("Kredit müqaviləsini seçin.");

                BigDecimal paid = strictlyPositive(paidAmount.getText());
                String pNote = note.getText().trim();

                LocalDateTime occurredAt = LocalDateTime.now();
                long paymentId = 0;
                if (Database.isConfigured()) {
                    try {
                        paymentId = Database.recordCreditPayment(
                                selectedCredit.code(),
                                selectedCredit.name(),
                                selectedCredit.bankOrCreditor(),
                                selectedCredit.monthlyAmount(),
                                paid,
                                pNote,
                                occurredAt
                        );
                    } catch (Exception dbEx) {
                        System.err.println("Kredit ödənişi bazaya yazılmadı: " + dbEx.getMessage());
                    }
                }

                // Update remaining balance on local memory
                BigDecimal newRemaining = selectedCredit.remainingBalance().subtract(paid);
                if (newRemaining.compareTo(BigDecimal.ZERO) < 0) newRemaining = BigDecimal.ZERO;
                CreditContract updatedContract = new CreditContract(
                        selectedCredit.code(),
                        selectedCredit.name(),
                        selectedCredit.bankOrCreditor(),
                        selectedCredit.totalAmount(),
                        selectedCredit.monthlyAmount(),
                        newRemaining
                );
                int index = credits.indexOf(selectedCredit);
                if (index >= 0) credits.set(index, updatedContract);

                CreditPaymentRecord record = new CreditPaymentRecord(
                        paymentId,
                        selectedCredit.code(),
                        selectedCredit.name(),
                        selectedCredit.bankOrCreditor(),
                        selectedCredit.monthlyAmount(),
                        paid,
                        pNote,
                        formatTimestamp(occurredAt)
                );
                creditPayments.add(0, record);

                // Add cash movement expense
                addCashMovement(CashDirection.EXPENSE, paid, "Aylıq kredit ödənişi: " + selectedCredit.name() + " (" + selectedCredit.bankOrCreditor() + ")");
                addLog("Kredit ödənişi", selectedCredit.name() + " — Ödənilən: " + formatMoney(paid));

                showMessage(Alert.AlertType.INFORMATION, "Kredit Ödənişi Qeydə Alındı",
                        selectedCredit.name() + " üçün " + formatMoney(paid) + " kredit ödənişi uğurla bazaya yazıldı.");
            } catch (IllegalArgumentException exception) {
                error.setText(exception.getMessage() == null ? "Məlumatları düzgün doldurun." : exception.getMessage());
                event.consume();
            }
        });
        dialog.showAndWait();
        showCreditPayments();
    }

    private void deleteCreditPaymentRecord(CreditPaymentRecord record) {
        if (record == null) return;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Kredit ödənişini sil");
        confirmation.setHeaderText("“" + record.creditName() + " — " + formatMoney(record.paidAmount()) + "” ödənişi silinsin?");
        confirmation.setContentText("Bu əməliyyatı geri qaytarmaq mümkün deyil.");
        confirmation.showAndWait().ifPresent(answer -> {
            if (answer == ButtonType.OK) {
                try {
                    if (record.id() > 0 && Database.isConfigured()) {
                        Database.deleteCreditPayment(record.id());
                    }

                    // Remove from main memory creditPayments list by ID or matching fields
                    creditPayments.removeIf(c -> (record.id() > 0 && c.id() == record.id())
                            || (c.creditCode().equals(record.creditCode())
                                && c.paidAmount().compareTo(record.paidAmount()) == 0
                                && (record.id() == 0 || c.id() == record.id())));

                    // Restore memory credit contract remaining balance
                    CreditContract cc = credits.stream().filter(c -> c.code().equals(record.creditCode())).findFirst().orElse(null);
                    if (cc != null) {
                        int cIdx = credits.indexOf(cc);
                        if (cIdx >= 0) {
                            credits.set(cIdx, new CreditContract(cc.code(), cc.name(), cc.bankOrCreditor(), cc.totalAmount(), cc.monthlyAmount(), cc.remainingBalance().add(record.paidAmount())));
                        }
                    }

                    // Reverse cash expense movement for credit payment in memory once
                    CashMovement cmMatch = cashMovements.stream()
                            .filter(cm -> cm.direction() == CashDirection.EXPENSE
                                    && cm.amount().compareTo(record.paidAmount()) == 0
                                    && cm.description().contains(record.creditName()))
                            .findFirst().orElse(null);
                    if (cmMatch != null) {
                        cashMovements.remove(cmMatch);
                    }

                    addLog("Kredit ödənişi silindi", record.creditName() + " — " + formatMoney(record.paidAmount()));
                    showCreditPayments();
                } catch (Exception exception) {
                    showMessage(Alert.AlertType.ERROR, "Ödəniş silinmədi", "Baza xətası: " + exception.getMessage());
                }
            }
        });
    }

    private void openPurchaseDialog() {
        if (suppliers.isEmpty() || rawMaterials.isEmpty()) {
            showMessage(Alert.AlertType.INFORMATION, "Alış üçün məlumat çatmır",
                    "Əvvəl “Təchizatçılar” bölməsindən təchizatçı, “Xammallar” bölməsindən isə xammal əlavə edin.");
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Xammal alışı");
        dialog.setHeaderText("Təchizatçıdan alınan xammalı və ödənişi qeyd edin");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        ComboBox<Supplier> supplier = new ComboBox<>(suppliers);
        supplier.setPromptText("Təchizatçını seçin"); supplier.setMaxWidth(Double.MAX_VALUE);
        ComboBox<RawMaterial> material = new ComboBox<>(rawMaterials);
        material.setPromptText("Xammalı seçin"); material.setMaxWidth(Double.MAX_VALUE);
        TextField quantity = new TextField(); quantity.setPromptText("Məsələn: 100");
        TextField total = new TextField(); total.setPromptText("Məsələn: 250.00");
        TextField paid = new TextField("0.00");
        Label error = new Label(); error.getStyleClass().add("error-label");
        VBox form = new VBox(8, new Label("Təchizatçı *"), supplier, new Label("Xammal *"), material,
                new Label("Alınan miqdar *"), quantity, new Label("Ümumi alış məbləği (₼) *"), total,
                new Label("İndi ödənilən nağd məbləğ (₼)"), paid, error);
        form.setPrefWidth(420);
        dialog.getDialogPane().setContent(form);
        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                if (supplier.getValue() == null || material.getValue() == null) throw new IllegalArgumentException("Təchizatçı və xammal mütləq seçilməlidir.");
                BigDecimal purchased = strictlyPositive(quantity.getText());
                BigDecimal totalAmount = positiveOrZero(total.getText());
                BigDecimal paidAmount = positiveOrZero(paid.getText());
                if (paidAmount.compareTo(totalAmount) > 0) throw new IllegalArgumentException("Ödəniş ümumi alış məbləğindən çox ola bilməz.");

                RawMaterial oldMaterial = material.getValue();
                int materialIndex = rawMaterials.indexOf(oldMaterial);
                BigDecimal purchaseUnitCost = totalAmount.divide(purchased, 4, RoundingMode.HALF_UP);
                rawMaterials.set(materialIndex, new RawMaterial(oldMaterial.code(), oldMaterial.name(), oldMaterial.unit(),
                        oldMaterial.stock().add(purchased), weightedAverage(oldMaterial.stock(), oldMaterial.unitCost(), purchased, purchaseUnitCost)));
                persistInventoryQuietly();

                Supplier oldSupplier = supplier.getValue();
                int supplierIndex = suppliers.indexOf(oldSupplier);
                BigDecimal newDebt = oldSupplier.debt().add(totalAmount.subtract(paidAmount));
                Supplier updatedSupplier = new Supplier(oldSupplier.code(), oldSupplier.name(), oldSupplier.phone(), newDebt);
                suppliers.set(supplierIndex, updatedSupplier);
                persistSupplier(updatedSupplier);

                if (paidAmount.signum() > 0) {
                    addCashMovement(CashDirection.EXPENSE, paidAmount, "Xammal alışında ilkin ödəniş: " + oldSupplier.name());
                    try {
                        long paymentId = Database.recordPayment("Xammal alışında ödəniş", null, oldSupplier.code(), null, paidAmount, LocalDateTime.now());
                        payments.add(new Payment(paymentId, "Təchizatçıya ödəniş", "", oldSupplier.code(), oldSupplier.name(), paidAmount, "", now()));
                    } catch (Exception dbEx) {
                        System.err.println("Ödəniş bazaya yazılmadı: " + dbEx.getMessage());
                    }
                }

                LocalDateTime purchaseTime = LocalDateTime.now();
                long purchaseId = 0;
                if (Database.isConfigured()) {
                    try {
                        purchaseId = Database.recordPurchase(null, oldSupplier.code(), oldMaterial.code(), purchased, totalAmount, paidAmount, purchaseTime);
                    } catch (Exception dbEx) {
                        System.err.println("Alış bazaya yazılmadı: " + dbEx.getMessage());
                    }
                }
                purchases.add(new Purchase(
                        purchaseId,
                        null,
                        oldSupplier.code(),
                        oldSupplier.name(),
                        oldMaterial.code(),
                        oldMaterial.name(),
                        oldMaterial.unit(),
                        purchased,
                        totalAmount,
                        paidAmount,
                        totalAmount.subtract(paidAmount),
                        formatTimestamp(purchaseTime)
                ));

                addLog("Xammal alışı", oldSupplier.name() + " — " + oldMaterial.name() + ": " + formatQuantity(purchased)
                        + " " + oldMaterial.unit() + ", cəmi " + formatMoney(totalAmount));

                showMessage(Alert.AlertType.INFORMATION, "Xammal anbara daxil edildi",
                        formatQuantity(purchased) + " " + oldMaterial.unit() + " “" + oldMaterial.name() + "” anbara əlavə edildi."
                                + " Təchizatçı borcu: " + formatMoney(newDebt));
            } catch (IllegalArgumentException exception) {
                error.setText(exception.getMessage() == null ? "Məlumatları düzgün doldurun." : exception.getMessage());
                event.consume();
            }
        });
        dialog.showAndWait();
    }

    private void openRawMaterialDialog(RawMaterial existing, TableView<RawMaterial> table) {
        boolean editing = existing != null;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(editing ? "Xammala düzəliş" : "Xammal əlavə et");
        dialog.setHeaderText(editing ? "Xammal məlumatlarını yeniləyin" : "Anbara yeni xammal növü qeyd edin");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        TextField code = new TextField(editing ? existing.code() : "");
        code.setDisable(editing);
        code.setPromptText("Məsələn: X-003");
        TextField name = new TextField(editing ? existing.name() : "");
        name.setPromptText("Xammalın adı");
        TextField unit = new TextField(editing ? existing.unit() : "kq");
        TextField quantity = new TextField(editing ? existing.stock().toPlainString() : "0");
        TextField unitCost = new TextField(editing ? existing.unitCost().toPlainString() : "0.00");
        Label error = new Label();
        error.getStyleClass().add("error-label");
        VBox form = new VBox(8, new Label("Xammal kodu *"), code, new Label("Ad *"), name,
                new Label("Vahid (kq, ədəd, metr...) *"), unit, new Label("Anbardakı miqdar *"), quantity,
                new Label("1 vahidin maya dəyəri (₼)"), unitCost, error);
        form.setPrefWidth(390);
        dialog.getDialogPane().setContent(form);
        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                BigDecimal stock = positiveOrZero(quantity.getText());
                BigDecimal cost = positiveOrZero(unitCost.getText());
                if (code.getText().isBlank() || name.getText().isBlank() || unit.getText().isBlank()) throw new IllegalArgumentException("required");
                if (!editing && rawMaterials.stream().anyMatch(material -> material.code().equalsIgnoreCase(code.getText().trim()))) {
                    error.setText("Bu xammal kodu artıq istifadə olunur.");
                    event.consume(); return;
                }
                RawMaterial material = new RawMaterial(code.getText().trim(), name.getText().trim(), unit.getText().trim(), stock, cost);
                if (editing) {
                    rawMaterials.set(rawMaterials.indexOf(existing), material);
                } else {
                    rawMaterials.add(material);
                }
                persistInventoryQuietly();
                table.getSelectionModel().select(material);
            } catch (IllegalArgumentException exception) {
                error.setText("Bütün xanaları doldurun; miqdar və dəyər 0 və ya müsbət rəqəm olmalıdır.");
                event.consume();
            }
        });
        dialog.showAndWait();
        showRawMaterials();
    }

    private void deleteRawMaterial(RawMaterial material) {
        if (material == null) return;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Xammalı sil");
        confirmation.setHeaderText("“" + material.name() + "” silinsin?");
        confirmation.setContentText("Bu xammal və onunla bağlı reseptlər silinəcək.");
        confirmation.showAndWait().ifPresent(answer -> {
            if (answer == ButtonType.OK) {
                try {
                    Database.deleteRawMaterial(material.code());
                    rawMaterials.remove(material);
                    addLog("Xammal silindi", "Xammal: " + material.name() + " (" + material.code() + ")");
                    showRawMaterials();
                } catch (Exception exception) {
                    showMessage(Alert.AlertType.ERROR, "Xammal silinmədi", "Baza xətası: " + exception.getMessage());
                }
            }
        });
    }

    private void showProducts() {
        pageTitle.setText("Məhsullar və reseptlər");
        Button add = new Button("+ Məhsul və resept əlavə et");
        add.getStyleClass().add("primary-button");
        Button delete = new Button("Sil");
        delete.getStyleClass().add("danger-button");
        HBox toolbar = new HBox(12, add, delete);

        TableView<Product> table = new TableView<>(products);
        table.getColumns().addAll(column("Kod", Product::code), column("Məhsul", Product::name),
                column("Resept", this::formulaText), column("Hazır məhsul sayı", product -> formatQuantity(product.stock())),
                column("Təhvil qiyməti", product -> formatMoney(product.salePrice())),
                column("İstehsal dəyəri", product -> formatMoney(product.unitCost())));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Hələ məhsul və resept yoxdur"));

        add.setOnAction(event -> openProductDialog(table));
        delete.setOnAction(event -> deleteProduct(table.getSelectionModel().getSelectedItem()));
        delete.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        VBox.setVgrow(table, Priority.ALWAYS);
        VBox content = new VBox(10, toolbar, new Label("Reseptdə 1 ədəd məhsul üçün lazım olan xammal miqdarını yazın."), table);
        content.getStyleClass().add("content");
        setPageContent(content);
    }

    private void deleteProduct(Product product) {
        if (product == null) return;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Məhsulu sil");
        confirmation.setHeaderText("“" + product.name() + "” silinsin?");
        confirmation.setContentText("Bu məhsul və onun reseptləri silinəcək.");
        confirmation.showAndWait().ifPresent(answer -> {
            if (answer == ButtonType.OK) {
                try {
                    Database.deleteProduct(product.code());
                    products.remove(product);
                    addLog("Məhsul silindi", "Məhsul: " + product.name() + " (" + product.code() + ")");
                    showProducts();
                } catch (Exception exception) {
                    showMessage(Alert.AlertType.ERROR, "Məhsul silinmədi", "Baza xətası: " + exception.getMessage());
                }
            }
        });
    }

    private void openProductDialog(TableView<Product> table) {
        if (rawMaterials.isEmpty()) {
            showMessage(Alert.AlertType.INFORMATION, "Əvvəl xammal əlavə edin", "Resept yaratmaq üçün əvvəlcə “Xammallar” bölməsindən xammal əlavə edilməlidir.");
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Məhsul və resept əlavə et");
        dialog.setHeaderText("1 ədəd məhsulun hazırlanma reseptini yaradın");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        TextField code = new TextField();
        TextField name = new TextField();
        TextField salePrice = new TextField("0.00");
        ComboBox<RawMaterial> material = new ComboBox<>(rawMaterials);
        material.setPromptText("Xammalı seçin");
        material.setMaxWidth(Double.MAX_VALUE);
        TextField needed = new TextField();
        needed.setPromptText("Məsələn: 2.5");
        Button addIngredient = new Button("Reseptə əlavə et");
        addIngredient.getStyleClass().add("secondary-button");
        ObservableList<FormulaItem> recipe = FXCollections.observableArrayList();
        ListView<FormulaItem> recipeList = new ListView<>(recipe);
        recipeList.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(FormulaItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    RawMaterial selectedMaterial = findMaterial(item.materialCode());
                    setText((selectedMaterial == null ? item.materialCode() : selectedMaterial.name()) + " — "
                            + formatQuantity(item.amountPerProduct()) + " "
                            + (selectedMaterial == null ? "" : selectedMaterial.unit()));
                }
            }
        });
        recipeList.setPrefHeight(120);
        Label error = new Label();
        error.getStyleClass().add("error-label");
        addIngredient.setOnAction(event -> {
            try {
                if (material.getValue() == null) throw new IllegalArgumentException();
                BigDecimal amount = strictlyPositive(needed.getText());
                if (recipe.stream().anyMatch(item -> item.materialCode().equals(material.getValue().code()))) {
                    error.setText("Bu xammal artıq reseptdə var."); return;
                }
                recipe.add(new FormulaItem(material.getValue().code(), amount));
                material.setValue(null); needed.clear(); error.setText("");
            } catch (IllegalArgumentException exception) {
                error.setText("Xammalı seçin və tələb olunan miqdarı düzgün yazın.");
            }
        });
        VBox form = new VBox(8, new Label("Məhsul kodu *"), code, new Label("Məhsul adı *"), name,
                new Label("Standart təhvil qiyməti (₼)"), salePrice,
                new Label("Resept üçün xammal"), material, new Label("1 məhsul üçün miqdar"), needed,
                addIngredient, new Label("Resept maddələri"), recipeList, error);
        form.setPrefWidth(420);
        dialog.getDialogPane().setContent(form);
        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (code.getText().isBlank() || name.getText().isBlank() || recipe.isEmpty()) {
                error.setText("Kod, məhsul adı və ən azı bir resept maddəsi tələb olunur.");
                event.consume(); return;
            }
            if (products.stream().anyMatch(product -> product.code().equalsIgnoreCase(code.getText().trim()))) {
                error.setText("Bu məhsul kodu artıq istifadə olunur.");
                event.consume(); return;
            }
            try {
                BigDecimal estimatedUnitCost = recipe.stream()
                        .map(item -> {
                            RawMaterial rm = findMaterial(item.materialCode());
                            return (rm == null ? BigDecimal.ZERO : rm.unitCost()).multiply(item.amountPerProduct());
                        })
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                Product newProduct = new Product(code.getText().trim(), name.getText().trim(), List.copyOf(recipe), BigDecimal.ZERO,
                        positiveOrZero(salePrice.getText()), estimatedUnitCost);
                products.add(newProduct);
                persistInventoryQuietly();
                table.getSelectionModel().select(newProduct);
            } catch (IllegalArgumentException exception) {
                error.setText("Təhvil qiymətini 0 və ya müsbət rəqəm kimi yazın.");
                event.consume();
            }
        });
        dialog.showAndWait();
    }

    private void showProduction() {
        showProduction(null);
    }

    private void showProduction(String preselectedCode) {
        pageTitle.setText("İstehsal");
        ComboBox<Product> product = new ComboBox<>(products);
        product.setPromptText("İstehsal ediləcək məhsulu seçin");
        product.setPrefWidth(400);
        TextField amount = new TextField();
        amount.setPromptText("İstehsal miqdarı (məsələn: 10)");
        amount.setPrefWidth(250);
        Label formula = new Label("Məhsul seçdikdən sonra lazım olan xammallar burada görünəcək.");
        formula.setWrapText(true);
        formula.setMaxWidth(600);
        product.setOnAction(event -> formula.setText(product.getValue() == null ? "" : "1 ədəd üçün resept: " + formulaText(product.getValue())));

        if (preselectedCode != null) {
            Product found = findProduct(preselectedCode);
            if (found != null) {
                product.setValue(found);
                formula.setText("1 ədəd üçün resept: " + formulaText(found));
            }
        }

        Button produce = new Button("İstehsal et və xammalı anbardan çıx");
        produce.getStyleClass().add("primary-button");
        produce.setOnAction(event -> produce(product.getValue(), amount.getText()));
        VBox content = new VBox(16, new Label("Məhsul"), product, new Label("İstehsal miqdarı"), amount, formula, produce);
        content.getStyleClass().add("content");
        setPageContent(content);
    }

    private void showWarehouse() {
        pageTitle.setText("Anbar");
        Label rawTitle = new Label("Xammal anbarı");
        rawTitle.getStyleClass().add("section-title");
        TableView<RawMaterial> rawTable = new TableView<>(rawMaterials);
        rawTable.getColumns().addAll(column("Kod", RawMaterial::code), column("Xammal", RawMaterial::name),
                column("Vahid", RawMaterial::unit), column("Qalan miqdar", material -> formatQuantity(material.stock())),
                column("Maya dəyəri", material -> formatMoney(material.unitCost())));
        rawTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        rawTable.setMinHeight(260);
        rawTable.setPrefHeight(320);
        VBox.setVgrow(rawTable, Priority.ALWAYS);

        Label productTitle = new Label("Hazır məhsul anbarı");
        productTitle.getStyleClass().add("section-title");
        TableView<Product> productTable = new TableView<>(products);
        productTable.getColumns().addAll(column("Kod", Product::code), column("Məhsul", Product::name),
                column("Əldə olan say", product -> formatQuantity(product.stock()) + " ədəd"),
                column("Standart satış qiyməti", product -> formatMoney(product.salePrice())),
                column("İstehsal maya dəyəri", product -> formatMoney(product.unitCost())));
        productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        productTable.setMinHeight(260);
        productTable.setPrefHeight(320);
        VBox.setVgrow(productTable, Priority.ALWAYS);

        VBox content = new VBox(14, rawTitle, rawTable, productTitle, productTable);
        content.getStyleClass().add("content");
        setPageContent(content);
    }

    private void showWastes() {
        pageTitle.setText("İtkilər və sayım fərqləri");
        BigDecimal totalWaste = wastes.stream().map(WasteRecord::cost).reduce(BigDecimal.ZERO, BigDecimal::add);
        Label summary = new Label("Qeydə alınmış ümumi itki dəyəri: " + formatMoney(totalWaste));
        summary.getStyleClass().add("section-title");
        Label note = new Label("Zay məhsul, qırılma, xarab olma və sayım fərqi üçün xammalı burada qeyd edin. Miqdar anbardan çıxılacaq.");
        note.setWrapText(true);
        Button addWaste = new Button("+ İtki qeyd et");
        addWaste.getStyleClass().add("danger-button");
        addWaste.setOnAction(event -> openWasteDialog());
        VBox wasteCards = new VBox(10);
        if (wastes.isEmpty()) wasteCards.getChildren().add(new Label("Hələ itki qeydə alınmayıb."));
        else wastes.reversed().forEach(waste -> wasteCards.getChildren().add(createWasteCard(waste)));
        VBox content = new VBox(16, summary, note, addWaste, wasteCards);
        content.getStyleClass().add("content");
        setPageContent(content);
    }

    private void openWasteDialog() {
        if (rawMaterials.isEmpty()) {
            showMessage(Alert.AlertType.INFORMATION, "Xammal yoxdur", "İtki qeyd etmək üçün əvvəlcə xammal əlavə edin.");
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("İtki qeyd et");
        dialog.setHeaderText("İtki miqdarı anbardan çıxılacaq");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        ComboBox<RawMaterial> material = new ComboBox<>(rawMaterials);
        material.setPromptText("Xammalı seçin"); material.setMaxWidth(Double.MAX_VALUE);
        TextField quantity = new TextField(); quantity.setPromptText("Məsələn: 3.5");
        TextField reason = new TextField(); reason.setPromptText("Məsələn: zay, qırılma, sayım fərqi");
        Label error = new Label(); error.getStyleClass().add("error-label");
        VBox form = new VBox(8, new Label("Xammal *"), material, new Label("İtki miqdarı *"), quantity,
                new Label("Səbəb *"), reason, error);
        form.setPrefWidth(420);
        dialog.getDialogPane().setContent(form);
        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                if (material.getValue() == null || reason.getText().isBlank()) throw new IllegalArgumentException("Xammalı və səbəbi daxil edin.");
                BigDecimal lost = strictlyPositive(quantity.getText());
                RawMaterial selected = material.getValue();
                if (selected.stock().compareTo(lost) < 0) throw new IllegalArgumentException("Anbarda kifayət qədər xammal yoxdur.");
                rawMaterials.set(rawMaterials.indexOf(selected), new RawMaterial(selected.code(), selected.name(), selected.unit(),
                        selected.stock().subtract(lost), selected.unitCost()));
                BigDecimal cost = lost.multiply(selected.unitCost());
                long wasteId = 0;
                persistInventoryQuietly();
                try {
                    wasteId = Database.recordWaste(selected.code(), lost, cost, reason.getText().trim(), LocalDateTime.now());
                } catch (Exception databaseException) {
                    System.err.println("İtki tarixi bazaya yazılmadı: " + databaseException.getMessage());
                }
                wastes.add(new WasteRecord(wasteId, selected.code(), selected.name(), selected.unit(), lost, cost, reason.getText().trim(), now()));
                addLog("İtki qeydi", selected.name() + " — " + formatQuantity(lost) + " " + selected.unit() + "; dəyəri: " + formatMoney(cost) + "; səbəb: " + reason.getText().trim());
            } catch (IllegalArgumentException exception) {
                error.setText(exception.getMessage() == null ? "Məlumatları düzgün daxil edin." : exception.getMessage());
                event.consume();
            }
        });
        dialog.showAndWait();
        showWastes();
    }

    private VBox createWasteCard(WasteRecord waste) {
        Button delete = new Button("Sil");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(event -> deleteWaste(waste));
        Label title = new Label(waste.materialName()); title.getStyleClass().add("transaction-title");
        Label detail = new Label(waste.reason()); detail.getStyleClass().add("transaction-detail");
        Label amount = new Label(formatQuantity(waste.quantity()) + " " + waste.unit() + " • " + formatMoney(waste.cost()));
        amount.getStyleClass().add("transaction-amount");
        Label time = new Label(waste.createdAt()); time.getStyleClass().add("transaction-detail");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(10, new VBox(3, title, detail), spacer, new VBox(3, amount, time), delete);
        VBox card = new VBox(top); card.getStyleClass().add("transaction-card");
        return card;
    }

    private void deleteWaste(WasteRecord waste) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, "Bu itki silinsin və xammal anbara geri qaytarılsın?", ButtonType.CANCEL, ButtonType.OK);
        confirmation.setTitle("İtkini sil");
        confirmation.setHeaderText("İtki qeydi silinir");
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try {
            if (waste.id() > 0) Database.deleteWaste(waste.id());
            RawMaterial material = findMaterial(waste.rawMaterialCode());
            if (material != null) {
                int index = rawMaterials.indexOf(material);
                rawMaterials.set(index, new RawMaterial(material.code(), material.name(), material.unit(), material.stock().add(waste.quantity()), material.unitCost()));
                persistInventoryQuietly();
            }
            wastes.remove(waste);

            // Clean up original "İtki qeydi" log so it doesn't leave orphan traces in activity logs
            activityLogs.removeIf(log -> log.type().equals("İtki qeydi")
                    && log.description().contains(waste.materialName())
                    && log.description().contains(formatQuantity(waste.quantity())));
            Database.deleteActivityLogByDescription("İtki qeydi", "%" + waste.materialName() + "%");

            addLog("İtki silindi", waste.materialName() + " — " + formatQuantity(waste.quantity()) + " " + waste.unit() + " anbara geri qaytarıldı.");
            showWastes();
        } catch (Exception exception) {
            showMessage(Alert.AlertType.ERROR, "İtkini silmək olmadı", exception.getMessage());
        }
    }

    private void showActivityLog() {
        showActivityLog("Hamısı", "Bütün aylar");
    }

    private void showActivityLog(String filter, String month) {
        pageTitle.setText("Əməliyyat jurnalı");
        Label note = new Label("İstehsal, mal təhvili, xammal alışı, ödəniş və itki əməliyyatları burada tarixçə kimi saxlanılır.");
        note.setWrapText(true);
        HBox filters = new HBox(8);
        String[] filterNames = {"Hamısı", "Mal təhvili", "Ödənişlər", "İtkilər", "İstehsal", "Alışlar"};
        for (String filterName : filterNames) {
            Button filterButton = new Button(filterName);
            filterButton.getStyleClass().add(filterName.equals(filter) ? "primary-button" : "secondary-button");
            filterButton.setOnAction(event -> showActivityLog(filterName, month));
            filters.getChildren().add(filterButton);
        }
        List<String> availableMonths = activityLogs.stream().map(this::monthKey).distinct().sorted(Comparator.reverseOrder()).toList();
        ComboBox<String> monthSelector = new ComboBox<>();
        monthSelector.getItems().add("Bütün aylar");
        monthSelector.getItems().addAll(availableMonths);
        monthSelector.setValue(month);
        monthSelector.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(String item) { return item == null || item.equals("Bütün aylar") ? "Bütün aylar" : monthLabel(item); }
            @Override public String fromString(String text) { return text; }
        });
        monthSelector.setOnAction(event -> showActivityLog(filter, monthSelector.getValue()));
        HBox monthFilter = new HBox(10, new Label("Ay üzrə:"), monthSelector);
        monthFilter.setAlignment(Pos.CENTER_LEFT);
        VBox logCards = new VBox(10);
        List<ActivityLog> visibleLogs = activityLogs.stream()
                .filter(log -> matchesLogFilter(log, filter))
                .filter(log -> month.equals("Bütün aylar") || monthKey(log).equals(month)).toList();
        if (visibleLogs.isEmpty()) logCards.getChildren().add(new Label("Bu bölmə üçün əməliyyat yoxdur."));
        else visibleLogs.reversed().forEach(log -> logCards.getChildren().add(createLogCard(log, filter, month)));
        VBox content = new VBox(16, note, filters, monthFilter, logCards);
        content.getStyleClass().add("content");
        setPageContent(content);
    }

    private boolean matchesLogFilter(ActivityLog log, String filter) {
        return switch (filter) {
            case "Mal təhvili" -> log.type().equals("Mal təhvili") || log.type().equals("Mal təhvili silindi");
            case "Ödənişlər" -> log.type().toLowerCase().contains("ödəniş");
            case "İtkilər" -> log.type().equals("İtki qeydi") || log.type().equals("İtki silindi");
            case "İstehsal" -> log.type().equals("İstehsal");
            case "Alışlar" -> log.type().equals("Xammal alışı");
            default -> true;
        };
    }

    private VBox createLogCard(ActivityLog log, String filter, String month) {
        Label type = new Label(log.type()); type.getStyleClass().add("transaction-title");
        Label description = new Label(log.description()); description.getStyleClass().add("transaction-detail");
        Label time = new Label(log.createdAt()); time.getStyleClass().add("transaction-detail");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button delete = new Button("Sil");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(event -> deleteActivityLog(log, filter, month));
        HBox row = new HBox(10, new VBox(3, type, description), spacer, time, delete);
        VBox card = new VBox(row); card.getStyleClass().add("transaction-card");
        return card;
    }

    private void deleteActivityLog(ActivityLog log, String filter, String month) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Jurnal qeydini sil");
        confirmation.setHeaderText("Bu jurnal qeydi silinsin?");
        confirmation.setContentText("Əməliyyatın özü geri qaytarılmayacaq; yalnız jurnal qeydi silinəcək.");
        confirmation.showAndWait().ifPresent(answer -> {
            if (answer != ButtonType.OK) return;
            try {
                if (log.id() > 0 && Database.isConfigured()) Database.deleteActivityLog(log.id());
                activityLogs.remove(log);
                showActivityLog(filter, month);
            } catch (Exception exception) {
                showMessage(Alert.AlertType.ERROR, "Jurnal silinmədi", "Baza xətası: " + exception.getMessage());
            }
        });
    }

    private void showInvoices() {
        pageTitle.setText("Qaimə-Fakturalar Və İnvoyslar");

        Button add = new Button("+ Yeni Qaimə / Fatura Kəs");
        add.getStyleClass().add("primary-button");
        Button pdfBtn = new Button("📄 PDF İxrac Et / Yüklə");
        pdfBtn.getStyleClass().add("secondary-button");
        Button delete = new Button("Sil");
        delete.getStyleClass().add("danger-button");

        HBox toolbar = new HBox(12, add, pdfBtn, delete);

        List<InvoiceEntry> invoiceList = new ArrayList<>();
        try {
            if (Database.isConfigured()) {
                invoiceList.addAll(Database.loadInvoices());
            }
        } catch (Exception e) {
            System.err.println("Faturalar yüklənmədi: " + e.getMessage());
        }

        ObservableList<InvoiceEntry> observableInvoices = FXCollections.observableArrayList(invoiceList);
        TableView<InvoiceEntry> table = new TableView<>(observableInvoices);
        table.setPlaceholder(new Label("Hələ heç bir Fatura / Qaimə kəsilməyib."));

        table.getColumns().addAll(
                column("Fatura №", InvoiceEntry::invoiceNo),
                column("Tarix", inv -> inv.issueDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))),
                column("Müştəri (Alıcı)", InvoiceEntry::customerName),
                column("Satıcı (VÖEN)", inv -> inv.sellerName() + " (" + inv.sellerVoen() + ")"),
                column("Ödəniş Növü", InvoiceEntry::paymentType),
                column("ƏDV-siz Cəmi", inv -> formatMoney(inv.subtotal())),
                column("ƏDV (% / Məbləğ)", inv -> inv.vatRate() + "% (" + formatMoney(inv.vatAmount()) + ")"),
                column("YEKUN MƏBLƏĞ (AZN)", inv -> formatMoney(inv.totalAmount())),
                column("Qeydlər", InvoiceEntry::notes)
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setMinHeight(420);
        VBox.setVgrow(table, Priority.ALWAYS);

        add.setOnAction(event -> openInvoiceDialog(null, table));
        pdfBtn.setOnAction(event -> exportInvoiceToPdf(table.getSelectionModel().getSelectedItem()));
        delete.setOnAction(event -> deleteInvoiceRecord(table.getSelectionModel().getSelectedItem(), table));

        pdfBtn.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        delete.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());

        VBox content = new VBox(18,
                new Label("Müştəriləriniz üçün rəsmi Azərbaycan VÖEN-li, ƏDV (0% / 18%) hesablanmış Qaimə-Fakturaları burada yarada, baxa və rəsmi PDF faylı kimi kompüterinizə yükleyə bilərsiniz."),
                toolbar, table);
        content.getStyleClass().add("content");
        setPageContent(content);
    }

    private void exportInvoiceToPdf(InvoiceEntry invoice) {
        if (invoice == null) return;

        try {
            java.io.File desktopDir = new java.io.File(System.getProperty("user.home"), "Desktop");
            if (!desktopDir.exists()) {
                desktopDir.mkdirs();
            }
            String safeFileName = "Fatura_" + invoice.invoiceNo().replaceAll("[^a-zA-Z0-9._-]", "_") + ".pdf";
            java.io.File targetFile = new java.io.File(desktopDir, safeFileName);

            InvoicePdfGenerator.generatePdf(invoice, targetFile);

            // Auto-open PDF or reveal in Windows Explorer
            try {
                if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) {
                    java.awt.Desktop.getDesktop().open(targetFile);
                } else {
                    new ProcessBuilder("explorer.exe", "/select,", targetFile.getAbsolutePath()).start();
                }
            } catch (Exception ex) {
                try {
                    new ProcessBuilder("explorer.exe", "/select,", targetFile.getAbsolutePath()).start();
                } catch (Exception ignore) {}
            }

            showMessage(Alert.AlertType.INFORMATION, "Fatura PDF Yaradıldı",
                    "Fatura PDF faylı birbaşa Masaüstünə (Desktop) uğurla saxlanıldı və açıldı:\n" + targetFile.getAbsolutePath());
        } catch (Exception e) {
            showMessage(Alert.AlertType.ERROR, "PDF Xətası", "PDF faylı yaradılarkən xəta baş verdi: " + e.getMessage());
        }
    }

    private void deleteInvoiceRecord(InvoiceEntry invoice, TableView<InvoiceEntry> table) {
        if (invoice == null) return;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, "“" + invoice.invoiceNo() + "” nömrəli fatura silinsin?", ButtonType.CANCEL, ButtonType.OK);
        confirmation.setTitle("Faturanı sil");
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try {
            if (invoice.id() > 0 && Database.isConfigured()) {
                Database.deleteInvoice(invoice.id());
            }
            table.getItems().remove(invoice);
            addLog("Fatura silindi", "Fatura: " + invoice.invoiceNo() + " — " + invoice.customerName());
        } catch (Exception e) {
            showMessage(Alert.AlertType.ERROR, "Fatura silinmədi", "Baza xətası: " + e.getMessage());
        }
    }

    private void openInvoiceDialog(InvoiceEntry existing, TableView<InvoiceEntry> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Yeni Qaimə / Fatura Kəs (Azərbaycan)");
        dialog.setHeaderText("Rəsmi Azərbaycan Şirkət və VÖEN rekvizitləri ilə Qaimə-Faktura yaradın");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        String autoNo = "FAT-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy")) + "-" + String.format("%04d", (int)(Math.random() * 9000 + 1000));
        TextField invoiceNo = new TextField(autoNo);
        TextField deliveryDocNo = new TextField();
        deliveryDocNo.setPromptText("Məsələn: D-2026-001 (İstəyə bağlı)");

        TextField sellerName = new TextField();
        sellerName.setPromptText("Şirkətin hüquqi adı");
        TextField sellerVoen = new TextField();
        sellerVoen.setPromptText("Şirkətin VÖEN-i");
        TextField sellerAddress = new TextField();
        sellerAddress.setPromptText("Şirkətin ünvanı");
        TextField sellerBankDetails = new TextField();
        sellerBankDetails.setPromptText("Bank adı və IBAN");

        ComboBox<Customer> customerCombo = new ComboBox<>(customers);
        customerCombo.setPromptText("Müştərini seçin");
        customerCombo.setMaxWidth(Double.MAX_VALUE);

        TextField customerVoen = new TextField();
        customerVoen.setPromptText("Alıcının VÖEN-i (əgər hüquqi şəxsdir)");

        customerCombo.setOnAction(ev -> {
            Customer c = customerCombo.getValue();
            if (c != null) {
                customerVoen.setPromptText(c.name() + " VÖEN");
            }
        });

        ComboBox<String> paymentTypeCombo = new ComboBox<>(FXCollections.observableArrayList("Köçürmə", "Nağd", "Nisyə / Təxirə salınmış"));
        paymentTypeCombo.setValue("Köçürmə");
        paymentTypeCombo.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> vatCombo = new ComboBox<>(FXCollections.observableArrayList("18% (Standart ƏDV)", "0% (ƏDV-dən azad)"));
        vatCombo.setValue("18% (Standart ƏDV)");
        vatCombo.setMaxWidth(Double.MAX_VALUE);

        // Invoice Items Form
        ObservableList<InvoiceEntry.InvoiceItem> itemsList = FXCollections.observableArrayList();

        ComboBox<Product> productCombo = new ComboBox<>(products);
        productCombo.setPromptText("Məhsulu seçin");
        productCombo.setMaxWidth(Double.MAX_VALUE);

        TextField customItemName = new TextField();
        customItemName.setPromptText("Və ya xüsusi məhsul/xidmət adı");

        TextField itemUnit = new TextField("ədəd");
        itemUnit.setPrefWidth(80);
        TextField itemQty = new TextField("1");
        itemQty.setPrefWidth(80);
        TextField itemPrice = new TextField("0.00");
        itemPrice.setPrefWidth(100);

        productCombo.setOnAction(ev -> {
            Product p = productCombo.getValue();
            if (p != null) {
                customItemName.setText(p.name());
                itemPrice.setText(p.salePrice().toPlainString());
            }
        });

        Button addItemBtn = new Button("+ Maddə Əlavə Et");
        addItemBtn.getStyleClass().add("secondary-button");

        TableView<InvoiceEntry.InvoiceItem> itemsTable = new TableView<>(itemsList);
        itemsTable.setPrefHeight(150);
        itemsTable.getColumns().addAll(
                column("Məhsul / Xidmət", InvoiceEntry.InvoiceItem::itemName),
                column("Vahid", InvoiceEntry.InvoiceItem::unit),
                column("Miqdar", item -> item.quantity().toString()),
                column("Qiymət", item -> formatMoney(item.unitPrice())),
                column("Məbləğ", item -> formatMoney(item.lineTotal()))
        );
        itemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Label subtotalLabel = new Label("Cəmi (ƏDV-siz): 0.00 ₼");
        Label vatLabel = new Label("ƏDV (18%): 0.00 ₼");
        Label totalLabel = new Label("YEKUN ÖDƏNİLƏCƏK: 0.00 ₼");
        totalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1a56db;");

        Runnable recalculateTotals = () -> {
            BigDecimal sub = itemsList.stream().map(InvoiceEntry.InvoiceItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal rate = vatCombo.getValue().contains("18%") ? new BigDecimal("18.00") : BigDecimal.ZERO;
            BigDecimal vat = sub.multiply(rate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            BigDecimal tot = sub.add(vat);

            subtotalLabel.setText("Cəmi (ƏDV-siz): " + formatMoney(sub));
            vatLabel.setText("ƏDV (" + rate + "%): " + formatMoney(vat));
            totalLabel.setText("YEKUN ÖDƏNİLƏCƏK: " + formatMoney(tot));
        };

        addItemBtn.setOnAction(ev -> {
            String name = customItemName.getText().trim();
            if (name.isEmpty() && productCombo.getValue() != null) {
                name = productCombo.getValue().name();
            }
            if (name.isEmpty()) return;
            try {
                BigDecimal q = positiveOrZero(itemQty.getText());
                BigDecimal p = positiveOrZero(itemPrice.getText());
                BigDecimal lineTot = q.multiply(p);
                itemsList.add(new InvoiceEntry.InvoiceItem(0, name, itemUnit.getText().trim(), q, p, lineTot));
                customItemName.clear();
                productCombo.setValue(null);
                itemQty.setText("1");
                itemPrice.setText("0.00");
                recalculateTotals.run();
            } catch (Exception ignore) {}
        });

        vatCombo.setOnAction(ev -> recalculateTotals.run());

        TextField notesField = new TextField();
        notesField.setPromptText("Əlavə qeydlər və ya müqavilə nömrəsi...");

        Label error = new Label();
        error.getStyleClass().add("error-label");

        VBox form = new VBox(10,
                new HBox(10, new VBox(4, new Label("Fatura № *"), invoiceNo), new VBox(4, new Label("Təhvil №"), deliveryDocNo)),
                new HBox(10, new VBox(4, new Label("Satıcı Şirkət *"), sellerName), new VBox(4, new Label("Satıcı VÖEN *"), sellerVoen)),
                new HBox(10, new VBox(4, new Label("Satıcı Ünvanı"), sellerAddress), new VBox(4, new Label("Bank / İBAN"), sellerBankDetails)),
                new HBox(10, new VBox(4, new Label("Alıcı Müştəri *"), customerCombo), new VBox(4, new Label("Alıcı VÖEN"), customerVoen)),
                new HBox(10, new VBox(4, new Label("Ödəniş Növü"), paymentTypeCombo), new VBox(4, new Label("ƏDV Dərəcəsi"), vatCombo)),
                new Label("Məhsul / Xidmət Maddələri *"),
                new HBox(8, productCombo, customItemName, itemUnit, itemQty, itemPrice, addItemBtn),
                itemsTable,
                new VBox(4, subtotalLabel, vatLabel, totalLabel),
                new Label("Qeydlər"), notesField,
                error
        );
        form.setPrefWidth(580);
        dialog.getDialogPane().setContent(form);

        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                if (invoiceNo.getText().isBlank() || sellerName.getText().isBlank() || sellerVoen.getText().isBlank()) {
                    throw new IllegalArgumentException("Fatura №, Satıcı adı və Satıcı VÖEN-i daxil edilməlidir.");
                }
                String custName = customerCombo.getValue() != null ? customerCombo.getValue().name() : "";
                if (custName.isEmpty()) throw new IllegalArgumentException("Alıcı müştəri seçilməlidir.");
                if (itemsList.isEmpty()) throw new IllegalArgumentException("Ən azı 1 məhsul və ya xidmət maddəsi əlavə edilməlidir.");

                BigDecimal sub = itemsList.stream().map(InvoiceEntry.InvoiceItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal rate = vatCombo.getValue().contains("18%") ? new BigDecimal("18.00") : BigDecimal.ZERO;
                BigDecimal vat = sub.multiply(rate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                BigDecimal tot = sub.add(vat);

                InvoiceEntry entry = new InvoiceEntry(
                        0,
                        invoiceNo.getText().trim(),
                        deliveryDocNo.getText().trim(),
                        LocalDateTime.now(),
                        sellerName.getText().trim(),
                        sellerVoen.getText().trim(),
                        sellerAddress.getText().trim(),
                        sellerBankDetails.getText().trim(),
                        custName,
                        customerVoen.getText().trim(),
                        paymentTypeCombo.getValue(),
                        rate,
                        sub,
                        vat,
                        tot,
                        notesField.getText().trim(),
                        new ArrayList<>(itemsList)
                );

                long savedId = 0;
                if (Database.isConfigured()) {
                    savedId = Database.recordInvoice(entry);
                }
                InvoiceEntry savedEntry = new InvoiceEntry(
                        savedId, entry.invoiceNo(), entry.deliveryDocNo(), entry.issueDate(),
                        entry.sellerName(), entry.sellerVoen(), entry.sellerAddress(), entry.sellerBankDetails(),
                        entry.customerName(), entry.customerVoen(), entry.paymentType(), entry.vatRate(),
                        entry.subtotal(), entry.vatAmount(), entry.totalAmount(), entry.notes(), entry.items()
                );

                addLog("Qaimə/Fatura kəsildi", savedEntry.invoiceNo() + " — " + savedEntry.customerName() + ": " + formatMoney(savedEntry.totalAmount()));
                showMessage(Alert.AlertType.INFORMATION, "Fatura Yaradıldı", "“" + savedEntry.invoiceNo() + "” nömrəli fatura uğurla yaradıldı.");

                // Auto prompt to export PDF
                exportInvoiceToPdf(savedEntry);
            } catch (IllegalArgumentException exception) {
                error.setText(exception.getMessage() == null ? "Məlumatları düzgün doldurun." : exception.getMessage());
                event.consume();
            } catch (Exception ex) {
                error.setText("Baza xətası: " + ex.getMessage());
                event.consume();
            }
        });

        dialog.showAndWait();
        showInvoices();
    }

    private String monthKey(ActivityLog log) {
        return monthKey(log.createdAt());
    }

    private String monthKey(CashMovement movement) {
        return monthKey(movement.createdAt());
    }

    private String monthKey(Purchase purchase) {
        return monthKey(purchase.createdAt());
    }

    private String monthKey(SalaryRecord record) {
        return monthKey(record.createdAt());
    }

    private String monthKey(CreditPaymentRecord record) {
        return monthKey(record.createdAt());
    }

    private String monthKey(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) return "";
        timestamp = timestamp.trim();
        if (timestamp.length() >= 10 && timestamp.charAt(2) == '.' && timestamp.charAt(5) == '.') {
            return timestamp.substring(6, 10) + "-" + timestamp.substring(3, 5);
        }
        if (timestamp.length() >= 7 && timestamp.charAt(4) == '-') {
            return timestamp.substring(0, 7);
        }
        return timestamp;
    }

    private String monthLabel(String key) {
        if (key == null || key.isBlank()) return "";
        key = key.trim();
        String[] months = {"Yanvar", "Fevral", "Mart", "Aprel", "May", "İyun", "İyul", "Avqust", "Sentyabr", "Oktyabr", "Noyabr", "Dekabr"};
        if (key.length() == 7 && key.charAt(4) == '-') {
            try {
                int year = Integer.parseInt(key.substring(0, 4));
                int month = Integer.parseInt(key.substring(5, 7));
                if (month >= 1 && month <= 12) {
                    return months[month - 1] + " " + year;
                }
            } catch (Exception ignored) { }
        }
        if (key.length() >= 7 && key.contains(".")) {
            try {
                int month = Integer.parseInt(key.substring(0, 2));
                String year = key.substring(3);
                if (month >= 1 && month <= 12) {
                    return months[month - 1] + " " + year;
                }
            } catch (Exception ignored) { }
        }
        return key;
    }

    private void addLog(String type, String description) {
        LocalDateTime occurredAt = LocalDateTime.now();
        long id = -1;
        if (Database.isConfigured()) {
            try {
                id = Database.addActivityLog(type, description, occurredAt);
            } catch (Exception exception) {
                System.err.println("Jurnal bazaya yazılmadı: " + exception.getMessage());
            }
        }
        activityLogs.add(new ActivityLog(id, type, description, formatTimestamp(occurredAt)));
    }

    private void addCashMovement(CashDirection direction, BigDecimal amount, String description) {
        if (amount.signum() <= 0) return;
        LocalDateTime occurredAt = LocalDateTime.now();
        long id = 0;
        if (Database.isConfigured()) {
            try {
                id = Database.addCashMovement(direction.name(), amount, description, occurredAt);
            } catch (Exception exception) {
                System.err.println("Pul hərəkəti bazaya yazılmadı: " + exception.getMessage());
            }
        }
        cashMovements.add(new CashMovement(id, direction, amount, description, formatTimestamp(occurredAt)));
    }

    private void showSales() {
        pageTitle.setText("Mal təhvili");
        Label intro = new Label("Müştəriyə verilən məhsulu qeyd edin. Mal təhvilindən sonra məhsul anbardan çıxır, qalan məbləğ müştəri borcuna yazılır.");
        intro.setWrapText(true);
        Button newSale = new Button("+ Yeni mal təhvili");
        newSale.getStyleClass().add("primary-button");
        newSale.setOnAction(event -> openSaleDialog());

        TextField search = new TextField();
        search.setPromptText("Sənəd №, müştəri və ya məhsul adı ilə axtarın...");
        search.getStyleClass().add("search-field");
        HBox toolbar = new HBox(12, search, newSale);
        HBox.setHgrow(search, Priority.ALWAYS);

        VBox saleCards = new VBox(10);
        renderSaleCards(saleCards, search.getText());
        search.textProperty().addListener((obs, oldVal, newVal) -> renderSaleCards(saleCards, newVal));

        Label historyTitle = new Label("Qeydə alınmış mal təhvilləri");
        historyTitle.getStyleClass().add("section-title");

        VBox content = new VBox(16, intro, toolbar, historyTitle, saleCards);
        content.getStyleClass().add("content");
        setPageContent(content);
    }

    private void renderSaleCards(VBox container, String filter) {
        container.getChildren().clear();
        String query = filter == null ? "" : filter.trim().toLowerCase();
        List<Sale> filtered = sales.stream()
                .filter(sale -> query.isEmpty()
                        || sale.code().toLowerCase().contains(query)
                        || sale.customerName().toLowerCase().contains(query)
                        || sale.productName().toLowerCase().contains(query)
                        || sale.managerName().toLowerCase().contains(query))
                .toList();

        if (filtered.isEmpty()) {
            container.getChildren().add(new Label("Mal təhvili əməliyyatı tapılmadı."));
        } else {
            filtered.reversed().forEach(sale -> container.getChildren().add(createSaleCard(sale)));
        }
    }

    private VBox createSaleCard(Sale sale) {
        Label title = new Label(sale.code() + "  •  " + sale.customerName());
        title.getStyleClass().add("transaction-title");
        String detailText = sale.productName() + " — " + formatQuantity(sale.quantity()) + " ədəd"
                + (sale.managerName().isBlank() || sale.managerName().equals("—") ? "" : " (Menecer: " + sale.managerName() + ")");
        Label detail = new Label(detailText);
        detail.getStyleClass().add("transaction-detail");

        Label amount = new Label("Cəmi: " + formatMoney(sale.total()));
        amount.getStyleClass().add("transaction-amount");
        Label debtInfo = new Label("Qalıq borc: " + formatMoney(sale.remainingDebt())
                + (sale.paidAmount().signum() > 0 ? " | Ödənilən: " + formatMoney(sale.paidAmount()) : ""));
        debtInfo.getStyleClass().add("transaction-detail");

        Label time = new Label(sale.createdAt());
        time.getStyleClass().add("transaction-detail");

        Button delete = new Button("Sil");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(event -> deleteDelivery(sale));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox top = new HBox(12, new VBox(3, title, detail), spacer, new VBox(3, amount, debtInfo, time), delete);
        top.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(top);
        card.getStyleClass().add("transaction-card");
        return card;
    }

    private void deleteDelivery(Sale sale) {
        if (sale == null) return;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Mal təhvilini sil");
        confirmation.setHeaderText("“" + sale.code() + " — " + sale.customerName() + "” mal təhvili silinsin?");
        confirmation.setContentText(
                formatQuantity(sale.quantity()) + " ədəd “" + sale.productName() + "” anbara geri qaytarılacaq və "
                        + (sale.remainingDebt().signum() > 0 ? "müştərinin borcundan " + formatMoney(sale.remainingDebt()) + " çıxılacaq." : "əməliyyat ləğv ediləcək."));
        confirmation.showAndWait().ifPresent(answer -> {
            if (answer != ButtonType.OK) return;
            try {
                // 1. Return delivered product to stock
                Product product = findProduct(sale.productCode());
                if (product == null) {
                    product = products.stream().filter(p -> p.name().equals(sale.productName())).findFirst().orElse(null);
                }
                if (product != null) {
                    int pIndex = products.indexOf(product);
                    products.set(pIndex, new Product(product.code(), product.name(), product.recipe(),
                            product.stock().add(sale.quantity()), product.salePrice(), product.unitCost()));
                    persistInventoryQuietly();
                }

                // 2. Adjust customer debt (subtract the unpaid part that was added to debt)
                Customer customer = findCustomer(sale.customerCode());
                if (customer == null) {
                    customer = customers.stream().filter(c -> c.name().equals(sale.customerName())).findFirst().orElse(null);
                }
                if (customer != null && sale.remainingDebt().signum() > 0) {
                    int cIndex = customers.indexOf(customer);
                    BigDecimal currentDebt = customerDebt(customer);
                    BigDecimal newDebt = currentDebt.subtract(sale.remainingDebt());
                    if (newDebt.signum() < 0) newDebt = BigDecimal.ZERO;
                    Customer updatedCustomer = new Customer(customer.code(), customer.name(), customer.phone(),
                            formatMoney(newDebt), customer.managerCode());
                    customers.set(cIndex, updatedCustomer);
                    persistCustomer(updatedCustomer);
                }

                // 3. If there was an initial paid amount, reverse cash movement and payment
                if (sale.paidAmount().signum() > 0) {
                    cashMovements.removeIf(cm -> cm.direction() == CashDirection.INCOME
                            && cm.amount().compareTo(sale.paidAmount()) == 0
                            && cm.description().contains(sale.customerName()));
                    Database.deleteCashMovementByDescription("Mal təhvilində ilkin ödəniş: " + sale.customerName(), sale.paidAmount());

                    payments.removeIf(p -> p.amount().compareTo(sale.paidAmount()) == 0
                            && p.personName().equals(sale.customerName())
                            && p.type().contains("Mal təhvil"));
                    Database.deletePaymentByCriteria("Mal təhvilində ödəniş", sale.customerCode(), null, sale.paidAmount());
                }

                // 4. Delete delivery from PostgreSQL database
                Database.deleteDelivery(sale.code());

                // 5. Remove from sales list
                sales.remove(sale);

                // 6. Log activity
                addLog("Mal təhvili silindi", sale.code() + " — " + sale.customerName() + "; "
                        + formatQuantity(sale.quantity()) + " ədəd " + sale.productName() + " anbara qaytarıldı.");

                // 7. Refresh UI
                showSales();
            } catch (Exception exception) {
                showMessage(Alert.AlertType.ERROR, "Mal təhvili silinmədi", "Xəta: " + exception.getMessage());
            }
        });
    }

    private void openSaleDialog() {
        if (customers.isEmpty() || products.isEmpty()) {
            showMessage(Alert.AlertType.INFORMATION, "Mal təhvili üçün məlumat çatmır", "Əvvəl müştəri və məhsul əlavə edin.");
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Yeni mal təhvili");
        dialog.setHeaderText("Müştəriyə verilən məhsulu və ödənişi qeyd edin");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        ComboBox<Customer> customer = new ComboBox<>(customers);
        customer.setPromptText("Müştərini seçin"); customer.setMaxWidth(Double.MAX_VALUE);
        customer.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Customer item) { return item == null ? "" : item.code() + " — " + item.name(); }
            @Override public Customer fromString(String text) { return null; }
        });
        ComboBox<Product> product = new ComboBox<>(products);
        product.setPromptText("Məhsulu seçin"); product.setMaxWidth(Double.MAX_VALUE);
        TextField quantity = new TextField(); quantity.setPromptText("Məsələn: 10");
        TextField price = new TextField(); price.setPromptText("Bir ədədin qiyməti");
        TextField paid = new TextField("0.00");
        Label info = new Label();
        Label error = new Label(); error.getStyleClass().add("error-label");
        product.setOnAction(event -> {
            if (product.getValue() != null) {
                price.setText(product.getValue().salePrice().toPlainString());
                info.setText("Anbarda: " + formatQuantity(product.getValue().stock()) + " ədəd");
            }
        });
        VBox form = new VBox(8, new Label("Müştəri *"), customer, new Label("Məhsul *"), product,
                info, new Label("Miqdar *"), quantity, new Label("Bir ədədin qiyməti (₼) *"), price,
                new Label("İndi ödənilən məbləğ (₼)"), paid, error);
        form.setPrefWidth(420);
        dialog.getDialogPane().setContent(form);
        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                if (customer.getValue() == null || product.getValue() == null) throw new IllegalArgumentException("Müştəri və məhsul seçilməlidir.");
                BigDecimal count = strictlyPositive(quantity.getText());
                BigDecimal unitPrice = positiveOrZero(price.getText());
                BigDecimal paidAmount = positiveOrZero(paid.getText());
                Product selectedProduct = product.getValue();
                BigDecimal total = unitPrice.multiply(count);
                if (selectedProduct.stock().compareTo(count) < 0) throw new IllegalArgumentException("Anbarda kifayət qədər məhsul yoxdur.");
                if (paidAmount.compareTo(total) > 0) throw new IllegalArgumentException("Ödəniş ümumi məbləğdən çox ola bilməz.");
                int productIndex = products.indexOf(selectedProduct);
                products.set(productIndex, new Product(selectedProduct.code(), selectedProduct.name(), selectedProduct.recipe(),
                        selectedProduct.stock().subtract(count), selectedProduct.salePrice(), selectedProduct.unitCost()));
                persistInventoryQuietly();
                Customer selectedCustomer = customer.getValue();
                Manager assignedManager = findManager(selectedCustomer.managerCode());
                String managerName = assignedManager == null ? "—" : assignedManager.name();
                String managerCode = assignedManager == null ? "" : assignedManager.code();

                int customerIndex = customers.indexOf(selectedCustomer);
                BigDecimal existingDebt = customerDebt(selectedCustomer);
                BigDecimal remainingDebt = total.subtract(paidAmount);
                Customer updatedCustomer = new Customer(selectedCustomer.code(), selectedCustomer.name(), selectedCustomer.phone(),
                        formatMoney(existingDebt.add(remainingDebt)), selectedCustomer.managerCode());
                customers.set(customerIndex, updatedCustomer);
                persistCustomer(updatedCustomer);

                String deliveryCode = "T-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
                BigDecimal costTotal = selectedProduct.unitCost().multiply(count);

                Sale newSale = new Sale(
                        deliveryCode,
                        selectedCustomer.code(),
                        selectedCustomer.name(),
                        selectedProduct.code(),
                        selectedProduct.name(),
                        count,
                        total,
                        costTotal,
                        paidAmount,
                        remainingDebt,
                        managerCode,
                        managerName,
                        now());
                sales.add(newSale);

                try {
                    Database.recordDelivery(deliveryCode, selectedCustomer.code(), managerCode, selectedProduct.code(), count,
                            unitPrice, selectedProduct.unitCost(), paidAmount, LocalDateTime.now());
                } catch (Exception databaseException) {
                    System.err.println("Mal təhvili bazaya yazılmadı: " + databaseException.getMessage());
                }

                if (paidAmount.signum() > 0) {
                    addCashMovement(CashDirection.INCOME, paidAmount, "Mal təhvilində ilkin ödəniş: " + selectedCustomer.name());
                    try {
                        long paymentId = Database.recordPayment("Mal təhvilində ödəniş", selectedCustomer.code(), null, managerCode, paidAmount, LocalDateTime.now());
                        payments.add(new Payment(paymentId, "Mal təhvilində ödəniş", selectedCustomer.code(), "", selectedCustomer.name(), paidAmount, managerCode, now()));
                    } catch (Exception dbEx) {
                        System.err.println("Ödəniş bazaya yazılmadı: " + dbEx.getMessage());
                    }
                }

                addLog("Mal təhvili", selectedCustomer.name() + " — " + selectedProduct.name() + ": " + formatQuantity(count)
                        + " ədəd; menecer: " + managerName + "; cəmi " + formatMoney(total));
            } catch (IllegalArgumentException exception) {
                error.setText(exception.getMessage() == null ? "Məlumatları düzgün doldurun." : exception.getMessage());
                event.consume();
            }
        });
        dialog.showAndWait();
        showSales();
    }

    private void showPayments() {
        pageTitle.setText("Ödənişlər");
        Label intro = new Label("Müştəridən daxil olan və təchizatçıya edilən ödənişləri qeyd edin.");
        intro.setWrapText(true);
        Button customerPayment = new Button("Müştəri ödənişi al");
        customerPayment.getStyleClass().add("primary-button");
        Button supplierPayment = new Button("Təchizatçıya ödəniş et");
        supplierPayment.getStyleClass().add("secondary-button");
        customerPayment.setOnAction(event -> openPaymentDialog(true));
        supplierPayment.setOnAction(event -> openPaymentDialog(false));

        TextField search = new TextField();
        search.setPromptText("Şəxs, şirkət və ya ödəniş növü ilə axtarın...");
        search.getStyleClass().add("search-field");

        HBox actions = new HBox(12, search, customerPayment, supplierPayment);
        HBox.setHgrow(search, Priority.ALWAYS);

        Label historyTitle = new Label("Son ödənişlər");
        historyTitle.getStyleClass().add("section-title");

        VBox paymentCards = new VBox(10);
        renderPaymentCards(paymentCards, search.getText());
        search.textProperty().addListener((obs, oldVal, newVal) -> renderPaymentCards(paymentCards, newVal));

        VBox content = new VBox(16, intro, actions, historyTitle, paymentCards);
        content.getStyleClass().add("content");
        setPageContent(content);
    }

    private void renderPaymentCards(VBox container, String filter) {
        container.getChildren().clear();
        String query = filter == null ? "" : filter.trim().toLowerCase();
        List<Payment> filtered = payments.stream()
                .filter(p -> query.isEmpty()
                        || p.personName().toLowerCase().contains(query)
                        || p.type().toLowerCase().contains(query)
                        || p.customerCode().toLowerCase().contains(query)
                        || p.supplierCode().toLowerCase().contains(query))
                .toList();

        if (filtered.isEmpty()) {
            container.getChildren().add(new Label("Hələ ödəniş əməliyyatı yoxdur."));
        } else {
            filtered.reversed().forEach(payment -> container.getChildren().add(createPaymentCard(payment)));
        }
    }

    private VBox createPaymentCard(Payment payment) {
        boolean isIncome = payment.type().contains("Müştəri") || payment.type().contains("təhvil");
        Label title = new Label(payment.type());
        title.getStyleClass().add("transaction-title");
        Label detail = new Label(payment.personName());
        detail.getStyleClass().add("transaction-detail");

        Label amount = new Label((isIncome ? "+ " : "- ") + formatMoney(payment.amount()));
        amount.getStyleClass().add("transaction-amount");
        if (!isIncome) {
            amount.setStyle("-fx-text-fill: #dc2626;");
        }

        Label time = new Label(payment.createdAt());
        time.getStyleClass().add("transaction-detail");

        Button delete = new Button("Sil");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(event -> deletePayment(payment));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox top = new HBox(12, new VBox(3, title, detail), spacer, new VBox(3, amount, time), delete);
        top.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(top);
        card.getStyleClass().add("transaction-card");
        return card;
    }

    private void deletePayment(Payment payment) {
        if (payment == null) return;
        boolean isCustomer = payment.type().contains("Müştəri") || payment.type().contains("təhvil");
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Ödənişi sil");
        confirmation.setHeaderText("“" + payment.type() + " — " + payment.personName() + " (" + formatMoney(payment.amount()) + ")” silinsin?");
        confirmation.setContentText("Ödəniş silindikdə məbləğ (" + formatMoney(payment.amount()) + ") "
                + (isCustomer ? "müştərinin borcuna geri əlavə ediləcək" : "təchizatçı borcuna geri əlavə ediləcək")
                + " və kassadan çıxarılacaq.");
        confirmation.showAndWait().ifPresent(answer -> {
            if (answer != ButtonType.OK) return;
            try {
                if (isCustomer) {
                    Customer customer = findCustomer(payment.customerCode());
                    if (customer == null) {
                        customer = customers.stream().filter(c -> c.name().equals(payment.personName())).findFirst().orElse(null);
                    }
                    if (customer != null) {
                        int cIndex = customers.indexOf(customer);
                        BigDecimal currentDebt = customerDebt(customer);
                        BigDecimal newDebt = currentDebt.add(payment.amount());
                        Customer updatedCustomer = new Customer(customer.code(), customer.name(), customer.phone(),
                                formatMoney(newDebt), customer.managerCode());
                        customers.set(cIndex, updatedCustomer);
                        persistCustomer(updatedCustomer);
                    }

                    // Remove from cash movements
                    cashMovements.removeIf(cm -> cm.direction() == CashDirection.INCOME
                            && cm.amount().compareTo(payment.amount()) == 0
                            && cm.description().contains(payment.personName()));
                    Database.deleteCashMovementByDescription("Müştəri ödənişi: " + payment.personName(), payment.amount());
                } else {
                    Supplier supplier = findSupplier(payment.supplierCode());
                    if (supplier == null) {
                        supplier = suppliers.stream().filter(s -> s.name().equals(payment.personName())).findFirst().orElse(null);
                    }
                    if (supplier != null) {
                        int sIndex = suppliers.indexOf(supplier);
                        BigDecimal newDebt = supplier.debt().add(payment.amount());
                        Supplier updatedSupplier = new Supplier(supplier.code(), supplier.name(), supplier.phone(), newDebt);
                        suppliers.set(sIndex, updatedSupplier);
                        persistSupplier(updatedSupplier);
                    }

                    // Remove from cash movements
                    cashMovements.removeIf(cm -> cm.direction() == CashDirection.EXPENSE
                            && cm.amount().compareTo(payment.amount()) == 0
                            && cm.description().contains(payment.personName()));
                    Database.deleteCashMovementByDescription("Təchizatçıya ödəniş: " + payment.personName(), payment.amount());
                }

                // Delete payment from PostgreSQL
                if (payment.id() > 0) {
                    Database.deletePayment(payment.id());
                } else {
                    Database.deletePaymentByCriteria(payment.type(), payment.customerCode(), payment.supplierCode(), payment.amount());
                }

                // Remove from payments list
                payments.remove(payment);

                // Log activity
                addLog("Ödəniş silindi", payment.type() + ": " + payment.personName() + " — " + formatMoney(payment.amount()) + " borca geri qaytarıldı.");

                // Refresh UI
                showPayments();
            } catch (Exception exception) {
                showMessage(Alert.AlertType.ERROR, "Ödəniş silinmədi", "Xəta: " + exception.getMessage());
            }
        });
    }

    private void openPaymentDialog(boolean fromCustomer) {
        ObservableList<?> people = fromCustomer ? customers : suppliers;
        if (people.isEmpty()) {
            showMessage(Alert.AlertType.INFORMATION, "Ödəniş üçün məlumat çatmır", fromCustomer ? "Əvvəl müştəri əlavə edin." : "Əvvəl təchizatçı əlavə edin.");
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(fromCustomer ? "Müştəri ödənişi" : "Təchizatçıya ödəniş");
        dialog.setHeaderText(fromCustomer ? "Müştərinin borcundan ödəniş qəbul edin" : "Təchizatçı borcuna ödəniş edin");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        ComboBox<Object> person = new ComboBox<>();
        person.getItems().addAll(people);
        person.setPromptText(fromCustomer ? "Müştərini seçin" : "Təchizatçını seçin"); person.setMaxWidth(Double.MAX_VALUE);
        person.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Object item) {
                if (item instanceof Customer customer) return customer.code() + " — " + customer.name() + " (borc: " + customer.balance() + ")";
                if (item instanceof Supplier supplier) return supplier.code() + " — " + supplier.name() + " (borc: " + formatMoney(supplier.debt()) + ")";
                return "";
            }
            @Override public Object fromString(String text) { return null; }
        });
        TextField amount = new TextField(); amount.setPromptText("Məsələn: 100.00");
        Label error = new Label(); error.getStyleClass().add("error-label");
        VBox form = new VBox(8, new Label(fromCustomer ? "Müştəri *" : "Təchizatçı *"), person,
                new Label("Ödəniş məbləği (₼) *"), amount, error);
        form.setPrefWidth(420);
        dialog.getDialogPane().setContent(form);
        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                if (person.getValue() == null) throw new IllegalArgumentException("Şəxsi seçin.");
                BigDecimal paymentAmount = strictlyPositive(amount.getText());
                if (fromCustomer) {
                    Customer customer = (Customer) person.getValue();
                    BigDecimal debt = customerDebt(customer);
                    if (paymentAmount.compareTo(debt) > 0) throw new IllegalArgumentException("Ödəniş müştəri borcundan çox ola bilməz (Borc: " + customer.balance() + ").");
                    Customer updatedCustomer = new Customer(customer.code(), customer.name(), customer.phone(),
                            formatMoney(debt.subtract(paymentAmount)), customer.managerCode());
                    customers.set(customers.indexOf(customer), updatedCustomer);
                    persistCustomer(updatedCustomer);

                    long paymentId = 0;
                    try {
                        paymentId = Database.recordPayment("Müştəridən ödəniş", customer.code(), null, customer.managerCode(), paymentAmount, LocalDateTime.now());
                    } catch (Exception dbEx) {
                        System.err.println("Ödəniş bazaya yazılmadı: " + dbEx.getMessage());
                    }
                    payments.add(new Payment(paymentId, "Müştəridən ödəniş", customer.code(), "", customer.name(), paymentAmount, customer.managerCode(), now()));
                    addCashMovement(CashDirection.INCOME, paymentAmount, "Müştəri ödənişi: " + customer.name());
                    addLog("Müştəri ödənişi", customer.name() + " — " + formatMoney(paymentAmount));
                } else {
                    Supplier supplier = (Supplier) person.getValue();
                    if (paymentAmount.compareTo(supplier.debt()) > 0) throw new IllegalArgumentException("Ödəniş təchizatçı borcundan çox ola bilməz (Borc: " + formatMoney(supplier.debt()) + ").");
                    Supplier updatedSupplier = new Supplier(supplier.code(), supplier.name(), supplier.phone(), supplier.debt().subtract(paymentAmount));
                    suppliers.set(suppliers.indexOf(supplier), updatedSupplier);
                    persistSupplier(updatedSupplier);

                    long paymentId = 0;
                    try {
                        paymentId = Database.recordPayment("Təchizatçıya ödəniş", null, supplier.code(), null, paymentAmount, LocalDateTime.now());
                    } catch (Exception dbEx) {
                        System.err.println("Ödəniş bazaya yazılmadı: " + dbEx.getMessage());
                    }
                    payments.add(new Payment(paymentId, "Təchizatçıya ödəniş", "", supplier.code(), supplier.name(), paymentAmount, "", now()));
                    addCashMovement(CashDirection.EXPENSE, paymentAmount, "Təchizatçıya ödəniş: " + supplier.name());
                    addLog("Təchizatçıya ödəniş", supplier.name() + " — " + formatMoney(paymentAmount));
                }
            } catch (IllegalArgumentException exception) {
                error.setText(exception.getMessage() == null ? "Məlumatları düzgün doldurun." : exception.getMessage());
                event.consume();
            }
        });
        dialog.showAndWait();
        showPayments();
    }

    private String now() {
        return formatTimestamp(LocalDateTime.now());
    }

    private String formatTimestamp(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    private String currentMonthKey() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    private String currentMonthTitle() {
        String[] months = {"yanvar", "fevral", "mart", "aprel", "may", "iyun", "iyul", "avqust", "sentyabr", "oktyabr", "noyabr", "dekabr"};
        LocalDate today = LocalDate.now();
        return today.getYear() + "-cı ilin " + months[today.getMonthValue() - 1] + " ayı";
    }

    private void produce(Product selectedProduct, String textAmount) {
        try {
            if (selectedProduct == null) throw new IllegalArgumentException("Məhsul seçilməyib.");
            Product product = findProduct(selectedProduct.code());
            if (product == null) throw new IllegalArgumentException("Məhsul sistemdə tapılmadı.");

            BigDecimal amount = strictlyPositive(textAmount);
            for (FormulaItem item : product.recipe()) {
                RawMaterial material = findMaterial(item.materialCode());
                BigDecimal required = item.amountPerProduct().multiply(amount);
                if (material == null || material.stock().compareTo(required) < 0) {
                    String materialName = material == null ? item.materialCode() : material.name();
                    throw new IllegalArgumentException(materialName + " kifayət deyil. Lazımdır: " + formatQuantity(required)
                            + ", anbarda: " + (material == null ? "0" : formatQuantity(material.stock())));
                }
            }

            BigDecimal productionUnitCost = product.recipe().stream()
                    .map(item -> {
                        RawMaterial rm = findMaterial(item.materialCode());
                        return rm == null ? BigDecimal.ZERO : rm.unitCost().multiply(item.amountPerProduct());
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            for (FormulaItem item : product.recipe()) {
                RawMaterial material = findMaterial(item.materialCode());
                if (material != null) {
                    int index = rawMaterials.indexOf(material);
                    if (index >= 0) {
                        rawMaterials.set(index, new RawMaterial(material.code(), material.name(), material.unit(),
                                material.stock().subtract(item.amountPerProduct().multiply(amount)), material.unitCost()));
                    }
                }
            }

            int productIndex = products.indexOf(product);
            Product updatedProduct = new Product(product.code(), product.name(), product.recipe(), product.stock().add(amount), product.salePrice(),
                    weightedAverage(product.stock(), product.unitCost(), amount, productionUnitCost));
            if (productIndex >= 0) {
                products.set(productIndex, updatedProduct);
            }

            persistInventoryQuietly();

            try {
                if (Database.isConfigured()) {
                    Database.recordProduction(product.code(), amount, productionUnitCost, LocalDateTime.now());
                }
            } catch (Exception exception) {
                System.err.println("İstehsal tarixi bazaya yazılmadı: " + exception.getMessage());
            }

            addLog("İstehsal", product.name() + " — " + formatQuantity(amount) + " ədəd istehsal edildi; xammallar resept üzrə anbardan çıxdı.");
            showMessage(Alert.AlertType.INFORMATION, "İstehsal tamamlandı", formatQuantity(amount) + " ədəd “" + product.name()
                    + "” istehsal edildi və reseptdəki xammallar anbardan silindi.");

            showProduction(product.code());
        } catch (IllegalArgumentException exception) {
            showMessage(Alert.AlertType.WARNING, "İstehsal mümkün deyil", exception.getMessage());
        } catch (Exception ex) {
            showMessage(Alert.AlertType.ERROR, "İstehsal Xətası", "Xəta baş verdi: " + ex.getMessage());
        }
    }

    private RawMaterial findMaterial(String code) {
        if (code == null) return null;
        return rawMaterials.stream().filter(material -> material.code().equals(code)).findFirst().orElse(null);
    }

    private Product findProduct(String code) {
        if (code == null) return null;
        return products.stream().filter(product -> product.code().equals(code)).findFirst().orElse(null);
    }

    private Customer findCustomer(String code) {
        if (code == null) return null;
        return customers.stream().filter(customer -> customer.code().equals(code)).findFirst().orElse(null);
    }

    private Supplier findSupplier(String code) {
        if (code == null) return null;
        return suppliers.stream().filter(supplier -> supplier.code().equals(code)).findFirst().orElse(null);
    }

    private Manager findManager(String code) {
        if (code == null) return null;
        return managers.stream().filter(manager -> manager.code().equals(code)).findFirst().orElse(null);
    }

    private Employee findEmployee(String code) {
        if (code == null) return null;
        return employees.stream().filter(employee -> employee.code().equals(code)).findFirst().orElse(null);
    }

    private String managerName(String code) {
        Manager manager = findManager(code);
        return manager == null ? "—" : manager.name();
    }

    private String formulaText(Product product) {
        List<String> parts = new ArrayList<>();
        for (FormulaItem item : product.recipe()) {
            RawMaterial material = findMaterial(item.materialCode());
            parts.add((material == null ? item.materialCode() : material.name()) + ": " + formatQuantity(item.amountPerProduct())
                    + " " + (material == null ? "" : material.unit()));
        }
        return String.join(", ", parts);
    }

    private BigDecimal positiveOrZero(String value) {
        if (value == null || value.trim().isEmpty()) return BigDecimal.ZERO;
        BigDecimal number = new BigDecimal(value.trim().replace(',', '.'));
        if (number.signum() < 0) throw new IllegalArgumentException();
        return number;
    }

    private BigDecimal strictlyPositive(String value) {
        BigDecimal number = positiveOrZero(value);
        if (number.signum() == 0) throw new IllegalArgumentException();
        return number;
    }

    private String formatQuantity(BigDecimal value) {
        if (value == null) return "0";
        return value.stripTrailingZeros().toPlainString();
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) return "0.00 ₼";
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString() + " ₼";
    }

    private BigDecimal customerDebt(Customer customer) {
        if (customer == null || customer.balance() == null) return BigDecimal.ZERO;
        try {
            return new BigDecimal(customer.balance().replace(" ₼", "").trim().replace(',', '.'));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal weightedAverage(BigDecimal oldQuantity, BigDecimal oldPrice, BigDecimal newQuantity, BigDecimal newPrice) {
        BigDecimal totalQuantity = oldQuantity.add(newQuantity);
        if (totalQuantity.signum() == 0) return BigDecimal.ZERO;
        return oldQuantity.multiply(oldPrice).add(newQuantity.multiply(newPrice)).divide(totalQuantity, 4, RoundingMode.HALF_UP);
    }

    private void showMessage(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private <T> TableColumn<T, String> column(String name, java.util.function.Function<T, String> getter) {
        TableColumn<T, String> column = new TableColumn<>(name);
        column.setCellValueFactory(data -> new SimpleStringProperty(getter.apply(data.getValue())));
        return column;
    }

    private void showPlaceholder(String name) {
        pageTitle.setText(name);
        Label message = new Label(name + " modulu növbəti mərhələdə hazırlanacaq.");
        message.getStyleClass().add("placeholder");
        layout.setCenter(new StackPane(message));
    }

    private record Customer(String code, String name, String phone, String balance, String managerCode) { }
    private record RawMaterial(String code, String name, String unit, BigDecimal stock, BigDecimal unitCost) {
        @Override public String toString() { return code + " — " + name + " (" + unit + ")"; }
    }
    private record FormulaItem(String materialCode, BigDecimal amountPerProduct) { }
    private record Product(String code, String name, List<FormulaItem> recipe, BigDecimal stock, BigDecimal salePrice, BigDecimal unitCost) {
        @Override public String toString() { return code + " — " + name; }
    }
    private record Supplier(String code, String name, String phone, BigDecimal debt) {
        @Override public String toString() { return code + " — " + name; }
    }
    private record Manager(String code, String name, BigDecimal commissionRate) {
        @Override public String toString() { return code + " — " + name; }
    }
    private record Sale(String code, String customerCode, String customerName, String productCode, String productName,
                        BigDecimal quantity, BigDecimal total, BigDecimal cost, BigDecimal paidAmount,
                        BigDecimal remainingDebt, String managerCode, String managerName, String createdAt) { }
    private record Payment(long id, String type, String customerCode, String supplierCode, String personName,
                           BigDecimal amount, String managerCode, String createdAt) { }
    private record WasteRecord(long id, String rawMaterialCode, String materialName, String unit, BigDecimal quantity, BigDecimal cost, String reason, String createdAt) { }
    private record ActivityLog(long id, String type, String description, String createdAt) { }
    private enum CashDirection { INCOME, EXPENSE }
    private record CashMovement(long id, CashDirection direction, BigDecimal amount, String description, String createdAt) { }
    private record MonthlyProductSale(String productCode, String productName, BigDecimal totalQuantity, BigDecimal totalAmount, int deliveryCount) { }
    private record Purchase(long id, String documentNo, String supplierCode, String supplierName,
                            String materialCode, String materialName, String unit,
                            BigDecimal quantity, BigDecimal totalAmount, BigDecimal paidAmount,
                            BigDecimal debtAmount, String createdAt) { }
    private record MonthlyRawMaterialPurchase(String materialCode, String materialName, String unit,
                                             BigDecimal totalQuantity, BigDecimal totalAmount,
                                             BigDecimal paidAmount, BigDecimal debtAmount, int purchaseCount) { }
    private record Employee(String code, String name, String position, String phone, BigDecimal monthlySalary) {
        @Override public String toString() { return code + " — " + name + " (" + position + ")"; }
    }
    private record SalaryRecord(long id, String employeeCode, String employeeName, String position, BigDecimal amount, String type, String note, String createdAt) { }
    private record MonthlySalaryReport(String employeeCode, String employeeName, String position, BigDecimal monthlySalary, BigDecimal totalPaid, BigDecimal advancePaid, BigDecimal bonusPaid, int paymentCount) { }
    private record CreditContract(String code, String name, String bankOrCreditor, BigDecimal totalAmount, BigDecimal monthlyAmount, BigDecimal remainingBalance) {
        @Override public String toString() { return code + " — " + name + " (" + bankOrCreditor + " • Aylıq: " + monthlyAmount + " ₼)"; }
    }
    private record CreditPaymentRecord(long id, String creditCode, String creditName, String bankOrCreditor, BigDecimal monthlyInstallment, BigDecimal paidAmount, String note, String createdAt) { }
    private record MonthlyCreditReport(String creditCode, String creditName, String bankOrCreditor, BigDecimal monthlyInstallment, BigDecimal totalPaid, BigDecimal remainingBalance, int paymentCount) { }

    public static void main(String[] args) {
        launch(args);
    }
}
