package com.bancoxyz.common.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * auth-service emite el claim "roles" (lista de strings, ej. ["ADMIN"]) en vez del
 * claim "scope"/"scp" que Spring Security espera por defecto, por lo que el
 * JwtGrantedAuthoritiesConverter estandar no sirve aqui.
 */
public class RolesClaimJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles == null) {
            roles = List.of();
        }
        Collection<GrantedAuthority> authorities = roles.stream()
                .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol))
                .collect(Collectors.toList());
        return new UsernamePasswordAuthenticationToken(jwt, jwt, authorities);
    }
}
