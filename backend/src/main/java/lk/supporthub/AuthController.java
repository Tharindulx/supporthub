package lk.supporthub;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import java.util.Map;

@RestController @RequestMapping("/api/auth")
class AuthController {
    private final AuthenticationManager manager; private final SecurityContextRepository contexts; private final HelpdeskService service;
    AuthController(AuthenticationManager manager, SecurityContextRepository contexts, HelpdeskService service) {
        this.manager=manager;this.contexts=contexts;this.service=service;
    }
    @GetMapping("/csrf") Map<String,String> csrf(CsrfToken token) { return Map.of("token",token.getToken()); }
    @PostMapping("/register") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    Dtos.Person register(@Valid @RequestBody Dtos.Register input) { return service.register(input); }
    @PostMapping("/login") Dtos.Person login(@Valid @RequestBody Dtos.Login input, HttpServletRequest req, HttpServletResponse res) {
        var auth=manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(input.email().trim().toLowerCase(java.util.Locale.ROOT),input.password()));
        if(req.getSession(false)!=null) req.changeSessionId();
        var context=SecurityContextHolder.createEmptyContext();context.setAuthentication(auth);
        SecurityContextHolder.setContext(context); contexts.saveContext(context,req,res);
        return service.me();
    }
    @GetMapping("/me") Dtos.Person me() { return service.me(); }
}
