package com.jay.shoppingmall.dto;

import com.jay.shoppingmall.domain.image.Image;
import com.jay.shoppingmall.domain.item.Item;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class WriteItemRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String description;

    private List<MultipartFile> image;

    @NotNull
    private Integer price;

    @NotNull
    private Integer stock;

    public Item toEntity() {
        return Item.builder()
                .name(name)
                .description(description)
                .price(price)
                .stock(stock)
                .build();
    }
}