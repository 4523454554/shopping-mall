package com.jay.shoppingmall.controller.templates;

import com.jay.shoppingmall.controller.common.CurrentUser;
import com.jay.shoppingmall.domain.user.User;
import com.jay.shoppingmall.dto.response.ItemDetailResponse;
import com.jay.shoppingmall.dto.response.ItemResponse;
import com.jay.shoppingmall.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@org.springframework.stereotype.Controller
@RequiredArgsConstructor
public class HomeController {

    private final ItemService itemService;

    @GetMapping(
            value = "/",
            produces = {MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE}
    )
    public String write(Model model, @PageableDefault(size = 5, sort = "id", direction = Sort.Direction.DESC) Pageable pageable, @CurrentUser User user) {
        List<ItemResponse> responses = itemService.itemAll(user, pageable);
        model.addAttribute("items", responses);

        return "home";
    }
}
