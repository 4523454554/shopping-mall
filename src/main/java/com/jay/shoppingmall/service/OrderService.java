package com.jay.shoppingmall.service;

import com.jay.shoppingmall.domain.cart.CartRepository;
import com.jay.shoppingmall.domain.image.Image;
import com.jay.shoppingmall.domain.image.ImageRelation;
import com.jay.shoppingmall.domain.image.ImageRepository;
import com.jay.shoppingmall.domain.item.ItemRepository;
import com.jay.shoppingmall.domain.item.item_option.ItemOptionRepository;
import com.jay.shoppingmall.domain.order.DeliveryStatus;
import com.jay.shoppingmall.domain.order.Order;
import com.jay.shoppingmall.domain.order.OrderRepository;
import com.jay.shoppingmall.domain.order.order_item.OrderItem;
import com.jay.shoppingmall.domain.order.order_item.OrderItemRepository;
import com.jay.shoppingmall.domain.order.order_item.order_delivery.OrderDelivery;
import com.jay.shoppingmall.domain.payment.Payment;
import com.jay.shoppingmall.domain.payment.PaymentRepository;
import com.jay.shoppingmall.domain.payment.payment_per_seller.PaymentPerSeller;
import com.jay.shoppingmall.domain.payment.payment_per_seller.PaymentPerSellerRepository;
import com.jay.shoppingmall.domain.user.User;
//import com.jay.shoppingmall.dto.response.order.OrderDetailResponse;
import com.jay.shoppingmall.domain.virtual_delivery_company.VirtualDeliveryCompany;
import com.jay.shoppingmall.domain.virtual_delivery_company.VirtualDeliveryCompanyRepository;
import com.jay.shoppingmall.dto.response.order.OrderDetailResponse;
import com.jay.shoppingmall.dto.response.order.OrderItemCommonResponse;
import com.jay.shoppingmall.dto.response.order.OrderItemResponse;
import com.jay.shoppingmall.dto.response.order.SimpleOrderResponse;
import com.jay.shoppingmall.dto.response.item.ItemAndQuantityResponse;
import com.jay.shoppingmall.dto.response.order.payment.PaymentDetailResponse;
import com.jay.shoppingmall.dto.response.order.payment.PaymentPerSellerResponse;
import com.jay.shoppingmall.dto.response.seller.SellerResponse;
import com.jay.shoppingmall.exception.exceptions.*;
import com.jay.shoppingmall.service.handler.FileHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Transactional
@Service
public class OrderService {

    private final CartService cartService;

    private final VirtualDeliveryCompanyRepository virtualDeliveryCompanyRepository;
    private final OrderRepository orderRepository;
    private final FileHandler fileHandler;
    private final ImageRepository imageRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentPerSellerRepository paymentPerSellerRepository;

    public Map<SellerResponse, List<ItemAndQuantityResponse>> orderProcess(User user) {
        final Map<SellerResponse, List<ItemAndQuantityResponse>> responseListMapBySeller = cartService.showCartItemsList(user);

        for (SellerResponse sellerResponse : responseListMapBySeller.keySet()) {
            final List<ItemAndQuantityResponse> itemAndQuantityResponses = responseListMapBySeller.get(sellerResponse);
            final List<ItemAndQuantityResponse> selectedCarts = itemAndQuantityResponses.stream().filter(ItemAndQuantityResponse::getIsSelected).collect(Collectors.toList());

            responseListMapBySeller.put(sellerResponse, selectedCarts);
        }
        return responseListMapBySeller;
    }

    public OrderDetailResponse showOrderDetail(final Long orderId, final User user) {

        final Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("???????????? ????????? ????????????"));

        if (!Objects.equals(user.getId(), order.getUser().getId())) {
            throw new NotValidException("????????? ???????????????");
        }
        LocalDateTime orderDate = order.getCreatedDate();

        //????????????
        final List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

        List<OrderItemResponse> orderItemResponses = new ArrayList<>();
        for (OrderItem orderItem : orderItems) {

            boolean isTrackingStarted = false;
            String trackingNumber = "";
            final DeliveryStatus deliveryStatus = orderItem.getOrderDelivery().getDeliveryStatus();
            if (deliveryStatus.equals(DeliveryStatus.DELIVERING) || deliveryStatus.equals(DeliveryStatus.SHIPPED) || deliveryStatus.equals(DeliveryStatus.DELIVERED) || deliveryStatus.equals(DeliveryStatus.FINISHED)) {
                trackingNumber = virtualDeliveryCompanyRepository.findByOrderItemId(orderItem.getId())
                        .orElseThrow(() -> new UserNotFoundException("????????? ???????????????")).getTrackingNumber();

                isTrackingStarted = true;
            }
            boolean isDelivered = deliveryStatus.equals(DeliveryStatus.DELIVERED);

            final Image image = imageRepository.findByImageRelationAndId(ImageRelation.ITEM_MAIN, orderItem.getMainImageId());
            final String mainImage = fileHandler.getStringImage(image);

            OrderItemResponse orderItemResponse = OrderItemResponse.builder()
                    .orderDate(orderDate)
                    .itemName(orderItem.getItem().getName())
                    .option1(orderItem.getItemOption().getOption1())
                    .option2(orderItem.getItemOption().getOption2())
                    .mainImage(mainImage)
                    .sellerCompanyName(orderItem.getSeller().getCompanyName())
                    .orderItemId(orderItem.getId())
                    .itemPrice(orderItem.getPriceAtPurchase())
                    .quantity(orderItem.getQuantity())
                    .deliveryStatus(deliveryStatus.getValue())
                    .trackingNumber(trackingNumber)
                    .isTrackingStarted(isTrackingStarted)
                    .isDelivered(isDelivered)
                    .build();
            orderItemResponses.add(orderItemResponse);
        }

