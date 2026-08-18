package com.cryptotracker.users;

import com.cryptotracker.common.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserProfileResponse getProfile(UUID userId) {
        return UserProfileResponse.from(findUser(userId));
    }

    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        user.setDisplayName(request.displayName());
        user.setAvatarUrl(request.avatarUrl());
        user.setBaseCurrency(Currency.valueOf(request.baseCurrency()));
        user.setTimezone(request.timezone());

        User saved = userRepository.save(user);
        return UserProfileResponse.from(saved);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
    }
}
