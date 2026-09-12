package com.clothflow.user.security;

import com.clothflow.user.entity.User;
import com.clothflow.user.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService
        implements UserDetailsService {

    private final UserRepository userRepository;

    public DatabaseUserDetailsService(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(
            String email
    ) throws UsernameNotFoundException {

        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(
                                () -> new UsernameNotFoundException(
                                        "User not found"
                                )
                        );

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                /*
                 * We do not use this password for our JWT login flow.
                 *
                 * Spring Security requires a password value for the
                 * UserDetails contract, so we provide the persisted
                 * password hash.
                 */
                .password(
                        user.getCredential()
                                .getPasswordHash()
                )
                .authorities(
                        user.getRoles()
                                .stream()
                                .map(role ->
                                        new SimpleGrantedAuthority(
                                                "ROLE_" +
                                                        role.getName().name()
                                        )
                                )
                                .toList()
                )
                .accountLocked(
                        user.getStatus().name().equals("LOCKED")
                )
                .disabled(
                        user.getStatus().name().equals("DISABLED")
                )
                .build();
    }
}