        //???????????? ????????? ?????? ?????? ??????
        final List<PaymentPerSeller> paymentPerSellers = paymentPerSellerRepository.findByPayment(order.getPayment());
        List<PaymentPerSellerResponse> paymentPerSellerResponses = new ArrayList<>();

        long paymentTotalPrice = 0L;
        int paymentTotalShippingFee = 0;
        for (PaymentPerSeller paymentPerSeller : paymentPerSellers) {
            final PaymentPerSellerResponse paymentPerSellerResponse = PaymentPerSellerResponse.builder()
                    .paymentPerSellerId(paymentPerSeller.getId())
                    .itemTotalPricePerSeller(paymentPerSeller.getItemTotalPricePerSeller())
                    .itemTotalQuantityPerSeller(paymentPerSeller.getItemTotalQuantityPerSeller())
                    .itemShippingFeePerSeller(paymentPerSeller.getItemShippingFeePerSeller())
                    .build();
            paymentTotalPrice += paymentPerSeller.getItemTotalPricePerSeller();
            paymentTotalShippingFee += paymentPerSeller.getItemShippingFeePerSeller();

            paymentPerSellerResponses.add(paymentPerSellerResponse);
        }
        final Payment payment = order.getPayment();

        PaymentDetailResponse paymentDetailResponse = PaymentDetailResponse.builder()
                .pg(payment.getPg().getName())
                .payMethod(payment.getPayMethod().getName())
                .paymentTotalPrice(paymentTotalPrice)
                .paymentTotalShippingFee(paymentTotalShippingFee)
                .buyerName(payment.getBuyerName())
                .buyerAddr("(" + payment.getBuyerPostcode() + ") " + payment.getBuyerAddr())
                .buyerEmail(payment.getBuyerEmail())
                .buyerTel(payment.getBuyerTel())
                .receiverName(payment.getReceiverInfo().getReceiverName())
                .receiverAddress("(" + payment.getReceiverInfo().getReceiverPostcode() + ") " + payment.getReceiverInfo().getReceiverAddress())
                .receiverEmail(payment.getReceiverInfo().getReceiverEmail())
                .receiverPhoneNumber(payment.getReceiverInfo().getReceiverPhoneNumber())
                .build();

        return OrderDetailResponse.builder()
                .orderItemResponses(orderItemResponses)
                .paymentDetailResponse(paymentDetailResponse)
                .paymentPerSellerResponses(paymentPerSellerResponses)
                .build();
    }

    public List<SimpleOrderResponse> showOrders(User user, Pageable pageable) {

        final Page<Order> orders = orderRepository.findByUserId(user.getId(), pageable)
                .orElseThrow(() -> new OrderNotFoundException("?????? ?????? ?????? ??? ??????"));
        //todo pageable ?????? ????????? ??????
        List<SimpleOrderResponse> simpleOrderResponses = new ArrayList<>();
        for (Order order : orders) {
            Payment payment = order.getPayment();
            if (!payment.getIsValidated()) {
                throw new PaymentFailedException("????????? ???????????? ?????? ???????????????");
            }
            //?????? ??????
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());

            //?????? ?????? ??? ?????? ?????? ????????? ?????? ????????? ?????? ?????? ?????????.
            final List<String> deliveryStatuses = orderItems.stream().map(OrderItem::getOrderDelivery).map(OrderDelivery::getDeliveryStatus).map(DeliveryStatus::getValue).distinct().collect(Collectors.toList());
            //?????? ?????? ??? ?????? ?????? ????????? ?????? ????????????
            //????????? ??????.
            final OrderItem mostExpensiveOneAtOrder = orderItems.stream().max(Comparator.comparingLong(OrderItem::getPriceAtPurchase))
                    .orElseThrow(() -> new ItemNotFoundException("????????? ???????????? ????????????"));

            String mainImage = fileHandler.getStringImage(imageRepository.findByImageRelationAndId(ImageRelation.ITEM_MAIN, mostExpensiveOneAtOrder.getMainImageId()));

            simpleOrderResponses.add(SimpleOrderResponse.builder()
                    .id(order.getId())
                    .orderDate(order.getCreatedDate())
                    .mainImage(mainImage)
                    .amount(payment.getAmount())
                    .deliveryStatuses(deliveryStatuses)
                    .merchantUid(payment.getMerchantUid())
                    .name(payment.getName())
                    .build());
        }
        return simpleOrderResponses;
    }
}
