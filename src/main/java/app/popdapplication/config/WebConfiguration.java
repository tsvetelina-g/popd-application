package app.popdapplication.config;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableMethodSecurity
public class WebConfiguration implements WebMvcConfigurer {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity, SessionRegistry sessionRegistry) throws Exception {

        httpSecurity.authorizeHttpRequests(matcher -> matcher
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers("/", "/register", "/movie", "/artist", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/find",
                                "/find/**",
                                "/movies",
                                "/movies/**",
                                "/movie/**",
                                "/artist/**")
                        .permitAll()
                        .anyRequest().authenticated()

                )
                .formLogin(formLogin -> formLogin
                                .loginPage("/login")
                                .defaultSuccessUrl("/profile", true)
                                .failureUrl("/login?error")
                                .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .invalidSessionUrl("/login")
                        .maximumSessions(10)
                        .expiredUrl("/login?expired")
                        .sessionRegistry(sessionRegistry)
                );

        return httpSecurity.build();
    }

}
