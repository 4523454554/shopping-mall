package com.jay.shoppingmall.service;

import com.jay.shoppingmall.domain.seller.Seller;
import com.jay.shoppingmall.domain.seller.SellerRepository;
import com.jay.shoppingmall.domain.token.password.PasswordResetTokenRepository;
import com.jay.shoppingmall.domain.user.Role;
import com.jay.shoppingmall.domain.user.User;
import com.jay.shoppingmall.domain.user.UserRepository;
import com.jay.shoppingmall.domain.token.password.PasswordResetToken;
import com.jay.shoppingmall.domain.user.model.Agree;
import com.jay.shoppingmall.dto.request.password.PasswordChangeRequest;
import com.jay.shoppingmall.dto.request.password.PasswordResetRequest;
import com.jay.shoppingmall.dto.request.UserValidationRequest;
import com.jay.shoppingmall.exception.exceptions.PasswordInvalidException;
import com.jay.shoppingmall.exception.exceptions.TokenExpiredException;
import com.jay.shoppingmall.exception.exceptions.UserDuplicatedException;
import com.jay.shoppingmall.exception.exceptions.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public void userRegistration(UserValidationRequest userValidationRequest) {
        String encryptedPassword = passwordEncoder.encode(userValidationRequest.getPassword());

        User user = User.builder()
                .email(userValidationRequest.getEmail())
                .password(encryptedPassword)
                .role(Role.ROLE_USER)
                .build();

        userRepository.save(user);
    }

    public void sellerSignup(final UserValidationRequest userValidationRequest) {
        String password = passwordEncoder.encode(userValidationRequest.getPassword());

        Agree agree = Agree.builder()
                .isMandatoryAgree(true)
                .isMarketingAgree(false)
                .build();

        User user = User.builder()
                .email(userValidationRequest.getEmail())
                .password(password)
                .role(Role.ROLE_SELLER)
                .agree(agree)
                .build();
        userRepository.save(user);

        Seller seller = Seller.builder()
                .isLawAgree(true)
                .isSellerAgree(true)
                .isActivated(true)
                .userId(user.getId())
                .build();
        sellerRepository.save(seller);
    }
    public void passwordTokenSender(PasswordResetRequest passwordResetRequest) {
        User user = userRepository.findByEmail(passwordResetRequest.getEmail())
                .orElseThrow(()-> new UserNotFoundException("?????? ???????????? ???????????? ????????????"));

        String token = UUID.randomUUID().toString();

        mailService.sendMail(user.getEmail(), token);

        PasswordResetToken passwordResetToken = PasswordResetToken.builder()
                .token(token)
                .email(user.getEmail())
                .isExpired(false)
                .build();

        passwordResetTokenRepository.save(passwordResetToken);
    }
    public void passwordTokenValidator(PasswordResetRequest passwordResetRequest) {

        PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByEmailAndToken(passwordResetRequest.getEmail(), passwordResetRequest.getToken())
                .orElseThrow(() -> new TokenExpiredException("???????????? ?????? ???????????????. ?????? ??????????????????"));

        if (LocalDateTime.now().isAfter(passwordResetToken.getExpirationTime())) {
            passwordResetToken.setIsExpired(true);
            throw new TokenExpiredException("????????? ?????????????????????");
        }
    }
    public void passwordUpdateAfterReset(UserValidationRequest userValidationRequest) {
        User user = userRepository.findByEmail(userValidationRequest.getEmail())
                .orElseThrow(() -> new UserNotFoundException("???????????? ?????? ??????????????????"));
        String encryptedPassword = passwordEncoder.encode(userValidationRequest.getPassword());

        user.updatePassword(encryptedPassword);
    }

    public void passwordChange(final PasswordChangeRequest passwordChangeRequest, User user) {
        User updatedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new UserNotFoundException("???????????? ?????? ???????????????"));

        if (!passwordEncoder.matches(passwordChangeRequest.getPasswordNow(), user.getPassword())) {
            throw new PasswordInvalidException("?????? ??????????????? ???????????? ????????????");
        }
        if (passwordEncoder.matches(passwordChangeRequest.getPasswordAfter(), user.getPassword())) {
            throw new PasswordInvalidException("?????? ??????????????? ?????? ??????????????? ????????????");
        }
        updatedUser.updatePassword(passwordEncoder.encode(passwordChangeRequest.getPasswordAfter()));
    }
}
