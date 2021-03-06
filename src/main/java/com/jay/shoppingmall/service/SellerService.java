package com.jay.shoppingmall.service;

import com.jay.shoppingmall.common.model.OptionValue;
import com.jay.shoppingmall.domain.cart.CartRepository;
import com.jay.shoppingmall.domain.image.Image;
import com.jay.shoppingmall.domain.image.ImageRelation;
import com.jay.shoppingmall.domain.image.ImageRepository;
import com.jay.shoppingmall.domain.item.Item;
import com.jay.shoppingmall.domain.item.ItemRepository;
import com.jay.shoppingmall.domain.item.item_option.ItemOption;
import com.jay.shoppingmall.domain.item.item_option.ItemOptionRepository;
import com.jay.shoppingmall.domain.item.item_price.ItemPrice;
import com.jay.shoppingmall.domain.item.item_price.ItemPriceRepository;
import com.jay.shoppingmall.domain.item.item_price.price_history.ItemPriceHistory;
import com.jay.shoppingmall.domain.item.item_price.price_history.ItemPriceHistoryRepository;
import com.jay.shoppingmall.domain.item.item_stock.ItemStock;
import com.jay.shoppingmall.domain.item.item_stock.ItemStockRepository;
import com.jay.shoppingmall.domain.item.item_stock.item_stock_history.ItemStockHistory;
import com.jay.shoppingmall.domain.item.item_stock.item_stock_history.ItemStockHistoryRepository;
import com.jay.shoppingmall.domain.item.temporary.ItemTemporary;
import com.jay.shoppingmall.domain.item.temporary.ItemTemporaryRepository;
import com.jay.shoppingmall.domain.notification.NotificationRepository;
import com.jay.shoppingmall.domain.notification.me_notification.MeNotification;
import com.jay.shoppingmall.domain.notification.me_notification.MeNotificationRepository;
import com.jay.shoppingmall.domain.notification.model.NotificationType;
import com.jay.shoppingmall.domain.notification.qna_notification.QnaNotification;
import com.jay.shoppingmall.domain.order.DeliveryStatus;
import com.jay.shoppingmall.domain.order.Order;
import com.jay.shoppingmall.domain.order.OrderRepository;
import com.jay.shoppingmall.domain.order.order_item.OrderItem;
import com.jay.shoppingmall.domain.order.order_item.OrderItemRepository;
import com.jay.shoppingmall.domain.payment.Payment;
import com.jay.shoppingmall.domain.payment.payment_per_seller.PaymentPerSeller;
import com.jay.shoppingmall.domain.payment.payment_per_seller.PaymentPerSellerRepository;
import com.jay.shoppingmall.domain.qna.Qna;
import com.jay.shoppingmall.domain.qna.QnaRepository;
import com.jay.shoppingmall.domain.seller.Seller;
import com.jay.shoppingmall.domain.seller.SellerRepository;
import com.jay.shoppingmall.domain.seller.seller_bank_account_history.SellerBankAccountHistory;
import com.jay.shoppingmall.domain.seller.seller_bank_account_history.SellerBankAccountHistoryRepository;
import com.jay.shoppingmall.domain.user.Role;
import com.jay.shoppingmall.domain.user.User;
import com.jay.shoppingmall.domain.user.UserRepository;
import com.jay.shoppingmall.domain.user.model.Address;
import com.jay.shoppingmall.dto.request.*;
import com.jay.shoppingmall.dto.request.qna.QnaAnswerRequest;
import com.jay.shoppingmall.dto.response.order.OrderDetailResponse;
import com.jay.shoppingmall.dto.response.order.OrderItemResponse;
import com.jay.shoppingmall.dto.response.order.payment.PaymentDetailResponse;
import com.jay.shoppingmall.dto.response.order.payment.PaymentPerSellerResponse;
import com.jay.shoppingmall.dto.response.order.payment.RecentPaymentPerSellerResponse;
import com.jay.shoppingmall.dto.response.order.payment.RecentPaymentPerSellerSimpleResponse;
import com.jay.shoppingmall.dto.response.seller.SellerBankAccountHistoryResponse;
import com.jay.shoppingmall.dto.response.seller.SellerBankResponse;
import com.jay.shoppingmall.dto.response.seller.SellerDefaultSettingsResponse;
import com.jay.shoppingmall.dto.response.item.ItemTemporaryResponse;
import com.jay.shoppingmall.dto.response.seller.StatisticsResponse;
import com.jay.shoppingmall.exception.exceptions.*;
import com.jay.shoppingmall.service.handler.FileHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class SellerService {

    private final SellerRepository sellerRepository;
    private final UserRepository userRepository;
    private final ImageRepository imageRepository;
    private final ItemRepository itemRepository;
    private final QnaRepository qnaRepository;
    private final ItemTemporaryRepository itemTemporaryRepository;
    private final CartRepository cartRepository;
    private final ItemOptionRepository itemOptionRepository;
    private final ItemPriceRepository itemPriceRepository;
    private final ItemStockRepository itemStockRepository;
    private final ItemStockHistoryRepository itemStockHistoryRepository;
    private final ItemPriceHistoryRepository itemPriceHistoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentPerSellerRepository paymentPerSellerRepository;
    private final OrderRepository orderRepository;
    private final SellerBankAccountHistoryRepository sellerBankAccountHistoryRepository;
    private final NotificationRepository<?> notificationRepository;
    private final MeNotificationRepository meNotificationRepository;

    private final FileHandler fileHandler;

    public Long writeItem(WriteItemRequest writeItemRequest, final MultipartFile file, final List<MultipartFile> files, final User user) {
        final Seller seller = sellerRepository.findByUserIdAndIsActivatedTrue(user.getId())
                .orElseThrow(() -> new SellerNotFoundException("???????????? ????????????"));

        Item item = Item.builder()
                .name(writeItemRequest.getItemName())
                .description(writeItemRequest.getDescription())
                .brandName(writeItemRequest.getItemBrandName())
                .seller(seller)
                .build();
        final Long savedItem = itemRepository.save(item).getId();

        Image mainImage = fileHandler.parseFilesInfo(file, ImageRelation.ITEM_MAIN, item.getId());
        imageRepository.save(mainImage);

        //MultiPartFile??? input??? ????????? ''?????? ??????????????? ????????? ?????? ??????.
        if (files != null) {
            for (MultipartFile multipartFile : files) {
                imageRepository.save(fileHandler.parseFilesInfo(multipartFile, ImageRelation.ITEM_DESCRIPTION, item.getId()));
            }
        }

        ItemPrice itemPrice = getItemPrice(writeItemRequest.getSalePrice(), writeItemRequest.getOriginalPrice());

        ItemStock itemStock = getItemStock(writeItemRequest.getStock());

        ItemOption itemOption = ItemOption.builder()
                .option1("????????????")
                .option2("????????????")
                .isOptionMainItem(true)
                .itemStock(itemStock)
                .itemPrice(itemPrice)
                .item(item)
                .build();
        itemOptionRepository.save(itemOption);

        return savedItem;
    }

    public Long writeOptionItem(final ApiWriteItemRequest apiWriteItemRequest, final List<OptionValue> optionValues, final MultipartFile file, final List<MultipartFile> files, final User user) {
        final Seller seller = sellerRepository.findByUserIdAndIsActivatedTrue(user.getId())
                .orElseThrow(() -> new SellerNotFoundException("???????????? ????????????"));

        final long count = optionValues.stream().filter(OptionValue::getIsOptionMainItem).count();
        if (count != 1) {
            throw new NotValidException("?????? ????????? ????????? ????????? ??? ????????????");
        }

        Item item = Item.builder()
                .description(apiWriteItemRequest.getDescription())
                .brandName(apiWriteItemRequest.getItemBrandName())
                .name(apiWriteItemRequest.getItemName())
                .seller(seller)
                .build();
        Item savedItem = itemRepository.save(item);

        Image image = fileHandler.parseFilesInfo(file, ImageRelation.ITEM_MAIN, item.getId());
        imageRepository.save(image);

        //REST??? ??? List<MultipartFile>??? empty??? null??? ??????.
        if (files != null) {
            for (MultipartFile multipartFile : files) {
                imageRepository.save(fileHandler.parseFilesInfo(multipartFile, ImageRelation.ITEM_DESCRIPTION, item.getId()));
            }
        }
        for (OptionValue optionValue : optionValues) {
            if (itemOptionRepository.findByOption1AndOption2AndItemId(optionValue.getOption1(), optionValue.getOption2(), item.getId()).isPresent()) {
                throw new AlreadyExistsException("?????? ?????? ????????? ???????????????");
            }
            final Long salePrice = optionValue.getOptionSalePrice();
            final Long optionOriginalPrice = optionValue.getOptionOriginalPrice();
            final Integer optionStock = optionValue.getOptionStock();

            ItemPrice itemPrice = getItemPrice(salePrice, optionOriginalPrice);

            ItemStock itemStock = getItemStock(optionStock);

            ItemOption itemOption = ItemOption.builder()
                    .option1(optionValue.getOption1())
                    .option2(optionValue.getOption2())
                    .isOptionMainItem(optionValue.getIsOptionMainItem())
                    .itemStock(itemStock)
                    .itemPrice(itemPrice)
                    .item(item)
                    .build();
            itemOptionRepository.save(itemOption);
        }
        return savedItem.getId();
    }

    private ItemStock getItemStock(final Integer stock) {
        ItemStock itemStock = ItemStock.builder()
                .stock(stock)
                .build();
        itemStockRepository.save(itemStock);
        ItemStockHistory itemStockHistory = ItemStockHistory.builder()
                .itemStock(itemStock)
                .stock(itemStock.getStock())
                .stockChangedDate(LocalDateTime.now())
                .build();
        itemStockHistoryRepository.save(itemStockHistory);
        return itemStock;
    }

    private ItemPrice getItemPrice(final Long salePrice, final Long originalPrice) {
        ItemPrice itemPrice = ItemPrice.builder()
                .priceNow(salePrice)
                .originalPrice(originalPrice == null ? salePrice : originalPrice)
                .build();
        itemPriceRepository.save(itemPrice);
        ItemPriceHistory itemPriceHistory = ItemPriceHistory.builder()
                .itemPrice(itemPrice)
                .price(itemPrice.getPriceNow())
                .priceUpdateDate(LocalDateTime.now())
                .build();
        itemPriceHistoryRepository.save(itemPriceHistory);
        return itemPrice;
    }


    public Boolean sellerAgreeCheck(final SellerAgreeRequest sellerAgreeRequest, final User user) {
        if (!sellerAgreeRequest.getIsSellerAgree() || !sellerAgreeRequest.getIsLawAgree()) {
            throw new NotValidException("?????? ????????? ??????????????????");
        }
        if (sellerRepository.findByUserIdAndIsActivatedTrue(user.getId()).isPresent()) {
            throw new AlreadyExistsException("?????? ???????????? ????????? ??????????????????");
        }

        user.updateUserRole(Role.ROLE_SELLER);

        Seller seller = Seller.builder()
                .userId(user.getId())
                .isLawAgree(sellerAgreeRequest.getIsLawAgree())
                .isSellerAgree(sellerAgreeRequest.getIsSellerAgree())
                .isActivated(true)
                .build();
        sellerRepository.save(seller);

        return true;
    }

    public void qnaAnswerRegister(final QnaAnswerRequest qnaAnswerRequest, final User user) {
        Long qnaId = qnaAnswerRequest.getQnaId();

        final Qna qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new QnaException("Q&A??? ???????????? ????????????"));

        if (qna.getIsAnswered()) {
            throw new AlreadyExistsException("????????? ?????? ???????????????");
        }
        Item item = qna.getItem();
        if (this.sellerCheck(item.getId(), user)) {
            qna.answerUpdate(qnaAnswerRequest.getAnswer());
        } else {
            throw new QnaException("????????? ??? ????????????");
        }
        //QnA ????????? ?????? notification to user
        final QnaNotification qnaNotification = notificationRepository.findByQnaId(qnaId);
        if (qnaNotification != null) {
            qnaNotification.answerUpdate(qnaAnswerRequest.getAnswer());

            MeNotification meNotification = MeNotification.builder()
                    .notificationType(NotificationType.QNA_ANSWER_TO_USER)
                    .originalMessage(qnaNotification.getMessage())
                    .sender(qnaNotification.getReceiver())
                    .receiver(qnaNotification.getSender())
                    .message(qnaAnswerRequest.getAnswer())
                    .item(item)
                    .build();

            meNotificationRepository.save(meNotification);
        }
    }

    public void temporarySave(final ApiWriteItemRequest apiWriteItemRequest, final List<OptionValue> optionValues, final User user) {
        Seller seller = sellerRepository.findByUserIdAndIsActivatedTrue(user.getId())
                .orElseThrow(() -> new SellerNotFoundException("???????????? ????????????"));

        final long count = optionValues.stream().filter(OptionValue::getIsOptionMainItem).count();
        if (count != 1) {
            throw new NotValidException("?????? ????????? ????????? ????????? ??? ????????????");
        }

        for (OptionValue optionValue : optionValues) {
            ItemTemporary itemTemporary = ItemTemporary.builder()
                    .brandName(apiWriteItemRequest.getItemBrandName())
                    .name(apiWriteItemRequest.getItemName())
                    .description(apiWriteItemRequest.getDescription())
                    .originalPrice(optionValue.getOptionOriginalPrice())
                    .salePrice(optionValue.getOptionSalePrice())
                    .stock(optionValue.getOptionStock())
                    .option1(optionValue.getOption1())
                    .option2(optionValue.getOption2())
                    .isOptionMainItem(optionValue.getIsOptionMainItem())
                    .seller(seller)
                    .build();

            itemTemporaryRepository.save(itemTemporary);
        }
    }

    public List<ItemTemporaryResponse> retrieveItemTemporaries(final User user) {
        Seller seller = sellerRepository.findByUserIdAndIsActivatedTrue(user.getId())
                .orElseThrow(() -> new SellerNotFoundException("???????????? ????????????"));

        return itemTemporaryRepository.findAllBySellerId(seller.getId()).stream()
                .map(itemTemporary -> ItemTemporaryResponse.builder()
                        .brandName(itemTemporary.getBrandName())
                        .name(itemTemporary.getName())
                        .originalPrice(itemTemporary.getOriginalPrice())
                        .description(itemTemporary.getDescription())
                        .stock(itemTemporary.getStock())
                        .salePrice(itemTemporary.getSalePrice())
                        .option1(itemTemporary.getOption1())
                        .option2(itemTemporary.getOption2())
                        .isOptionMainItem(itemTemporary.getIsOptionMainItem())
                        .build()).collect(Collectors.toList());
    }

    public Boolean sellerCheck(final Long itemId, final User user) {
        if (user == null) {
            return false;
        }
        Long sellerId = sellerRepository.findByUserIdAndIsActivatedTrue(user.getId())
                .map(Seller::getId)
                .orElse(-1L);
        Long sellerId2 = itemRepository.findById(itemId)
                .map(Item::getSeller)
                .map(Seller::getId)
                .orElse(-2L);

        return Objects.equals(sellerId, sellerId2);
    }

    public void itemDelete(final User user, final Long itemId) {
        if (!this.sellerCheck(itemId, user)) {
            throw new SellerNotFoundException("???????????? ????????????");
        }
        itemRepository.deleteById(itemId);
        cartRepository.deleteByUserIdAndItemId(user.getId(), itemId);
    }

    public void sellerDefaultSettingSave(SellerDefaultSettingsRequest request, User user) {
        Seller seller = sellerRepository.findByUserIdAndIsActivatedTrue(user.getId())
                .orElseThrow(() -> new SellerNotFoundException("???????????? ????????????"));

        if (sellerRepository.existsByCompanyName(request.getCompanyName())) {
            if (!seller.getCompanyName().equals(request.getCompanyName()))
                throw new AlreadyExistsException("?????? ?????? ?????? ??????????????????");
        }

        final Address itemReleaseAddress = Address.builder()
                .address(request.getItemReleaseAddress())
                .detailAddress(request.getItemReleaseDetailAddress())
                .extraAddress(request.getItemReleaseExtraAddress())
                .zipcode(request.getItemReleaseZipcode())
                .build();

        if (Objects.equals(request.getItemReturnAddress(), "") || Objects.equals(request.getItemReturnDetailAddress(), "") || Objects.equals(request.getItemReturnZipcode(), "")) {
            seller.sellerDefaultUpdate(
                    request.getCompanyName(),
                    request.getContactNumber(),
                    itemReleaseAddress,
                    itemReleaseAddress,
                    request.getShippingFeeDefault(),
                    request.getReturnShippingFeeDefault(),
                    request.getShippingFeeFreePolicy(),
                    request.getDefaultDeliveryCompany());
        } else {
            final Address itemReturnAddress = Address.builder()
                    .address(request.getItemReturnAddress())
                    .detailAddress(request.getItemReturnDetailAddress())
                    .extraAddress(request.getItemReturnExtraAddress())
                    .zipcode(request.getItemReturnZipcode())
                    .build();

            seller.sellerDefaultUpdate(
                    request.getCompanyName(),
                    request.getContactNumber(),
                    itemReleaseAddress,
                    itemReturnAddress,
                    request.getShippingFeeDefault(),
                    request.getReturnShippingFeeDefault(),
                    request.getShippingFeeFreePolicy(),
                    request.getDefaultDeliveryCompany());
        }
    }

    @Transactional(readOnly = true)
    public SellerDefaultSettingsResponse sellerDefaultSettings(User user) {
        Seller seller = sellerRepository.findByUserIdAndIsActivatedTrue(user.getId())
                .orElseThrow(() -> new SellerNotFoundException("???????????? ????????????"));

        return SellerDefaultSettingsResponse.builder()
                .companyName(seller.getCompanyName())
                .contactNumber(seller.getContactNumber())
                .shippingFeeDefault(seller.getShippingFeeDefault())
                .shippingFeeFreePolicy(seller.getShippingFeeFreePolicy())

                .itemReleaseZipcode(seller.getItemReleaseAddress().getZipcode())
                .itemReleaseAddress(seller.getItemReleaseAddress().getAddress())
                .itemReleaseDetailAddress(seller.getItemReleaseAddress().getDetailAddress())
                .itemReleaseExtraAddress(seller.getItemReleaseAddress().getExtraAddress())

                .itemReturnZipcode(seller.getItemReturnAddress().getZipcode())
                .itemReturnAddress(seller.getItemReturnAddress().getAddress())
                .itemReturnDetailAddress(seller.getItemReturnAddress().getDetailAddress())
                .itemReturnExtraAddress(seller.getItemReturnAddress().getExtraAddress())

                .defaultDeliveryCompany(seller.getDefaultDeliveryCompany())
                .returnShippingFeeDefault(seller.getReturnShippingFeeDefault())
                .build();
    }

    public boolean sellerDefaultSettingCheck(final User user) {
        Seller seller = sellerRepository.findByUserIdAndIsActivatedTrue(user.getId())
                .orElseThrow(() -> new SellerNotFoundException("???????????? ????????????"));

        return seller.getCompanyName() != null;
    }

    public List<RecentPaymentPerSellerResponse> getSellerRecentOrders(final User user, Pageable pageable) {
        Seller seller = sellerRepository.findByUserIdAndIsActivatedTrue(user.getId())
                .orElseThrow(() -> new SellerNotFoundException("???????????? ????????????"));

        final List<PaymentPerSeller> recentPayments = paymentPerSellerRepository.findBySellerIdOrderByCreatedDateDesc(seller.getId(), pageable);

        List<RecentPaymentPerSellerResponse> recentPaymentPerSellerResponses = new ArrayList<>();
        for (PaymentPerSeller paymentPerSeller : recentPayments) {
            final Integer itemTotalQuantityPerSeller = paymentPerSeller.getItemTotalQuantityPerSeller();

            //????????? ???????????? ??????
            final PaymentPerSellerResponse paymentPerSellerResponse = PaymentPerSellerResponse.builder()
                    .paymentPerSellerId(paymentPerSeller.getId())
                    .itemShippingFeePerSeller(paymentPerSeller.getItemShippingFeePerSeller())
                    .itemTotalQuantityPerSeller(itemTotalQuantityPerSeller)
                    .itemTotalPricePerSeller(paymentPerSeller.getItemTotalPricePerSeller())
                    .build();

            //?????? ??? ?????? ?????? ????????? ????????? ??????.
            final List<OrderItem> orderItems = orderItemRepository.findByOrderIdAndSellerId(paymentPerSeller.getOrder().getId(), seller.getId());
            final OrderItem mostExpensiveOneAtOrder = orderItems.stream().max(Comparator.comparingLong(OrderItem::getPriceAtPurchase))
                    .orElseThrow(() -> new ItemNotFoundException("????????? ???????????? ????????????"));

            String mainImage = fileHandler.getStringImage(imageRepository.findByImageRelationAndId(ImageRelation.ITEM_MAIN, mostExpensiveOneAtOrder.getMainImageId()));
            String mostExpensiveOne = mostExpensiveOneAtOrder.getItem().getName();
            String name = itemTotalQuantityPerSeller == 1 ? mostExpensiveOne : mostExpensiveOne + " ??? " + (itemTotalQuantityPerSeller - 1) + "???";

            //????????? ?????? ??????
            final Payment payment = paymentPerSeller.getPayment();
            RecentPaymentPerSellerSimpleResponse recentPaymentPerSellerSimpleResponse = RecentPaymentPerSellerSimpleResponse.builder()
                    .orderId(paymentPerSeller.getOrder().getId())
                    .orderDate(paymentPerSeller.getOrder().getCreatedDate())
                    .pg(payment.getPg().getName())
                    .payMethod(payment.getPayMethod().getName())
                    .merchantUid(payment.getMerchantUid())
                    .mainImage(mainImage)
                    .name(name)
                    .build();

            //????????? ???????????? ?????? + ?????? ??????
            RecentPaymentPerSellerResponse recentPaymentPerSellerResponse = RecentPaymentPerSellerResponse.builder()
                    .recentPaymentPerSellerSimpleResponse(recentPaymentPerSellerSimpleResponse)
                    .paymentPerSellerResponse(paymentPerSellerResponse)
                    .build();

            recentPaymentPerSellerResponses.add(recentPaymentPerSellerResponse);
        }
        return recentPaymentPerSellerResponses;
    }

    public OrderDetailResponse showOrderDetail(final Long orderId, final User user) {
        Seller seller = sellerRepository.findByUserIdAndIsActivatedTrue(user.getId())
                .orElseThrow(() -> new SellerNotFoundException("???????????? ????????????"));
        final Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("???????????? ????????? ????????????"));
        boolean isAllItemTrackingNumberIssued = true;

        LocalDateTime orderDate = order.getCreatedDate();
        //????????????
        final List<OrderItem> orderItems = orderItemRepository.findByOrderIdAndSellerId(orderId, seller.getId());

        List<OrderItemResponse> orderItemResponses = new ArrayList<>();
        for (OrderItem orderItem : orderItems) {
            boolean isTrackingStarted = false;

            final DeliveryStatus deliveryStatus = orderItem.getOrderDelivery().getDeliveryStatus();

            if (!deliveryStatus.equals(DeliveryStatus.PAYMENT_DONE)) {
                isTrackingStarted = true;
            } else {
                isAllItemTrackingNumberIssued = false;
            }
            final Image image = imageRepository.findByImageRelationAndId(ImageRelation.ITEM_MAIN, orderItem.getMainImageId());
            final String mainImage = fileHandler.getStringImage(image);

            OrderItemResponse orderItemResponse = OrderItemResponse.builder()
                    .orderItemId(orderItem.getId())
                    .orderDate(orderDate)
                    .itemName(orderItem.getItem().getName())
                    .mainImage(mainImage)
                    .sellerCompanyName(orderItem.getSeller().getCompanyName())
                    .orderItemId(orderItem.getId())
                    .itemPrice(orderItem.getPriceAtPurchase())
                    .quantity(orderItem.getQuantity())
                    .deliveryStatus(deliveryStatus.getValue())
                    .isTrackingStarted(isTrackingStarted)
                    .build();

            orderItemResponses.add(orderItemResponse);
        }

        final PaymentPerSeller paymentPerSeller = paymentPerSellerRepository.findByOrderIdAndSellerId(orderId, seller.getId());
        final Payment payment = order.getPayment();
        PaymentDetailResponse paymentDetailResponse = PaymentDetailResponse.builder()
                .pg(payment.getPg().getName())
                .payMethod(payment.getPayMethod().getName())

                .paymentTotalPrice(paymentPerSeller.getItemTotalPricePerSeller())
                .paymentTotalShippingFee(paymentPerSeller.getItemShippingFeePerSeller())

                .buyerName(payment.getBuyerName())
                .buyerAddr("(" + payment.getBuyerPostcode() + ") " + payment.getBuyerAddr())
                .buyerEmail(payment.getBuyerEmail())
                .buyerTel(payment.getBuyerTel())

                .receiverName(payment.getReceiverInfo().getReceiverName())
                .receiverAddress("(" + payment.getReceiverInfo().getReceiverPostcode() + ") " + payment.getReceiverInfo().getReceiverAddress())
                .receiverEmail(payment.getReceiverInfo().getReceiverEmail())
                .receiverPhoneNumber(payment.getReceiverInfo().getReceiverPhoneNumber())
                .isAllItemTrackingNumberIssued(isAllItemTrackingNumberIssued)
                .build();

        return OrderDetailResponse.builder()
                .orderItemResponses(orderItemResponses)
                .paymentDetailResponse(paymentDetailResponse)
                .build();
    }

    public SellerBankResponse getSellerBalance(final User user) {
        Seller seller = sellerRepository.findByUserIdAndIsActivatedTrue(user.getId())
                .orElseThrow(() -> new SellerNotFoundException("???????????? ????????????"));

        final List<SellerBankAccountHistory> transactionHistories = sellerBankAccountHistoryRepository.findTop20BySellerIdOrderByCreatedDateDesc(seller.getId());

        List<SellerBankAccountHistoryResponse> sellerBankAccountHistoryResponses = new ArrayList<>();
        for (SellerBankAccountHistory sellerBankAccountHistory : transactionHistories) {
            sellerBankAccountHistoryResponses.add(SellerBankAccountHistoryResponse.builder()
                    .transactionMoney(sellerBankAccountHistory.getTransactionMoney())
                    .transactionType(sellerBankAccountHistory.getTransactionType().getValue())
                    .build());
        }

        return SellerBankResponse.builder()
                .sellerId(seller.getId())
                .bankAccount(seller.getBankAccount())
                .sellerBankAccountHistoryResponses(sellerBankAccountHistoryResponses)
                .build();
    }

    public List<StatisticsResponse> getStatisticsByDay(final User user) {
        Seller seller = sellerRepository.findByUserIdAndIsActivatedTrue(user.getId())
                .orElseThrow(() -> new SellerNotFoundException("???????????? ????????????"));

        List<StatisticsResponse> statisticsResponses = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDateTime startDatetime = LocalDateTime.of(LocalDate.now().minusDays(i), LocalTime.of(0, 0, 0));
            LocalDateTime endDatetime = LocalDateTime.of(LocalDate.now().minusDays(i), LocalTime.of(23, 59, 59));

            final List<PaymentPerSeller> perDay = paymentPerSellerRepository.findBySellerIdAndCreatedDateBetween(seller.getId(), startDatetime, endDatetime);
            final long totalPricePerDay = perDay.stream().mapToLong(PaymentPerSeller::getItemTotalPricePerSeller).sum();
            final long totalQuantityPerDay = perDay.stream().mapToLong(PaymentPerSeller::getItemTotalQuantityPerSeller).sum();
            final long totalOrderPerDay = perDay.size();

            final StatisticsResponse statisticsResponse = StatisticsResponse.builder()
                    .date(startDatetime)
                    .totalPricePerDay(totalPricePerDay)
                    .totalQuantityPerDay(totalQuantityPerDay)
                    .totalOrderPerDay(totalOrderPerDay)
                    .build();
            statisticsResponses.add(statisticsResponse);
        }
        return statisticsResponses;
    }

    public void getItemRecentReviews(final User user, final Pageable pageable) {
        Seller seller = sellerRepository.findByUserIdAndIsActivatedTrue(user.getId())
                .orElseThrow(() -> new SellerNotFoundException("???????????? ????????????"));

        //Seller??? ???????????? Review??? ??????????????? ????????? ??????????????
        //Review??? QnA??? ????????? ?????? ??????????????? ???????????? ???????????? ????????? ??????. ?????? ????????? ???????????? ????????? ?????? ?????? ??????.
    }

}
