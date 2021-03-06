package com.jay.shoppingmall.service;

import com.jay.shoppingmall.domain.image.Image;
import com.jay.shoppingmall.domain.image.ImageRelation;
import com.jay.shoppingmall.domain.image.ImageRepository;
import com.jay.shoppingmall.domain.item.Item;
import com.jay.shoppingmall.domain.item.ItemRepository;
import com.jay.shoppingmall.domain.model.page.CustomPage;
import com.jay.shoppingmall.domain.model.page.PageDto;
import com.jay.shoppingmall.domain.order.DeliveryStatus;
import com.jay.shoppingmall.domain.order.order_item.OrderItem;
import com.jay.shoppingmall.domain.order.order_item.OrderItemRepository;
import com.jay.shoppingmall.domain.point.Point;
import com.jay.shoppingmall.domain.point.PointRepository;
import com.jay.shoppingmall.domain.point.point_history.PointHistory;
import com.jay.shoppingmall.domain.point.point_history.PointHistoryRepository;
import com.jay.shoppingmall.domain.point.point_history.model.PointPercentage;
import com.jay.shoppingmall.domain.point.point_history.model.PointStatus;
import com.jay.shoppingmall.domain.point.point_history.model.PointType;
import com.jay.shoppingmall.domain.review.Review;
import com.jay.shoppingmall.domain.review.ReviewRepository;
import com.jay.shoppingmall.domain.review.Star;
import com.jay.shoppingmall.domain.user.User;
import com.jay.shoppingmall.dto.request.ReviewWriteRequest;
import com.jay.shoppingmall.dto.response.review.OrderItemForReviewResponse;
import com.jay.shoppingmall.dto.response.review.ReviewResponse;
import com.jay.shoppingmall.dto.response.review.ReviewStarCalculationResponse;
import com.jay.shoppingmall.exception.exceptions.ItemNotFoundException;
import com.jay.shoppingmall.exception.exceptions.ReviewException;
import com.jay.shoppingmall.service.common.CommonService;
import com.jay.shoppingmall.service.handler.FileHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final PointRepository pointRepository;
    private final ImageRepository imageRepository;
    private final ItemRepository itemRepository;

    private final FileHandler fileHandler;
    private final PaymentService paymentService;
    private final CommonService commonService;

    @Transactional(readOnly = true)
    public OrderItemForReviewResponse orderItemRequestForReview(final Long orderItemId, final User user) {
        final OrderItem orderItem = reviewAvailableCheck(orderItemId, user);

        final String name = orderItem.getItem().getName();
        final Long priceAtPurchase = orderItem.getPriceAtPurchase();
        final Integer quantity = orderItem.getQuantity();
        final int pointOnlyText = (int) ((priceAtPurchase * quantity) * PointPercentage.ONLY_TEXT.getPercentage());
        final int pointWithPicture = (int) ((priceAtPurchase * quantity) * PointPercentage.TEXT_AND_PICTURE.getPercentage());

        return OrderItemForReviewResponse.builder()
                .orderItemId(orderItem.getId())
                .name(name)
                .pointOnlyText(pointOnlyText)
                .pointWithPicture(pointWithPicture)
                .build();
    }

    public ReviewStarCalculationResponse reviewStarCalculation(Item item) {

        double reviewAverageRating = item.getReviewAverageRating() == null ? 0.0 : item.getReviewAverageRating();
        double fullStar = Math.floor(reviewAverageRating);
        double halfStar = Math.round((reviewAverageRating - fullStar) * 100) / 100.0;
        double emptyStar = 5 - Math.ceil(reviewAverageRating);

        if (halfStar < 0.4 && halfStar > 0.0) {
            halfStar = 0.0;
            emptyStar++;
        } else if (halfStar >= 0.8) {
            halfStar = 0.0;
            fullStar++;
        } else if (halfStar >= 0.4) {
            halfStar = 1.0;
        }
        return ReviewStarCalculationResponse.builder()
                .reviewCount(item.getReviewCount() == null ? 0 : item.getReviewCount())
                .reviewAverageRating(reviewAverageRating)
                .fullStar(fullStar)
                .halfStar(halfStar)
                .emptyStar(emptyStar)
                .build();
    }


    public ReviewResponse reviewWrite(final ReviewWriteRequest request, final User user, final List<MultipartFile> files) {
        final OrderItem orderItem = reviewAvailableCheck(request.getOrderItemId(), user);

        double pointPercentage = PointPercentage.ONLY_TEXT.getPercentage();
        if (files != null) {
            pointPercentage = PointPercentage.TEXT_AND_PICTURE.getPercentage();
        }
        pointCalculation(user, orderItem, pointPercentage);

        final Star star = Star.getByValue(request.getStar());

        //Truncated incorrect DOUBLE value ?????? ?????? ?????? ??????
        final String text = request.getText();

        Review review = Review.builder()
                .star(star)
                .text(text)
                .user(user)
                .item(orderItem.getItem())
                .orderItem(orderItem)
                .build();
        reviewRepository.save(review);

        Long foreignId = review.getId();
        ImageRelation imageRelation = ImageRelation.REVIEW;

        List<String> stringImages = fileHandler.getStringImages(files, imageRelation, foreignId);

        final Item item = orderItem.getItem();
        item.reviewAverageCalculation((double) star.value());

        //?????? ??????
        paymentService.moneyTransactionToSeller(orderItem);

        return ReviewResponse.builder()
                .reviewId(review.getId())
                .reviewImages(stringImages)
                .star(review.getStar().value())
                .build();
    }

    @Transactional(readOnly = true)
    public PageDto getItemReviews(Long itemId, Pageable pageable) {
        final Page<Review> reviewPage = reviewRepository.findAllByItemIdOrderByCreatedDateDesc(itemId, pageable);
        CustomPage customPage = new CustomPage(reviewPage, "review");
        final List<ReviewResponse> reviewResponses = getReviewResponses(reviewPage.getContent());

        for (ReviewResponse reviewResponse : reviewResponses) {
            final List<Image> images = imageRepository.findAllByImageRelationAndForeignId(ImageRelation.REVIEW, reviewResponse.getReviewId());

            List<String> reviewImages = new ArrayList<>();
            for (Image image : images) {
                final String stringImage = fileHandler.getStringImage(image);
                reviewImages.add(stringImage);
            }
            reviewResponse.setReviewImages(reviewImages);
        }

        return PageDto.builder()
                .customPage(customPage)
                .content(reviewResponses)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getMyRecentReviews(final User user) {

        final List<Review> recent10Reviews = reviewRepository.findFirst10ByUserIdOrderByCreatedDateDesc(user.getId());
        final List<ReviewResponse> reviewResponses = getReviewResponses(recent10Reviews);

        for (ReviewResponse reviewResponse : reviewResponses) {
            final String itemImage = fileHandler.getStringImage(imageRepository.findByImageRelationAndForeignId(ImageRelation.ITEM_MAIN, reviewResponse.getItemId()));
            reviewResponse.setItemImage(itemImage);
        }
        return reviewResponses;
    }

    private OrderItem reviewAvailableCheck(final Long orderItemId, final User user) {
        if (reviewRepository.findByUserIdAndOrderItemId(user.getId(), orderItemId).isPresent()) {
            throw new ReviewException("????????? ?????? ?????????????????????");
        }
        final OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new ItemNotFoundException("????????? ???????????? ????????????"));
        if (!orderItem.getOrder().getUser().getId().equals(user.getId())) {
            throw new ReviewException("?????? ????????? ??????????????????");
        }
        if (!orderItem.getOrderDelivery().getDeliveryStatus().equals(DeliveryStatus.DELIVERED)) {
            throw new ReviewException("????????? ???????????? ???????????????");
        }
        return orderItem;
    }


