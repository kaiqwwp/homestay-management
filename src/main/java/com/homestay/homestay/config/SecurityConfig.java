package com.homestay.homestay.config;

import com.homestay.homestay.entity.User;
import com.homestay.homestay.repository.UserRepository;
import com.homestay.homestay.security.CustomOAuth2UserService;
import com.homestay.homestay.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.util.Collections;
import java.util.UUID;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret:}")
    private String googleClientSecret;

    @Autowired(required = false)
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    @Lazy
    private UserService userService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private boolean isGoogleOAuthConfigured() {
        return googleClientId != null && !googleClientId.isBlank()
                && !googleClientId.equalsIgnoreCase("YOUR_GOOGLE_CLIENT_ID")
                && googleClientSecret != null && !googleClientSecret.isBlank()
                && !googleClientSecret.equalsIgnoreCase("YOUR_GOOGLE_CLIENT_SECRET");
    }

    @Bean
    public AuthenticationSuccessHandler loginSuccessHandler() {
        return (request, response, authentication) -> {
            User user = null;

            // Nếu đăng nhập qua Google OAuth2 / OIDC thật từ accounts.google.com
            if (authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
                String email = oAuth2User.getAttribute("email");
                String name = oAuth2User.getAttribute("name");
                if (email != null && !email.isBlank()) {
                    String cleanEmail = email.trim().toLowerCase();
                    user = userRepository.findByEmail(cleanEmail)
                            .orElseGet(() -> userRepository.findByUsername(cleanEmail).orElse(null));
                    if (user == null) {
                        user = new User();
                        user.setUsername(cleanEmail);
                        user.setEmail(cleanEmail);
                        String defaultName = (name != null && !name.isBlank())
                                ? name.trim()
                                : cleanEmail.substring(0, cleanEmail.indexOf('@'));
                        user.setFullName(defaultName);
                        user.setPassword(passwordEncoder().encode(UUID.randomUUID().toString()));
                        user.setRole("CUSTOMER");
                        user.setAuthProvider("GOOGLE");
                        user = userRepository.save(user);
                    }
                    userService.syncOrCreateCustomerForUser(user, user.getEmail(), user.getPhone());

                    // Đồng bộ Principal về username của hệ thống để mọi trang nhận diện chính xác
                    UsernamePasswordAuthenticationToken sysAuth = new UsernamePasswordAuthenticationToken(
                            user.getUsername(),
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
                    );
                    SecurityContext sc = SecurityContextHolder.getContext();
                    sc.setAuthentication(sysAuth);
                    HttpSession session = request.getSession(true);
                    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);
                }
            }

            if (user == null) {
                String username = authentication.getName();
                user = userRepository.findByUsername(username).orElse(null);
            }

            if (user != null && "CUSTOMER".equalsIgnoreCase(user.getRole()) && !user.hasCompleteProfile()) {
                response.sendRedirect("/profile?requireUpdate=true");
                return;
            }
            response.sendRedirect("/dashboard");
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Hỗ trợ REST API và AJAX calls
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/", "/login", "/register", "/home", "/oauth2/**", "/login/oauth2/**",
                        "/css/**", "/js/**", "/images/**", "/uploads/**", 
                        "/error/**", "/403", "/404", "/500",
                        "/api/auth/**", "/api/payment/sepay-webhook", "/api/ai/**"
                ).permitAll()
                .requestMatchers("/users/**").hasRole("ADMIN")
                .requestMatchers("/reports/**").hasRole("ADMIN")
                .requestMatchers(
                        "/customers/**", "/invoices/**",
                        "/bookings/calendar", "/bookings/*/approve", "/bookings/*/reject",
                        "/bookings/*/check-in", "/bookings/*/release-room",
                        "/bookings/*/status", "/bookings/*/edit"
                ).hasAnyRole("ADMIN", "STAFF")
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
                .successHandler(loginSuccessHandler())
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

        // Kích hoạt OAuth2 Login thật qua accounts.google.com khi đã cấu hình Client ID & Client Secret
        if (isGoogleOAuthConfigured()) {
            ClientRegistration googleRegistration = CommonOAuth2Provider.GOOGLE.getBuilder("google")
                    .clientId(googleClientId.trim())
                    .clientSecret(googleClientSecret.trim())
                    .scope("openid", "profile", "email")
                    .build();
            ClientRegistrationRepository repo = new InMemoryClientRegistrationRepository(googleRegistration);

            http.oauth2Login(oauth2 -> {
                oauth2.clientRegistrationRepository(repo);
                oauth2.loginPage("/login");
                oauth2.successHandler(loginSuccessHandler());
                if (customOAuth2UserService != null) {
                    oauth2.userInfoEndpoint(info -> info.userService(customOAuth2UserService));
                }
            });
        }

        return http.build();
    }
}
