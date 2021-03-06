package com.jay.shoppingmall.controller.api;

import com.jay.shoppingmall.common.CurrentUser;
import com.jay.shoppingmall.domain.user.User;
import com.jay.shoppingmall.dto.request.CartManipulationRequest;
import com.jay.shoppingmall.dto.response.cart.CartPriceResponse;
import com.jay.shoppingmall.dto.response.cart.CartPriceTotalResponse;
import com.jay.shoppingmall.dto.response.item.ItemOptionResponse;
import com.jay.shoppingmall.exception.exceptions.ItemNotFoundException;
import com.jay.shoppingmall.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.List;

@RequiredArgsConstructor
@RestController
@PreAuthorize("hasRole('USER')")
@RequestMapping("/api/v1/cart")
public class CartApiController {

    private final CartService cartService;

    @PostMapping("/total")
    public ResponseEntity<?> showCartTotal(@CurrentUser User user) {
        Integer totalQuantity = 0;
        if (user == null) {
            return ResponseEntity.ok(totalQuantity);
        }
        totalQuantity = cartService.getTotalQuantity(user);

        return ResponseEntity.ok(totalQuantity);
    }

    //Session으로 구현하여 GET 요청으로 사용.
    @GetMapping("/add")
    public ResponseEntity<?> addToCart(
            HttpSession session,
            @CurrentUser User user) {

        @SuppressWarnings("unchecked")
        final List<ItemOptionResponse> itemOptions = (List<ItemOptionResponse>) session.getAttribute("itemOptions");
        if (itemOptions == null) {
            throw new ItemNotFoundException("옵션을 선택해주세요");
        }
        cartService.addOptionItemsToCart(itemOptions, user);

        return ResponseEntity.ok().body(null);
    }
    @PostMapping("/delete")
    public ResponseEntity<?> deleteCart(@Valid @RequestBody CartManipulationRequest request, @CurrentUser User user) {

        CartPriceResponse cartPriceResponse = cartService.deleteCart(request, user);

        return ResponseEntity.ok().body(cartPriceResponse);
    }
    @PostMapping("/item/price")
    public ResponseEntity<?> cartManipulation(@Valid @RequestBody CartManipulationRequest request, @CurrentUser User user) {
        final CartPriceResponse cartPriceResponse = cartService.changeQuantity(request, user);

        return ResponseEntity.ok(cartPriceResponse);
    }
    @GetMapping("/item/total")
    public ResponseEntity<?> getCartTotals(@CurrentUser User user) {
        final CartPriceTotalResponse cartPriceTotalResponse = cartService.cartPriceTotal(user);

        return ResponseEntity.ok(cartPriceTotalResponse);
    }
    @GetMapping("/select/{boolean}")
    public ResponseEntity<?> getCartSelects(@PathVariable("boolean") String check, @CurrentUser User user) {
        final CartPriceResponse cartPriceResponse = cartService.cartSelectAll(check, user);

        return ResponseEntity.ok(cartPriceResponse);
    }
}
