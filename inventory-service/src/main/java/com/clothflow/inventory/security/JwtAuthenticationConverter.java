package com.clothflow.inventory.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {

        Collection<SimpleGrantedAuthority> authorities =
                new ArrayList<>();

        /*
         * Customer roles.
         *
         * Example:
         * roles = ["CUSTOMER"]
         *
         * becomes:
         * ROLE_CUSTOMER
         */
        List<String> roles =
                jwt.getClaimAsStringList("roles");

        if (roles != null) {
            roles.stream()
                    .filter(role ->
                            role != null
                                    && !role.isBlank()
                    )
                    .map(role ->
                            new SimpleGrantedAuthority(
                                    "ROLE_" + role
                            )
                    )
                    .forEach(authorities::add);
        }

        /*
         * Service scopes.
         *
         * Example:
         * scope = ["inventory.read", "inventory.write"]
         *
         * becomes:
         * SCOPE_inventory.read
         * SCOPE_inventory.write
         */
        List<String> scopes =
                jwt.getClaimAsStringList("scope");

        if (scopes != null) {
            scopes.stream()
                    .filter(scope ->
                            scope != null
                                    && !scope.isBlank()
                    )
                    .map(scope ->
                            new SimpleGrantedAuthority(
                                    "SCOPE_" + scope
                            )
                    )
                    .forEach(authorities::add);
        }

        /*
         * Explicit token-type authorities.
         *
         * This is extremely important.
         *
         * TOKEN_ACCESS
         * TOKEN_SERVICE
         *
         * allow endpoint authorization to distinguish
         * customer JWTs from service JWTs.
         */
        String tokenType =
                jwt.getClaimAsString("token_type");

        if ("access".equals(tokenType)) {

            authorities.add(
                    new SimpleGrantedAuthority(
                            "TOKEN_ACCESS"
                    )
            );

        } else if ("service".equals(tokenType)) {

            authorities.add(
                    new SimpleGrantedAuthority(
                            "TOKEN_SERVICE"
                    )
            );
        }

        return new JwtAuthenticationToken(
                jwt,
                authorities,
                jwt.getSubject()
        );
    }
}