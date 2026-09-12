package com.clothflow.user.service;

import com.clothflow.user.dto.request.RegisterRequest;
import com.clothflow.user.dto.response.UserResponse;
import com.clothflow.user.entity.Role;
import com.clothflow.user.entity.RoleName;
import com.clothflow.user.entity.User;
import com.clothflow.user.entity.UserCredential;
import com.clothflow.user.exception.RoleNotFoundException;
import com.clothflow.user.exception.UserAlreadyExistsException;
import com.clothflow.user.repository.RoleRepository;
import com.clothflow.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserRegistrationService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {

        String email = normalizeEmail(request.email());
        String username = normalizeUsername(request.username());

        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException(
                    "Email is already registered"
            );
        }

        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException(
                    "Username is already taken"
            );
        }

        Role customerRole = roleRepository
                .findByName(RoleName.CUSTOMER)
                .orElseThrow(() ->
                        new RoleNotFoundException(
                                "Default CUSTOMER role was not found"
                        )
                );

        User user = new User(
                email,
                username
        );

        user.addRole(customerRole);

        String passwordHash =
                passwordEncoder.encode(request.password());

        UserCredential credential =
                new UserCredential(
                        user,
                        passwordHash
                );

        user.setCredential(credential);

        User savedUser =
                userRepository.save(user);

        return toResponse(savedUser);
    }

    private String normalizeEmail(String email) {
        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeUsername(String username) {
        return username
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private UserResponse toResponse(User user) {

        Set<RoleName> roles = user.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toUnmodifiableSet());

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getStatus(),
                roles
        );
    }
}