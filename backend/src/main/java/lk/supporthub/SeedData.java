package lk.supporthub;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

@Component
class SeedData implements CommandLineRunner {
    private final AccountRepository accounts;private final TicketRepository tickets;private final ActivityRepository activities;
    private final PasswordEncoder encoder;private final Environment env;
    SeedData(AccountRepository a,TicketRepository t,ActivityRepository h,PasswordEncoder e,Environment env) {accounts=a;tickets=t;activities=h;encoder=e;this.env=env;}
    @Override @Transactional public void run(String... args) {
        if(Arrays.asList(env.getActiveProfiles()).contains("demo")) {
            if(accounts.count()>0) return;
            var admin=account("Alex Morgan","admin@supporthub.test",Role.ADMIN,"DemoAdmin!2026");
            var agent=account("Jamie Chen","agent@supporthub.test",Role.AGENT,"DemoAgent!2026");
            var requester=account("Sam Taylor","requester@supporthub.test",Role.REQUESTER,"DemoUser!2026");
            String[] titles={"Unable to connect to the office VPN","New starter needs workspace access","Laptop battery drains during meetings","Email attachments are not downloading","Request for a second monitor","Shared drive permissions need updating","Wi-Fi drops in the meeting room","Design software license activation","Password reset for analytics workspace"};
            Category[] categories={Category.NETWORK,Category.ACCESS,Category.HARDWARE,Category.SOFTWARE,Category.HARDWARE,Category.ACCESS,Category.NETWORK,Category.SOFTWARE,Category.ACCESS};
            Status[] statuses={Status.IN_PROGRESS,Status.OPEN,Status.WAITING,Status.RESOLVED,Status.OPEN,Status.IN_PROGRESS,Status.OPEN,Status.CLOSED,Status.RESOLVED};
            Priority[] priorities={Priority.URGENT,Priority.HIGH,Priority.MEDIUM,Priority.MEDIUM,Priority.LOW,Priority.HIGH,Priority.HIGH,Priority.LOW,Priority.MEDIUM};
            for(int i=0;i<titles.length;i++) {
                var t=new Ticket();t.title=titles[i];t.description="Hello support team,\n\n"+titles[i]+". This is affecting my daily work. I have tried restarting and checking the usual settings, but I still need assistance.\n\nPlease let me know what additional information you need.";
                t.category=categories[i];t.priority=priorities[i];t.status=statuses[i];t.requester=requester;
                t.assignee=i%3==1?null:agent;t.createdAt=Instant.now().minus(24+i*7,ChronoUnit.HOURS);t.updatedAt=Instant.now().minus(i*2,ChronoUnit.HOURS);tickets.save(t);
                var h=new Activity();h.ticket=t;h.actor=admin;h.action="Demo ticket imported with status "+t.status;h.createdAt=t.createdAt;activities.save(h);
            }
        } else {
            String email=env.getProperty("supporthub.admin-email","").trim().toLowerCase(java.util.Locale.ROOT);
            String password=env.getProperty("supporthub.admin-password","");
            if(!email.isBlank()&&accounts.findByEmail(email).isEmpty()) {
                if(password.length()<12||password.length()>64) throw new IllegalStateException("ADMIN_PASSWORD must contain 12–64 characters");
                account("Administrator",email,Role.ADMIN,password);
            }
        }
    }
    private Account account(String name,String email,Role role,String password) {
        var a=new Account();a.name=name;a.email=email;a.role=role;a.passwordHash=encoder.encode(password);return accounts.save(a);
    }
}
