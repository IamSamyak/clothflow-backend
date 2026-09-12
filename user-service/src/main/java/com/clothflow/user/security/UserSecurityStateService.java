package com.clothflow.user.security;

import com.clothflow.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserSecurityStateService {

    private final UserRepository userRepository;

    public UserSecurityStateService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isSecurityVersionValid(
            UUID userId,
            long tokenSecurityVersion
    ) {
        return userRepository.findSecurityVersionById(userId)
                .map(currentVersion ->
                        currentVersion == tokenSecurityVersion)
                .orElse(false);
    }
}