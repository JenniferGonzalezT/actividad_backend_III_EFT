package com.bancoxyz.cuentas.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Los microservicios de negocio son resource server: sin este interceptor, las
 * llamadas Feign salientes no llevan el JWT del usuario original y el downstream
 * responde 401. Se reenvia el header Authorization de la request entrante.
 */
@Component
public class FeignAuthForwardingInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes servletAttrs) {
            String authHeader = servletAttrs.getRequest().getHeader("Authorization");
            if (authHeader != null) {
                template.header("Authorization", authHeader);
            }
        }
    }
}
