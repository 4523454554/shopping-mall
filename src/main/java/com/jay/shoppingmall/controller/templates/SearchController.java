package com.jay.shoppingmall.controller.templates;

import com.jay.shoppingmall.dto.response.ItemResponse;
import com.jay.shoppingmall.dto.response.SearchResponse;
import com.jay.shoppingmall.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/search")
@Controller
public class SearchController {

    private final ItemService itemService;

    @GetMapping
    public String searchByKeyword(@RequestParam(value = "q", required = false) String keyword, Model model) {
        if (keyword == null || keyword.equals("")) {
            return "redirect:/";
        }
        final List<ItemResponse> itemResponses = itemService.searchItemsByKeyword(keyword);
        model.addAttribute("items", itemResponses);
        model.addAttribute("result", keyword);
        return "/item/search-result";
    }
}