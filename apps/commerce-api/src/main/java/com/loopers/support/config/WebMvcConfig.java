package com.loopers.support.config;

import com.loopers.support.interceptor.AdminInterceptor;
import com.loopers.support.interceptor.AuthInterceptor;
import com.loopers.support.interceptor.QueueTokenInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@RequiredArgsConstructor
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AdminInterceptor adminInterceptor;
    private final QueueTokenInterceptor queueTokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
            .addPathPatterns(
                "/api/v1/users/me",
                "/api/v1/users/me/**",
                "/api/v1/users/*/likes",
                "/api/v1/products/*/likes",
                "/api/v1/orders",
                "/api/v1/orders/**",
                "/api/v1/coupons/**",
                "/api/v1/payments",
                "/api/v1/payments/*/sync",
				"/api/v1/queue",
				"/api/v1/queue/**"
            );
        // 인증(authInterceptor) 이후에 실행되어야 인증된 사용자 정보를 활용해 입장 토큰을 검증할 수 있다.
        registry.addInterceptor(queueTokenInterceptor)
            .addPathPatterns("/api/v1/orders");
        registry.addInterceptor(adminInterceptor)
            .addPathPatterns("/api-admin/v1/**");
    }
}
