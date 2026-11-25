package com.omnirig.controllers;

import com.omnirig.models.Category;
import com.omnirig.models.Product;
import com.omnirig.models.SavedBuild;
import com.omnirig.services.AuthService;
import com.omnirig.services.MockAuthService;
import com.omnirig.services.MockProductService;
import com.omnirig.services.ProductService;
import com.omnirig.utils.AuthModalHelper;
import com.omnirig.utils.CatalogFilterBarHelper;
import com.omnirig.utils.ModalOverlayHelper;
import com.omnirig.utils.ProductUiFactory;
import com.omnirig.utils.WindowControlsHelper;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

import java.io.IOException;
import java.util.*;

public class CustomerDashboardController {

    private static final String SVG_USER =
        "M 12 10 m -3.5 0 a 3.5 3.5 0 1 0 7 0 a 3.5 3.5 0 1 0 -7 0 " +
        "M 5 20 C 5 16.5 8 14.5 12 14.5 C 16 14.5 19 16.5 19 20";

    @FXML private StackPane rootStack;
    @FXML private BorderPane mainPane;
    @FXML private ScrollPane catalogScroll;
    @FXML private HBox topBar;
    @FXML private Button btnMinimize;
    @FXML private Button btnMaximize;
    @FXML private Button btnClose;

    @FXML private TextField searchNameField;
    @FXML private FlowPane categoryPillsBox;
    @FXML private FlowPane filterBarBox;
    @FXML private Button accountButton;
    @FXML private FlowPane productGrid;
    @FXML private Label sectionTitle;
    @FXML private Button cartButton;

    private final ProductService productService = new MockProductService();
    private final AuthService authService = new MockAuthService();
    private final Map<Product, Integer> cart = new LinkedHashMap<>();
    private final Map<String, List<SavedBuild>> savedBuildsByEmail = new HashMap<>();
    private CheckoutController.CustomerProfile loggedInUser = null;

    private Category selectedCategory = null;
    private WindowControlsHelper windowControls;
    private CatalogFilterBarHelper filterBarHelper;

    @FXML
    public void initialize() {
        windowControls = new WindowControlsHelper(mainPane, topBar, btnMinimize, btnMaximize, btnClose);
        windowControls.attach();

        setupAccountButtonIcon();

        filterBarHelper = new CatalogFilterBarHelper(
            categoryPillsBox,
            filterBarBox,
            this::handleCategorySelected,
            this::applySearchAndFilters
        );
        filterBarHelper.buildCategoryPills();
        filterBarHelper.rebuildFiltersFor(null, productService.getAllProducts());

        renderProducts(productService.getAllProducts());
        updateCartButtonLabel();
    }

    private void setupAccountButtonIcon() {
        if (accountButton == null) return;
        SVGPath svg = new SVGPath();
        svg.setContent(SVG_USER);
        svg.setFill(Color.TRANSPARENT);
        svg.setStroke(Color.web("#E2E8F0"));
        svg.setStrokeWidth(1.8);
        svg.setStrokeLineCap(StrokeLineCap.ROUND);
        svg.setStrokeLineJoin(StrokeLineJoin.ROUND);
        svg.setScaleX(0.75);
        svg.setScaleY(0.75);

        StackPane iconBox = new StackPane(new Group(svg));
        iconBox.setPrefSize(18, 18);
        iconBox.setMinSize(18, 18);
        iconBox.setMaxSize(18, 18);

        accountButton.setGraphic(iconBox);
        accountButton.setGraphicTextGap(7);
        accountButton.setMinWidth(Region.USE_PREF_SIZE);
    }

    // --- WINDOW CONTROLS ---
    @FXML private void handleMinimize() { windowControls.minimize(); }
    @FXML private void handleMaximize() { windowControls.toggleMaximize(); }
    @FXML private void handleClose()    { windowControls.close(); }
    @FXML private void handleLogoClick(){ showCatalogView(); }

    // --- CATALOG & FILTERS ---
    private void handleCategorySelected(Category category) {
        selectedCategory = category;
        sectionTitle.setText(category == null ? "Όλα τα Προϊόντα" : category.getDisplayName());
        List<Product> catProducts = (category == null)
            ? productService.getAllProducts()
            : productService.getProductsByCategory(category);
        filterBarHelper.rebuildFiltersFor(category, catProducts);
        showCatalogView();
        applySearchAndFilters();
    }

    private void applySearchAndFilters() {
        List<Product> searched = productService.searchProducts(searchNameField.getText(), null, selectedCategory);
        renderProducts(filterBarHelper.applyFiltersAndSort(searched));
    }

    private void renderProducts(List<Product> products) {
        productGrid.getChildren().clear();
        if (products.isEmpty()) {
            Label empty = new Label("Δεν βρέθηκαν προϊόντα.");
            empty.setStyle("-fx-text-fill: #64748B; -fx-font-size: 15px;");
            productGrid.getChildren().add(empty);
            return;
        }
        for (Product p : products) {
            productGrid.getChildren().add(
                ProductUiFactory.createProductCard(p, this::addToCart, this::showProductDetails)
            );
        }
    }

    private void showCatalogView() {
        if (mainPane.getCenter() != catalogScroll) {
            setCenterView(catalogScroll);
        }
    }

    private void showProductDetails(Product p) {
        ScrollPane details = ProductUiFactory.createProductDetailsView(p, this::showCatalogView, this::addToCart);
        setCenterView(details);
    }

