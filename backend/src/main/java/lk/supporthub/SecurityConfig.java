package lk.supporthub;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.*;
import org.springframework.security.web.csrf.*;

@Configuration
class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean UserDetailsService users(AccountRepository accounts) {
        return email -> { var a=accounts.findByEmail(email).orElseThrow(()->new UsernameNotFoundException("Invalid credentials"));
            return User.withUsername(a.email).password(a.passwordHash).roles(a.role.name()).disabled(!a.enabled).build(); };
    }
    @Bean AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
        var provider=new org.springframework.security.authentication.dao.DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder); return new ProviderManager(provider);
    }
    @Bean SecurityContextRepository contexts() { return new HttpSessionSecurityContextRepository(); }
    @Bean SecurityFilterChain chain(HttpSecurity http, SecurityContextRepository contexts) throws Exception {
        var csrf=CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrf.setCookieCustomizer(c->c.sameSite("Lax"));
        return http.csrf(c->c.csrfTokenRepository(csrf).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .securityContext(c->c.securityContextRepository(contexts))
            .authorizeHttpRequests(a->a.requestMatchers("/api/auth/csrf","/api/auth/login","/api/auth/register","/api/health").permitAll()
                .requestMatchers("/api/**").authenticated().anyRequest().permitAll())
            .exceptionHandling(e->e.authenticationEntryPoint((q,s,x)->{s.setStatus(401);s.setContentType("application/json");s.getWriter().write("{\"message\":\"Please sign in\"}");})
                .accessDeniedHandler((q,s,x)->{s.setStatus(403);s.setContentType("application/json");s.getWriter().write("{\"message\":\"Access denied or security token expired. Refresh and try again.\"}");}))
            .logout(l->l.logoutUrl("/api/auth/logout").logoutSuccessHandler((q,s,a)->s.setStatus(204)).deleteCookies("JSESSIONID"))
            .headers(h->h.contentSecurityPolicy(c->c.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'")))
            .build();
    }
}
