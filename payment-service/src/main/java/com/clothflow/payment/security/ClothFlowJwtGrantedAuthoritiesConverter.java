package com.clothflow.payment.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ClothFlowJwtGrantedAuthoritiesConverter
        implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {

        List<GrantedAuthority> authorities =
                new ArrayList<>();

        List<String> roles =
                jwt.getClaimAsStringList("roles");

        if (roles != null) {

            roles.forEach(role ->
                    authorities.add(
                            new SimpleGrantedAuthority(
                                    "ROLE_" + role
                            )
                    )
            );
        }

        List<String> scopes =
                jwt.getClaimAsStringList("scope");

        if (scopes != null) {

            scopes.forEach(scope ->
                    authorities.add(
                            new SimpleGrantedAuthority(
                                    "SCOPE_" + scope
                            )
                    )
            );
        }

        String tokenType =
                jwt.getClaimAsString("token_type");

        if ("service".equals(tokenType)) {

            authorities.add(
                    new SimpleGrantedAuthority(
                            "TOKEN_SERVICE"
                    )
            );
        }

        if ("access".equals(tokenType)) {

            authorities.add(
                    new SimpleGrantedAuthority(
                            "TOKEN_ACCESS"
                    )
            );
        }

        return authorities;
    }
}