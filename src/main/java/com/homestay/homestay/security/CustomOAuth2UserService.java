package com.homestay.homestay.security;

import com.homestay.homestay.entity.User;
import com.homestay.homestay.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");

        if (email != null) {
            Optional<User> optUser = userRepository.findByUsername(email);
            User user;
            if (optUser.isEmpty()) {
                user = new User();
                user.setUsername(email);
                user.setPassword("{noop}");
                user.setFullName(name != null ? name : email);
                user.setEmail(email);
                user.setRole("CUSTOMER");
                user.setAuthProvider("GOOGLE");
                userRepository.save(user);
            } else {
                user = optUser.get();
            }

            String role = user.getRole();
            if (!role.startsWith("ROLE_")) {
                role = "ROLE_" + role;
            }

            return new DefaultOAuth2User(
                    Collections.singletonList(new SimpleGrantedAuthority(role)),
                    attributes,
                    "email"
            );
        }

        return oAuth2User;
    }
}
