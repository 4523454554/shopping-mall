package com.jay.shoppingmall.domain.seller;

import com.jay.shoppingmall.domain.user.model.Address;
import com.jay.shoppingmall.exception.exceptions.AgreeException;
import lombok.*;

import javax.persistence.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Seller {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Long userId;

    private Long bankAccount;

    @Column(unique = true)
    private String companyName;

    private String contactNumber;

    @AttributeOverrides({
            @AttributeOverride(name = "zipcode", column = @Column(name = "item_release_zipcode")),
            @AttributeOverride(name = "address", column = @Column(name = "item_release_address")),
            @AttributeOverride(name = "detailAddress", column = @Column(name = "item_release_detailAddress")),
            @AttributeOverride(name = "extraAddress", column = @Column(name = "item_release_extraAddress"))
    })
    @Embedded
    private Address itemReleaseAddress;

    @AttributeOverrides({
            @AttributeOverride(name = "zipcode", column = @Column(name = "item_return_zipcode")),
            @AttributeOverride(name = "address", column = @Column(name = "item_return_address")),
            @AttributeOverride(name = "detailAddress", column = @Column(name = "item_return_detailAddress")),
            @AttributeOverride(name = "extraAddress", column = @Column(name = "item_return_extraAddress"))
    })
    @Embedded
    private Address itemReturnAddress;

    private Integer shippingFeeDefault;

    private Integer returnShippingFeeDefault;

    private Integer shippingFeeFreePolicy;

    private String defaultDeliveryCompany;

    private Boolean isSellerAgree;

    private Boolean isLawAgree;

    private Boolean isActivated;

    public void sellerDefaultUpdate(final String companyName, final String contactNumber, final Address itemReleaseAddress, final Address itemReturnAddress, final Integer shippingFeeDefault, final Integer returnShippingFeeDefault, final Integer shippingFeeFreePolicy, final String defaultDeliveryCompany) {
        this.companyName = companyName;
        this.contactNumber = contactNumber;
        this.itemReleaseAddress = itemReleaseAddress;
        this.itemReturnAddress = itemReturnAddress;
        this.shippingFeeDefault = shippingFeeDefault;
        this.returnShippingFeeDefault = returnShippingFeeDefault;
        this.shippingFeeFreePolicy = shippingFeeFreePolicy;
        this.defaultDeliveryCompany = defaultDeliveryCompany;
    }

    public void sellerBankAccountUp(final Long bankAccount) {
        if (this.bankAccount == null) {
            this.bankAccount = 0L;
        }
        this.bankAccount += bankAccount;
    }

    @PrePersist
    public void prePersist() {
        this.bankAccount = this.bankAccount == null ? 0L : this.bankAccount;
    }

}
