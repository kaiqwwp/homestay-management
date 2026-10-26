package com.homestay.homestay.config;

import com.homestay.homestay.security.CustomOAuth2UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired(required = false)
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired(required = false)
    private CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Hỗ trợ REST API và AJAX calls
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/", "/login", "/register", "/home", 
                        "/css/**", "/js/**", "/images/**", "/uploads/**", 
                        "/error/**", "/403", "/404", "/500",
                        "/api/auth/**", "/api/payment/sepay-webhook", "/api/ai/**"
                ).permitAll()
                .requestMatchers("/users/**").hasRole("ADMIN")
                .requestMatchers(
                        "/rooms/new", "/rooms/*/edit", "/rooms/*/delete",
                        "/room-types/new", "/room-types/*/edit", "/room-types/*/delete",
                        "/services/new", "/services/*/edit", "/services/*/delete"
                ).hasAnyRole("ADMIN", "STAFF")
                .anyRequest().authenticated()
            )
            .formLogin(login -> login
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/error/403")
            );

        // Kích hoạt OAuth2 Login nếu ClientRegistrationRepository khả dụng (đã cấu hình google client-id)
        if (clientRegistrationRepository != null) {
            http.oauth2Login(oauth2 -> {
                oauth2.loginPage("/login");
                oauth2.defaultSuccessUrl("/dashboard", true);
                if (customOAuth2UserService != null) {
                    oauth2.userInfoEndpoint(info -> info.userService(customOAuth2UserService));
                }
            });
        }

        return http.build();
    }
}
