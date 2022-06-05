package com.jay.shoppingmall.dto.request;

import com.jay.shoppingmall.domain.image.Image;
import com.jay.shoppingmall.domain.item.Item;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class WriteItemRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String description;

//    private List<String> optionArray;

    private List<MultipartFile> image;

    @NotNull
    private MultipartFile mainImage;

    private Long originalPrice;

    @NotNull
    private Long salePrice;

    @NotNull
    private Integer stock;
}
