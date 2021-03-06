package com.jay.shoppingmall.demo;

import com.jay.shoppingmall.common.CurrentUser;
import com.jay.shoppingmall.domain.user.Role;
import com.jay.shoppingmall.domain.user.User;
import com.jay.shoppingmall.dto.request.UserValidationRequest;
import com.jay.shoppingmall.dto.response.order.OrderItemResponse;
import com.jay.shoppingmall.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
//TODO 데모용
public class DemoController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final DemoService demoService;

    @GetMapping("/seller/delivery-status")
    public String demoDeliveryStatus(@CurrentUser User user, Pageable pageable, Model model) {
        final List<OrderItemResponse> orderItemResponses = demoService.deliveryChange(user, pageable);
        model.addAttribute("orderItemResponses", orderItemResponses);

        return "demo/delivery";
    }
    @PostMapping("/seller/delivery-status")
    public String demoDeliveryStatusAction(@RequestBody Map<String, Long> map, @CurrentUser User user) {
        final Long orderItemId = map.get("orderItemId");
        System.out.println("orderItemId = " + orderItemId);

        demoService.deliveryDone(user, orderItemId);
        return "demo/delivery";
    }

    @PostMapping("/demo/login/user")
    public String demoLogin1(HttpSession session) {
        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                "demo@user", "password123"));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        session.setAttribute("demo", "password123");
        return "redirect:/";
    }

    @PostMapping("/demo/login/seller")
    public String demoLogin2(HttpSession session) {
        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                "demo@seller", "password123"));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        session.setAttribute("demo", "password123");
        return "redirect:/seller";
    }

    @PostMapping("/demo/login/seller2")
    public String demoLogin4(HttpSession session) {
        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                "demo@seller2", "password123"));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        session.setAttribute("demo", "password123");
        return "redirect:/seller";
    }

    @PostMapping("/demo/login/admin")
    public String demoLogin3() {
        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                "demo@admin", "specialPassWorD123"));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return "redirect:/admin";
    }


    @PostMapping("/random-user-generator")
    public String randomUserGenerator() {
        String email = UUID.randomUUID() + "@" + UUID.randomUUID();
        String password = UUID.randomUUID().toString();

        UserValidationRequest userValidationRequest = UserValidationRequest.builder()
                .email(email)
                .password(password)
                .repeatPassword(password)
                .build();

        authService.userRegistration(userValidationRequest);

        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userValidationRequest.getEmail(), userValidationRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return "redirect:/";
    }
}
