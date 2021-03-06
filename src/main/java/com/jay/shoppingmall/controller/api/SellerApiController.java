package com.jay.shoppingmall.controller.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jay.shoppingmall.common.CurrentUser;
import com.jay.shoppingmall.common.model.OptionValue;
import com.jay.shoppingmall.domain.user.User;
import com.jay.shoppingmall.dto.request.ApiWriteItemRequest;
import com.jay.shoppingmall.dto.request.SellerAgreeRequest;
import com.jay.shoppingmall.dto.request.SellerDefaultSettingsRequest;
import com.jay.shoppingmall.dto.request.WriteItemRequest;
import com.jay.shoppingmall.exception.exceptions.AgreeException;
import com.jay.shoppingmall.exception.exceptions.NotValidException;
import com.jay.shoppingmall.exception.exceptions.UserNotFoundException;
import com.jay.shoppingmall.service.NotificationService;
import com.jay.shoppingmall.service.SellerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/seller")
@PreAuthorize("hasRole('SELLER')")
public class SellerApiController {

    private final SellerService sellerService;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    private final AuthenticationManager authenticationManager;

    @PostMapping(value = "/write", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> sellerOptionItemWrite(@Valid @RequestPart ApiWriteItemRequest apiWriteItemRequest,
                                             @RequestParam("mainImage") MultipartFile file,
                                             @RequestParam(value = "descriptionImage", required = false) List<MultipartFile> files,
                                             @CurrentUser User user) {
        if (apiWriteItemRequest.getDescription().length() > 200) {
            throw new NotValidException("????????? 200??????????????? ??????????????????");
        }
        if (file.isEmpty()) {
            throw new NotValidException("?????? ????????? ??????????????????");
        }
        if ((file.getSize() / (1024 * 1024)) >= 5) {
            throw new NotValidException("?????? ?????? ????????? 5MB??? ?????? ??? ????????????");
        }
        if (files != null && files.size() > 5) {
            throw new NotValidException("????????? ?????? ????????? 5???????????? ???????????? ???????????????");
        }
        final List<OptionValue> optionValues = objectMapper.convertValue(apiWriteItemRequest.getOptionArray(), new TypeReference<List<OptionValue>>() {
        });
        Long itemId = sellerService.writeOptionItem(apiWriteItemRequest, optionValues, file, files, user);

        return ResponseEntity.ok(itemId);
    }
    @PostMapping(value = "/write-no-option", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> sellerOneItemWrite(@Valid @RequestPart WriteItemRequest writeItemRequest,
                                             @RequestParam("mainImage") MultipartFile file,
                                             @RequestParam(value = "descriptionImage", required = false) List<MultipartFile> files,
                                             @CurrentUser User user) {
        if (writeItemRequest.getDescription().length() > 200) {
            throw new NotValidException("????????? 200??????????????? ??????????????????");
        }
        if (file.isEmpty()) {
            throw new NotValidException("?????? ????????? ??????????????????");
        }
        if ((file.getSize() / (1024 * 1024)) >= 5) {
            throw new NotValidException("?????? ?????? ????????? 5MB??? ?????? ??? ????????????");
        }
//        if (files != null && files.size() > 5) {
//            throw new NotValidException("????????? ?????? ????????? 5???????????? ???????????? ???????????????");
//        }
        Long itemId = sellerService.writeItem(writeItemRequest, file, files, user);

        return ResponseEntity.ok(itemId);
    }

    @PostMapping("/settings")
    public ResponseEntity<?> sellerSettingsAction(@Valid @RequestBody SellerDefaultSettingsRequest request, @CurrentUser User user) {
        final String trimmedNumber = request.getContactNumber().trim().replace("-", "");
        request.setContactNumber(trimmedNumber);
        if (!request.getContactNumber().matches("^0([1|2])([0|1|6|7|8|9])?-?([0-9]{3,4})-?([0-9]{4})$")) {
            throw new NotValidException("??????????????? ????????? ?????? ????????????");
        }
        sellerService.sellerDefaultSettingSave(request, user);

        return ResponseEntity.ok(null);
    }

    @PostMapping("/agree")
    public ResponseEntity<?> agreeCheck(@Valid @RequestBody SellerAgreeRequest sellerAgreeRequest, @CurrentUser User user, HttpServletRequest request) {
        if (user == null) {
            throw new UserNotFoundException("????????? ???????????????");
        }

        //TODO ?????????
        if (user.getEmail().equals("demo@user")) {
            throw new UserNotFoundException("????????? ????????? ??????????????????..!");
        }

        if (sellerAgreeRequest.getIsSellerAgree() == null || sellerAgreeRequest.getIsLawAgree() == null) {
            throw new AgreeException("????????? ???????????????");
        }
        if (!sellerAgreeRequest.getIsSellerAgree() || !sellerAgreeRequest.getIsLawAgree()) {
            throw new AgreeException("?????? ????????? ??????????????? ?????????");
        }

        if (!sellerService.sellerAgreeCheck(sellerAgreeRequest, user)) {
            throw new AgreeException("????????? ???????????????");
        }
        Authentication oldAuthentication = SecurityContextHolder.getContext().getAuthentication();
        Authentication newAuthentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(oldAuthentication.getPrincipal(), oldAuthentication.getCredentials()));
        SecurityContextHolder.getContext().setAuthentication(newAuthentication);

        return ResponseEntity.ok().body(true);
    }
}
