package com.jay.shoppingmall.service;

import com.jay.shoppingmall.domain.image.Image;
import com.jay.shoppingmall.domain.image.ImageRepository;
import com.jay.shoppingmall.domain.item.Item;
import com.jay.shoppingmall.domain.item.ItemRepository;
import com.jay.shoppingmall.dto.WriteItemRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminService {

    private final ImageRepository imageRepository;
    private final ItemRepository itemRepository;
    private final FileHandler fileHandler;

    public Long writeItem(WriteItemRequest writeItemRequest, final List<MultipartFile> files) {

//        Item item = Item.builder()
//                .name(writeItemRequest.getName())
//                .description(writeItemRequest.getDescription())
//                .price(writeItemRequest.getPrice())
//                .stock(writeItemRequest.getStock())
//                .build();

        Item item = new Item(writeItemRequest.getName(), writeItemRequest.getDescription(), writeItemRequest.getPrice(), writeItemRequest.getStock());

        List<Image> imageList = fileHandler.parseFileInfo(files);

        if (!imageList.isEmpty()) {
            for (Image image : imageList) {
                item.addImage(imageRepository.save(image));
            }
        }

        return itemRepository.save(item).getId();
    }
}