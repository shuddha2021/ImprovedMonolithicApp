import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EnhancedMonolithicApp {
    public static void main(String[] args) {
        Application app = new Application();
        app.run();
    }
}

class Application {
    private final ProductService productService;
    private final OrderService orderService;
    private final UserInterface userInterface;
    private final InventoryService inventoryService;
    private final ReportingService reportingService;

    public Application() {
        this.productService = new ProductService();
        this.inventoryService = new InventoryService();
        this.orderService = new OrderService(productService, inventoryService);
        this.reportingService = new ReportingService(orderService);
        this.userInterface = new UserInterface(productService, orderService, inventoryService, reportingService);
        initializeProducts();
    }

    private void initializeProducts() {
        productService.addProduct(new Product(1, "Laptop", 999.99));
        productService.addProduct(new Product(2, "Smartphone", 599.99));
        productService.addProduct(new Product(3, "Tablet", 299.99));
        inventoryService.updateInventory(1, 10);
        inventoryService.updateInventory(2, 20);
        inventoryService.updateInventory(3, 15);
    }

    public void run() {
        userInterface.start();
    }
}

class Product {
    private final int id;
    private final String name;
    private double price;

    public Product(int id, String name, double price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    @Override
    public String toString() {
        return String.format("Product{id=%d, name='%s', price=%.2f}", id, name, price);
    }
}

class ProductService {
    private final List<Product> products = new ArrayList<>();

    public void addProduct(Product product) {
        products.add(product);
    }

    public void listProducts() {
        System.out.println("Product Catalog:");
        products.forEach(System.out::println);
    }

    public Optional<Product> getProductById(int id) {
        return products.stream()
                .filter(product -> product.getId() == id)
                .findFirst();
    }

    public void updateProductPrice(int id, double newPrice) {
        getProductById(id).ifPresent(product -> product.setPrice(newPrice));
    }
}

class InventoryService {
    private final Map<Integer, Integer> inventory = new HashMap<>();

    public void updateInventory(int productId, int quantity) {
        inventory.put(productId, quantity);
    }

    public int getInventory(int productId) {
        return inventory.getOrDefault(productId, 0);
    }

    public boolean hasEnoughInventory(int productId, int quantity) {
        return getInventory(productId) >= quantity;
    }

    public void reduceInventory(int productId, int quantity) {
        int currentQuantity = getInventory(productId);
        updateInventory(productId, currentQuantity - quantity);
    }

    public void displayInventory() {
        System.out.println("Current Inventory:");
        inventory.forEach((id, quantity) ->
                System.out.printf("Product ID: %d, Quantity: %d%n", id, quantity));
    }
}

class Order {
    private static int orderCounter = 0;
    private final int orderId;
    private final int productId;
    private final int quantity;
    private final double totalPrice;
    private final LocalDateTime orderTime;

    public Order(int productId, int quantity, double totalPrice) {
        this.orderId = ++orderCounter;
        this.productId = productId;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.orderTime = LocalDateTime.now();
    }

    public int getOrderId() { return orderId; }
    public int getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public double getTotalPrice() { return totalPrice; }
    public LocalDateTime getOrderTime() { return orderTime; }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return String.format("Order{id=%d, productId=%d, quantity=%d, totalPrice=%.2f, orderTime=%s}",
                orderId, productId, quantity, totalPrice, orderTime.format(formatter));
    }
}

class OrderService {
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final List<Order> orders = new ArrayList<>();

    public OrderService(ProductService productService, InventoryService inventoryService) {
        this.productService = productService;
        this.inventoryService = inventoryService;
    }

    public void placeOrder(int productId, int quantity) {
        if (!inventoryService.hasEnoughInventory(productId, quantity)) {
            System.out.println("Not enough inventory to fulfill this order.");
            return;
        }

        productService.getProductById(productId)
                .ifPresentOrElse(
                        product -> processOrder(product, quantity),
                        () -> System.out.println("Product not found.")
                );
    }

    private void processOrder(Product product, int quantity) {
        double total = product.getPrice() * quantity;
        Order order = new Order(product.getId(), quantity, total);
        orders.add(order);
        inventoryService.reduceInventory(product.getId(), quantity);
        System.out.printf("Order placed successfully. Order details: %s%n", order);
    }

    public List<Order> getOrders() {
        return new ArrayList<>(orders);
    }
}

class ReportingService {
    private final OrderService orderService;

    public ReportingService(OrderService orderService) {
        this.orderService = orderService;
    }

    public void generateSalesReport() {
        System.out.println("Sales Report:");
        Map<Integer, Double> salesByProduct = new HashMap<>();
        Map<Integer, Integer> quantityByProduct = new HashMap<>();

        for (Order order : orderService.getOrders()) {
            int productId = order.getProductId();
            salesByProduct.merge(productId, order.getTotalPrice(), Double::sum);
            quantityByProduct.merge(productId, order.getQuantity(), Integer::sum);
        }

        salesByProduct.forEach((productId, totalSales) -> {
            int quantitySold = quantityByProduct.get(productId);
            System.out.printf("Product ID: %d, Total Sales: $%.2f, Quantity Sold: %d%n",
                    productId, totalSales, quantitySold);
        });
    }
}

class UserInterface {
    private final Scanner scanner;
    private final ProductService productService;
    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final ReportingService reportingService;

    public UserInterface(ProductService productService, OrderService orderService,
                         InventoryService inventoryService, ReportingService reportingService) {
        this.scanner = new Scanner(System.in);
        this.productService = productService;
        this.orderService = orderService;
        this.inventoryService = inventoryService;
        this.reportingService = reportingService;
    }

    public void start() {
        while (true) {
            displayMenu();
            int choice = getUserChoice();
            processUserChoice(choice);
            if (choice == 7) break;
        }
        scanner.close();
    }

    private void displayMenu() {
        System.out.println("\n1. List Products");
        System.out.println("2. Place Order");
        System.out.println("3. View Inventory");
        System.out.println("4. Update Product Price");
        System.out.println("5. Update Inventory");
        System.out.println("6. Generate Sales Report");
        System.out.println("7. Exit");
        System.out.print("Enter choice: ");
    }

    private int getUserChoice() {
        while (!scanner.hasNextInt()) {
            System.out.println("Invalid input. Please enter a number.");
            scanner.next(); // Consume the invalid input
        }
        return scanner.nextInt();
    }

    private void processUserChoice(int choice) {
        switch (choice) {
            case 1 -> productService.listProducts();
            case 2 -> placeOrder();
            case 3 -> inventoryService.displayInventory();
            case 4 -> updateProductPrice();
            case 5 -> updateInventory();
            case 6 -> reportingService.generateSalesReport();
            case 7 -> System.out.println("Exiting...");
            default -> System.out.println("Invalid choice. Try again.");
        }
    }

    private void placeOrder() {
        System.out.print("Enter product ID: ");
        int productId = getUserChoice();
        System.out.print("Enter quantity: ");
        int quantity = getUserChoice();
        orderService.placeOrder(productId, quantity);
    }

    private void updateProductPrice() {
        System.out.print("Enter product ID: ");
        int productId = getUserChoice();
        System.out.print("Enter new price: ");
        double newPrice = scanner.nextDouble();
        productService.updateProductPrice(productId, newPrice);
        System.out.println("Product price updated successfully.");
    }

    private void updateInventory() {
        System.out.print("Enter product ID: ");
        int productId = getUserChoice();
        System.out.print("Enter new quantity: ");
        int newQuantity = getUserChoice();
        inventoryService.updateInventory(productId, newQuantity);
        System.out.println("Inventory updated successfully.");
    }
}