    private void setCenterView(Node view) {
        mainPane.setCenter(view);
        ProductUiFactory.playViewEnterAnimation(view);
    }

    @FXML
    public void handleSearch() {
        showCatalogView();
        applySearchAndFilters();
    }

    // --- CART & CHECKOUT ---
    private void addToCart(Product p) {
        cart.merge(p, 1, Integer::sum);
        updateCartButtonLabel();
    }

    private void updateCartButtonLabel() {
        int count = cart.values().stream().mapToInt(Integer::intValue).sum();
        cartButton.setText("Καλάθι (" + count + ")");
    }

    @FXML
    public void handleOpenCart() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/omnirig/views/Cart.fxml"));
            ScrollPane cartPane = loader.load();

            CartController controller = loader.getController();
            controller.init(cart, this::showCatalogView, this::updateCartButtonLabel, this::handleCheckoutEntry);
            setCenterView(cartPane);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleCheckoutEntry() {
        if (loggedInUser != null) {
            openCheckoutView(loggedInUser);
            return;
        }
        ModalOverlayHelper.showCheckoutAuthModal(
            rootStack,
            () -> openLoginModal(() -> openCheckoutView(loggedInUser)),
            () -> openRegisterModal(() -> openCheckoutView(loggedInUser)),
            () -> openCheckoutView(null)
        );
    }

    private void openCheckoutView(CheckoutController.CustomerProfile user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/omnirig/views/Checkout.fxml"));
            ScrollPane checkoutPane = loader.load();

            CheckoutController controller = loader.getController();
            controller.init(cart, user, this::handleOpenCart, () -> {
                updateCartButtonLabel();
                showCatalogView();
            });
            setCenterView(checkoutPane);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- PC BUILDER & SAVED BUILDS ---
    @FXML
    public void handleOpenPcBuilder() {
        openPcBuilderWith(null);
    }

    private void openPcBuilderWith(Map<Category, Product> initialParts) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/omnirig/views/PcBuilder.fxml"));
            ScrollPane builderPane = loader.load();

            PcBuilderController controller = loader.getController();
            controller.init(
                rootStack,
                productService,
                this::showCatalogView,
                products -> {
                    for (Product p : products) addToCart(p);
                    showCatalogView();
                },
                (buildName, parts) -> {
                    if (loggedInUser == null) {
                        openLoginModal(() -> saveBuildForCurrentUser(buildName, parts));
                    } else {
                        saveBuildForCurrentUser(buildName, parts);
                    }
                }
            );
            if (initialParts != null) {
                controller.loadBuild(initialParts);
            }
            setCenterView(builderPane);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveBuildForCurrentUser(String buildName, Map<Category, Product> parts) {
        if (loggedInUser == null) return;
        double total = parts.values().stream().mapToDouble(Product::getPrice).sum();
        SavedBuild sb = new SavedBuild(UUID.randomUUID().toString(), buildName, parts, total);
        savedBuildsByEmail
            .computeIfAbsent(loggedInUser.email().toLowerCase(), k -> new ArrayList<>())
            .add(sb);
    }

    // --- AUTH & ACCOUNT PAGE ---
    @FXML
    private void handleAccountClick() {
        if (loggedInUser == null) {
            openLoginModal(null);
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/omnirig/views/Account.fxml"));
            ScrollPane accountPane = loader.load();

            List<SavedBuild> userBuilds = savedBuildsByEmail
                .computeIfAbsent(loggedInUser.email().toLowerCase(), k -> new ArrayList<>());

            AccountController controller = loader.getController();
            controller.init(
                authService,
                loggedInUser,
                userBuilds,
                this::showCatalogView,
                updatedUser -> {
                    loggedInUser = updatedUser;
                    accountButton.setText(updatedUser.firstName());
                },
                build -> openPcBuilderWith(build.parts()),
                build -> userBuilds.remove(build),
                () -> {
                    loggedInUser = null;
                    accountButton.setText("Λογαριασμός");
                    showCatalogView();
                },
                () -> {
                    savedBuildsByEmail.remove(loggedInUser.email().toLowerCase());
                    loggedInUser = null;
                    accountButton.setText("Λογαριασμός");
                    showCatalogView();
                }
            );
            setCenterView(accountPane);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleLogin() {
        openLoginModal(null);
    }

    @FXML
    public void handleRegister() {
        openRegisterModal(null);
    }

    private void openLoginModal(Runnable afterLoginAction) {
        AuthModalHelper.showLoginModal(
            rootStack,
            authService,
            user -> {
                loggedInUser = user;
                accountButton.setText(user.firstName());
                if (afterLoginAction != null) afterLoginAction.run();
            },
            () -> openRegisterModal(afterLoginAction),
            this::openAdminPanel
        );
    }

    private void openRegisterModal(Runnable afterRegisterAction) {
        AuthModalHelper.showRegisterModal(
            rootStack,
            authService,
            user -> {
                loggedInUser = user;
                accountButton.setText(user.firstName());
                if (afterRegisterAction != null) afterRegisterAction.run();
            },
            () -> openLoginModal(afterRegisterAction)
        );
    }

    private void openAdminPanel() {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/omnirig/views/AdminPanel.fxml"));
        ScrollPane adminPane = loader.load();

        AdminPanelController controller = loader.getController();
        controller.init(rootStack, productService, () -> {
            handleCategorySelected(selectedCategory);
        });
        setCenterView(adminPane);
    } catch (IOException e) {
        e.printStackTrace();
    }
    
}
}