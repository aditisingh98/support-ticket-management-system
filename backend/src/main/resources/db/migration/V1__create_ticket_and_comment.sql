-- Ticket and comment schema aligned with spec/data-model.md
-- TIMESTAMP WITH TIME ZONE is used (PostgreSQL synonym of TIMESTAMPTZ) for H2 test portability.

CREATE TABLE ticket (
    id          UUID PRIMARY KEY,
    title       TEXT NOT NULL,
    description TEXT NOT NULL,
    priority    VARCHAR(16) NOT NULL,
    assignee    TEXT NULL,
    status      VARCHAR(32) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ticket_priority_chk CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT ticket_status_chk CHECK (status IN (
        'OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'CANCELLED'
    ))
);

CREATE TABLE comment (
    id         UUID PRIMARY KEY,
    ticket_id  UUID NOT NULL REFERENCES ticket (id),
    body       TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
