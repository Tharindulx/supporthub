package lk.supporthub;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
class ApiErrors {
    @ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> business(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"Request failed":e.getReason()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validation(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest().body(Map.of("message",e.getBindingResult().getFieldErrors().stream().map(f->f.getField()+": "+f.getDefaultMessage()).collect(java.util.stream.Collectors.joining("; "))));
    }
    @ExceptionHandler(AuthenticationException.class) ResponseEntity<?> auth() {return ResponseEntity.status(401).body(Map.of("message","Invalid email or password"));}
    @ExceptionHandler({DataIntegrityViolationException.class,OptimisticLockingFailureException.class}) ResponseEntity<?> conflict() {
        return ResponseEntity.status(409).body(Map.of("message","The data changed or already exists. Refresh and try again."));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class}) ResponseEntity<?> malformed() {
        return ResponseEntity.badRequest().body(Map.of("message","Invalid request. Check the field values."));
    }
}
