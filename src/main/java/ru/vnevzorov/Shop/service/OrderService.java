package ru.vnevzorov.Shop.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vnevzorov.Shop.enumeration.Status;
import ru.vnevzorov.Shop.model.Order;
import ru.vnevzorov.Shop.model.OrderedProduct;
import ru.vnevzorov.Shop.model.ShoppingCart;
import ru.vnevzorov.Shop.model.user.User;
import ru.vnevzorov.Shop.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {
    private static final Logger log = LogManager.getLogger();

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private UserService userService;

    @Autowired
    OrderedProductService orderedProductService;

    @Autowired
    EmailService emailService;

    public Order prepareNewOrder(String shoppingCartId) {
        User user = userService.getUserByLogin("firstUser"); //FIXME

        ShoppingCart cart = shoppingCartService.getById(shoppingCartId);
        Order order = new Order();
        order.setUser(user);
        order.setOrderedProducts(cart.getOrderedProducts());
        order.setTotalPrice(cart.getTotalPrice());

        return order;
    }

    @Transactional
    public void saveOrder(Order order) {
        User user = order.getUser();
        order.setTotalPrice(user.getShoppingCart().getTotalPrice());
        order.setPayment(paymentService.getPayment(order.getPayment().getType()));
        order.setShipment(shipmentService.getShipment(order.getShipment().getType()));
        order.setDate(LocalDateTime.now());
        order.setStatus(Status.CREATED);
        setOrderedProducts(order);

        orderRepository.save(order);
        shoppingCartService.deleteCart();
    }

    public void setOrderedProducts(Order order) {
        User user = order.getUser();
        List<OrderedProduct> orderedProducts = user.getShoppingCart().getOrderedProducts();
        orderedProducts.forEach(product -> product.setOrder(order));

        order.setOrderedProducts(orderedProducts);
    }

    public Iterable<Order> getAll() {
        return orderRepository.findAll();
    }

    @Transactional
    public void changeOrderStatus(Order orderStatus) {
        if (orderStatus == null || orderStatus.getStatus() == null || orderStatus.getStatus().equals("")) {
            throw new RuntimeException("неверные данные");
        }
        Order order = orderRepository.findById(orderStatus.getId()).get();
        Status newStatus = orderStatus.getStatus();
        order.setStatus(newStatus);
        log.info("order status changed: orderId=" + order.getId() + " newStatus=" + newStatus);

        emailService.sendMessage(order.getUser().getEmail(),
                "Order " + order.getId(),
                "Status of your order " + order.getId() + " was changed to " + order.getStatus() + "\r\nThank you for choosing our service");
    }
}