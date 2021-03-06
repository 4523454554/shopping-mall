package com.jay.shoppingmall.domain.review;

import com.jay.shoppingmall.common.BaseTimeEntity;
import com.jay.shoppingmall.domain.image.Image;
import com.jay.shoppingmall.domain.item.Item;
import com.jay.shoppingmall.domain.order.order_item.OrderItem;
import com.jay.shoppingmall.domain.user.User;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import javax.validation.constraints.Min;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Where(clause = "is_deleted = 0")
@SQLDelete(sql = "UPDATE review SET is_deleted = 1, deleted_date = NOW() WHERE id = ?")
public class Review extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String text;

    @Enumerated(EnumType.STRING)
    private Star star;

    @Column(columnDefinition = "boolean default 0")
    private final Boolean isDeleted = false;

    private LocalDateTime deletedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;
}
