package lk.supporthub;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.*;
import static lk.supporthub.Dtos.*;

@RestController @RequestMapping("/api")
class ApiController {
    private final HelpdeskService service;
    ApiController(HelpdeskService service) { this.service=service; }
    @GetMapping("/health") Map<String,String> health() {return Map.of("status","UP");}
    @GetMapping("/dashboard") Dashboard dashboard() {return service.dashboard();}
    @GetMapping("/tickets") PageResult list(@RequestParam(required=false) String q,@RequestParam(required=false) Status status,
        @RequestParam(required=false) Priority priority,@RequestParam(defaultValue="false") boolean mine,@RequestParam(defaultValue="0") int page) {
        return service.list(q,status,priority,mine,page);
    }
    @PostMapping("/tickets") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    TicketView create(@Valid @RequestBody NewTicket input) {return service.create(input);}
    @GetMapping("/tickets/{id}") Detail detail(@PathVariable Long id) {return service.detail(id);}
    @PutMapping("/tickets/{id}") TicketView update(@PathVariable Long id,@Valid @RequestBody UpdateTicket input) {return service.update(id,input);}
    @PostMapping("/tickets/{id}/comments") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    CommentView comment(@PathVariable Long id,@Valid @RequestBody NewComment input) {return service.comment(id,input);}
    @GetMapping("/agents") List<Person> agents() {return service.agents();}
    @GetMapping("/users") List<Person> team() {return service.team();}
    @PutMapping("/users/{id}") Person updateUser(@PathVariable Long id,@Valid @RequestBody UpdateAccount input) {return service.updateAccount(id,input);}
}
