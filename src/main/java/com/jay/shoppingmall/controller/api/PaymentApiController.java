package com.jay.shoppingmall.controller.api;

import com.google.common.base.Splitter;
import com.jay.shoppingmall.common.CurrentUser;
import com.jay.shoppingmall.domain.user.User;
import com.jay.shoppingmall.dto.request.PaymentRequest;
import com.jay.shoppingmall.dto.response.order.payment.PaymentResponse;
import com.jay.shoppingmall.dto.response.order.payment.PaymentDetailResponse;
import com.jay.shoppingmall.exception.exceptions.UserNotFoundException;
import com.jay.shoppingmall.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.json.JSONException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.io.IOException;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/payment")
public class PaymentApiController {

    private final PaymentService paymentService;

    @PostMapping("/record")
    public ResponseEntity<?> paymentRecordGenerateBeforePg(@Valid @RequestBody PaymentRequest paymentRequest, @CurrentUser User user) {
        PaymentResponse paymentResponse = paymentService.paymentRecordGenerateBeforePg(paymentRequest, user);

        return ResponseEntity.ok(paymentResponse);
    }

    @PostMapping("/complete")
    public ResponseEntity<?> paymentResult(@RequestBody String uid, @CurrentUser User user) throws JSONException, IOException {
        if (user == null) {
            throw new UserNotFoundException("잘못된 접근입니다");
        }
        Map<String,String> queryParameters = Splitter
                .on("&")
                .withKeyValueSeparator("=")
                .split(uid);

        String imp_uid = queryParameters.get("imp_uid");
        String merchant_uid = queryParameters.get("merchant_uid");

        PaymentDetailResponse paymentDetailResponse = paymentService.paymentTotal(imp_uid, merchant_uid, user);

        return ResponseEntity.ok(paymentDetailResponse);
    }

}
