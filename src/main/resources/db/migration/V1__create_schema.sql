CREATE TABLE readers (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE books (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    description TEXT,
    total_copies INTEGER NOT NULL CHECK (total_copies >= 0),
    available_copies INTEGER NOT NULL,
    create_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_book_stock CHECK (
        available_copies >= 0 AND available_copies <= total_copies
    )
);

CREATE TABLE loans (
    id UUID PRIMARY KEY,
    reader_id UUID NOT NULL REFERENCES readers(id),
    book_id UUID NOT NULL REFERENCES books(id),
    borrow_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    return_time TIMESTAMPTZ,
    update_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_loan_time CHECK (
        return_time IS NULL OR return_time >= borrow_time
    )
);

CREATE UNIQUE INDEX uq_active_loan
    ON loans (reader_id, book_id) WHERE return_time IS NULL;

CREATE INDEX idx_reader_active_loans
    ON loans (reader_id, borrow_time DESC, id DESC)
    WHERE return_time IS NULL;

CREATE INDEX idx_books_listing ON books (create_time DESC, id DESC);
