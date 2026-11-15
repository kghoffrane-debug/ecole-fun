package tn.ecole.fun.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(a -> a
                .requestMatchers("/css/**", "/js/**", "/img/**", "/login", "/manifest.json", "/sw.js").permitAll()
                // Le propriétaire du SaaS gère les écoles, et seulement cela.
                .requestMatchers("/schools/**").hasRole("SUPER_ADMIN")
                // Les données d'une école ne sont jamais accessibles au propriétaire ni sans école.
                .requestMatchers("/students/**", "/classes/**", "/subjects/**", "/grades/**",
                        "/payments/**", "/messages/**", "/users/**")
                    .hasAnyRole("ADMIN", "TEACHER", "PARENT", "STUDENT", "ACCOUNTANT")
                .anyRequest().authenticated())
            .formLogin(f -> f.loginPage("/login").defaultSuccessUrl("/", true).permitAll())
            .logout(l -> l.logoutSuccessUrl("/login?logout"));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
