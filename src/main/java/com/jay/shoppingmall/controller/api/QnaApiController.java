package com.jay.shoppingmall.controller.api;

import com.jay.shoppingmall.controller.common.CurrentUser;
import com.jay.shoppingmall.domain.user.User;
import com.jay.shoppingmall.dto.request.QnaWriteRequest;
import com.jay.shoppingmall.dto.response.QnaResponse;
import com.jay.shoppingmall.exception.exceptions.UserNotFoundException;
import com.jay.shoppingmall.service.QnaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/qna")
public class QnaApiController {

    private final QnaService qnaService;

    @PostMapping("/write")
    public ResponseEntity<?> qnaWrite(@Valid @RequestBody QnaWriteRequest qnaWriteRequest, @CurrentUser User user) {
        if (user == null) {
            throw new UserNotFoundException("잘못된 요청입니다");
        }

        QnaResponse qnaResponse = qnaService.qnaWrite(qnaWriteRequest, user);

        return ResponseEntity.ok().body(qnaResponse);
    }
}
