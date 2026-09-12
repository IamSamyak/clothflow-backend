package com.clothflow.product.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.List;

public class JwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {

        List<String> roles =
                jwt.getClaimAsStringList("roles");

        Collection<SimpleGrantedAuthority> authorities =
                roles == null
                        ? List.of()
                        : roles.stream()
                        .filter(role ->
                                role != null
                                        && !role.isBlank()
                        )
                        .map(role ->
                                new SimpleGrantedAuthority(
                                        "ROLE_" + role
                                )
                        )
                        .toList();

        return new JwtAuthenticationToken(
                jwt,
                authorities,
                jwt.getSubject()
        );
    }
}