//    private List<String> getStringImages(final List<MultipartFile> files, final ImageRelation imageRelation, final Long foreignId) {
//        List<String> stringImages = new ArrayList<>();
//        if (files != null) {
//            for (MultipartFile file : files) {
//                final Image image = fileHandler.parseFilesInfo(file, imageRelation, foreignId);
//                imageRepository.save(image);
//                stringImages.add(fileHandler.getStringImage(image));
//            }
//        }
//        return stringImages;
//    }

    private void pointCalculation(final User user, final OrderItem orderItem, final double pointPercentage) {
        final Long priceAtPurchase = orderItem.getPriceAtPurchase();
        final Integer quantity = orderItem.getQuantity();
        //double to int safety ?????? ??????.
        final int calculatedPoint = (int) ((priceAtPurchase * quantity) * pointPercentage);

        Point point = pointRepository.findByUserId(user.getId());

        if (point == null) {
            point = Point.builder()
                    .pointNow(0)
                    .user(user)
                    .build();
            pointRepository.save(point);
        }
        point.plusPoint(calculatedPoint);

        PointHistory pointHistory = PointHistory.builder()
                .point(point)
                .pointNumber(calculatedPoint)
                .pointStatus(PointStatus.PAID)
                .pointType(PointType.REVIEW)
                .build();
        pointHistoryRepository.save(pointHistory);
    }

    private List<ReviewResponse> getReviewResponses(final List<Review> reviews) {
        List<ReviewResponse> reviewResponses = new ArrayList<>();
        for (Review review : reviews) {
//            final List<Image> images = imageRepository.findAllByImageRelationAndForeignId(ImageRelation.REVIEW, review.getId());
//
//            List<String> reviewImages = new ArrayList<>();
//            for (Image image : images) {
//                final String stringImage = fileHandler.getStringImage(image);
//                reviewImages.add(stringImage);
//            }

            final Item item = review.getItem();
            final String anonymousName = commonService.anonymousName(review.getUser().getUsername());

            ReviewResponse reviewResponse = ReviewResponse.builder()
                    .itemId(item.getId())
                    .itemName(item.getName())
                    .option1(review.getOrderItem().getItemOption().getOption1())
                    .option2(review.getOrderItem().getItemOption().getOption2())
                    .userName(anonymousName)
                    .reviewId(review.getId())
                    .reviewWrittenDate(review.getCreatedDate())
//                    .reviewImages(reviewImages)
                    .star(review.getStar().value())
                    .text(review.getText())
                    .build();
            reviewResponses.add(reviewResponse);
        }
        return reviewResponses;
    }
}
