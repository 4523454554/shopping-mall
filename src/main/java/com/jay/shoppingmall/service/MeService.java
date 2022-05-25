package com.jay.shoppingmall.service;

import com.jay.shoppingmall.domain.image.ImageRepository;
import com.jay.shoppingmall.domain.item.Item;
import com.jay.shoppingmall.domain.item.ItemRepository;
import com.jay.shoppingmall.domain.user.User;
import com.jay.shoppingmall.domain.user.UserRepository;
import com.jay.shoppingmall.domain.user.model.Address;
import com.jay.shoppingmall.domain.user.model.Agree;
import com.jay.shoppingmall.domain.user.model.Name;
import com.jay.shoppingmall.domain.user.model.PhoneNumber;
import com.jay.shoppingmall.domain.zzim.Zzim;
import com.jay.shoppingmall.domain.zzim.ZzimRepository;
import com.jay.shoppingmall.dto.request.AgreeRequest;
import com.jay.shoppingmall.dto.request.DeleteMeRequest;
import com.jay.shoppingmall.dto.response.ItemResponse;
import com.jay.shoppingmall.dto.response.MeDetailResponse;
import com.jay.shoppingmall.dto.request.UserUpdateRequest;
import com.jay.shoppingmall.dto.response.SearchResponse;
import com.jay.shoppingmall.dto.response.ZzimResponse;
import com.jay.shoppingmall.exception.exceptions.AgreeException;
import com.jay.shoppingmall.exception.exceptions.ItemNotFoundException;
import com.jay.shoppingmall.exception.exceptions.PasswordInvalidException;
import com.jay.shoppingmall.exception.exceptions.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class MeService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ZzimRepository zzimRepository;
    private final ItemRepository itemRepository;
    private final FileHandler fileHandler;
    private final ImageRepository imageRepository;

    public boolean passwordCheck(String password, User user) {

        return passwordEncoder.matches(password, user.getPassword());
    }

    public MeDetailResponse findById(final Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new UsernameNotFoundException("User Not Found"));

        return MeDetailResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .build();
    }

    public boolean agreeCheck(final AgreeRequest agreeRequest, final Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("잘못된 요청입니다"));

        if (user.getAgree().getIsMandatoryAgree()) {
            throw new AgreeException("잘못된 요청입니다");
        }
        if (!agreeRequest.getIsMandatoryAgree()) {
            throw new AgreeException("필수 항목을 반드시 동의하셔야 합니다");
        }

        Agree agree = Agree.builder()
                .isMandatoryAgree(agreeRequest.getIsMandatoryAgree())
                .isMarketingAgree(agreeRequest.getIsMarketingAgree())
                .build();

        user.setAgree(agree);

        return true;
    }

    public User updateInfo(final UserUpdateRequest request, final Object isMarketingAgree, final Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("잘못된 요청입니다"));

        Address address = Address.builder()
                .address(request.getAddress())
                .detailAddress(request.getDetailAddress())
                .extraAddress(request.getExtraAddress())
                .zipcode(request.getZipcode())
                .build();
        Name name = Name.builder()
                .last(request.getLastName())
                .first(request.getFirstName())
                .build();

        Agree agree = Agree.builder()
                .isMandatoryAgree(true)
                .isMarketingAgree(isMarketingAgree.equals("true") ? true : false)
                .build();

        user.userUpdate(address, name, agree, splitPhoneNumber(request.getPhoneNumber()));

        return userRepository.save(user);

//         UserUpdateResponse.builder()
//                .address(user.getAddress().getAddress())
//                .detailAddress(user.getAddress().getDetailAddress())
//                .extraAddress(user.getAddress().getExtraAddress())
//                .zipcode(user.getAddress().getZipcode())
//                .lastName(user.getName().getLast())
//                .firstName(user.getName().getFirst())
//                .phoneNumber(user.getPhoneNumber().getFullNumber())
//                .build();
    }

    private PhoneNumber splitPhoneNumber(String phoneNumber) {
        if (phoneNumber.contains("-")) {
            phoneNumber = phoneNumber.replace("-", "");
        }
        return PhoneNumber.builder()
                .first(phoneNumber.substring(0, 3))
                .middle(phoneNumber.length() == 11 ?
                        phoneNumber.substring(3, 7) :
                        phoneNumber.substring(3, 6))
                .last(phoneNumber.substring(phoneNumber.length() - 4))
                .build();
    }

    public void deleteMe(final DeleteMeRequest request, final User user) {

        if (!passwordCheck(request.getPassword(), user)) {
            throw new PasswordInvalidException("비밀번호가 일치하지 않습니다");
        }
        userRepository.delete(user);
    }

    public List<ItemResponse> getAllMeZzim(User user) {
        List<Zzim> zzims = zzimRepository.findByUser(user).orElseThrow(() -> new ItemNotFoundException("아이디에 상품이 없습니다"));
        List<ItemResponse> itemResponses = new ArrayList<>();

        for (Zzim zzim : zzims) {
            if (!zzim.getIsZzimed()) {
                continue;
            }
            Item item = itemRepository.findById(zzim.getItem().getId())
                    .orElseThrow(() -> new ItemNotFoundException("상품이 없습니다"));

            itemResponses.add(ItemResponse.builder()
                    .id(item.getId())
                    .name(item.getName())
                    .zzim(item.getZzim())
                    .image(fileHandler.getStringImage(imageRepository.findByItemIdAndIsMainImageTrue(item.getId())))
                    .price(item.getPrice())
                    .salePrice(item.getSalePrice())
                    .build());
        }
        return itemResponses;
    }
}
