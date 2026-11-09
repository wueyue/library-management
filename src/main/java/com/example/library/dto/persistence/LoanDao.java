package com.example.library.dto.persistence;

import com.example.library.exception.BusinessException;
import com.example.library.exception.ErrorCode;
import com.example.library.model.Loan;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Loan 的独立 JDBC 数据访问组件。 */
@Repository
public class LoanDao {
    private final JdbcTemplate jdbc;

    public LoanDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static UUID uuid(ResultSet r, String key) throws SQLException {
        return r.getObject(key, UUID.class);
    }

    private static OffsetDateTime time(ResultSet r, String key) throws SQLException {
        return r.getObject(key, OffsetDateTime.class);
    }

    private static Loan loan(ResultSet r) throws SQLException {
        return new Loan(
                uuid(r, "id"),
                uuid(r, "book_id"),
                uuid(r, "reader_id"),
                time(r, "borrow_time"),
                time(r, "return_time"),
                time(r, "update_time"));
    }

    private static final RowMapper<Loan> LOAN = (r, n) -> loan(r);

    public record CurrentLoanRow(Loan loan, String title, String author) {
    }

    public boolean activeLoan(UUID reader, UUID book) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM loans WHERE reader_id=? AND book_id=? AND"
                                + " return_time IS NULL)",
                        Boolean.class,
                        reader,
                        book));
    }

    public Loan createLoan(Loan loan) {
        loan.validate();
        return jdbc.queryForObject(
                "INSERT INTO loans(id,reader_id,book_id,borrow_time,return_time,update_time)"
                        + " VALUES(?,?,?,?,?,?) RETURNING *",
                LOAN,
                loan.getId(),
                loan.getReaderId(),
                loan.getBookId(),
                loan.getBorrowTime(),
                loan.getReturnTime(),
                loan.getUpdateTime());
    }

    public Loan findOwnedById(UUID id, UUID reader) {
        return findOwned(id, reader, false);
    }

    public Loan findOwnedByIdForUpdate(UUID id, UUID reader) {
        return findOwned(id, reader, true);
    }

    private Loan findOwned(UUID id, UUID reader, boolean lock) {
        return jdbc
                .query(
                        "SELECT * FROM loans WHERE id=? AND reader_id=?"
                                + (lock ? " FOR UPDATE" : ""),
                        LOAN,
                        id,
                        reader)
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.LOAN_NOT_FOUND, "借阅不存在"));
    }

    public Loan markReturned(Loan loan) {
        loan.validate();
        return jdbc.queryForObject(
                "UPDATE loans SET return_time=?,update_time=? WHERE id=? AND return_time IS NULL"
                        + " RETURNING *",
                LOAN,
                loan.getReturnTime(),
                loan.getUpdateTime(),
                loan.getId());
    }

    public List<CurrentLoanRow> currentLoans(UUID reader, int page, int size) {
        return jdbc.query(
                "SELECT l.*,b.title,b.author FROM loans l JOIN books b ON b.id=l.book_id WHERE"
                    + " l.reader_id=? AND l.return_time IS NULL ORDER BY l.borrow_time DESC,l.id"
                    + " DESC LIMIT ? OFFSET ?",
                (r, n) -> new CurrentLoanRow(loan(r), r.getString("title"), r.getString("author")),
                reader,
                size,
                (long) (page - 1) * size);
    }
}
