CREATE TABLE accounts (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(100) NOT NULL, email VARCHAR(190) NOT NULL UNIQUE,
 password_hash VARCHAR(255) NOT NULL, role VARCHAR(20) NOT NULL, enabled BOOLEAN NOT NULL
);
CREATE TABLE tickets (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, title VARCHAR(160) NOT NULL,
 description VARCHAR(8000) NOT NULL, category VARCHAR(20) NOT NULL,
 priority VARCHAR(20) NOT NULL, status VARCHAR(20) NOT NULL,
 requester_id BIGINT NOT NULL, assignee_id BIGINT,
 created_at TIMESTAMP(6) NOT NULL, updated_at TIMESTAMP(6) NOT NULL, version BIGINT NOT NULL,
 FOREIGN KEY (requester_id) REFERENCES accounts(id), FOREIGN KEY (assignee_id) REFERENCES accounts(id)
);
CREATE INDEX idx_ticket_requester ON tickets(requester_id);
CREATE INDEX idx_ticket_status ON tickets(status);
CREATE INDEX idx_ticket_updated ON tickets(updated_at);
CREATE TABLE comments (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, ticket_id BIGINT NOT NULL, author_id BIGINT NOT NULL,
 body VARCHAR(4000) NOT NULL, created_at TIMESTAMP(6) NOT NULL,
 FOREIGN KEY (ticket_id) REFERENCES tickets(id), FOREIGN KEY (author_id) REFERENCES accounts(id)
);
CREATE TABLE activities (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, ticket_id BIGINT NOT NULL, actor_id BIGINT NOT NULL,
 action VARCHAR(500) NOT NULL, created_at TIMESTAMP(6) NOT NULL,
 FOREIGN KEY (ticket_id) REFERENCES tickets(id), FOREIGN KEY (actor_id) REFERENCES accounts(id)
);
CREATE INDEX idx_comment_ticket ON comments(ticket_id);
CREATE INDEX idx_activity_ticket ON activities(ticket_id);
