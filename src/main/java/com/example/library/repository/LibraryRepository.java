package com.example.library.repository;

import com.example.library.exception.BusinessException;
import com.example.library.model.Book;
import com.example.library.model.Reader;
import com.example.library.model.Loan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** 持久化边界仅接受和返回领域对象，不依赖 API 请求/响应 DTO。 */
@Repository
public class LibraryRepository {
    private final JdbcTemplate jdbc;
    public LibraryRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    private static UUID uuid(ResultSet r,String key) throws SQLException { return r.getObject(key,UUID.class); }
    private static OffsetDateTime time(ResultSet r,String key) throws SQLException { return r.getObject(key,OffsetDateTime.class); }
    private static final RowMapper<Book> BOOK=(r,n)->new Book(uuid(r,"id"),r.getString("title"),r.getString("author"),r.getString("description"),r.getInt("total_copies"),r.getInt("available_copies"),time(r,"create_time"),time(r,"update_time"));
    private static final RowMapper<Reader> READER=(r,n)->new Reader(uuid(r,"id"),r.getString("name"),r.getString("student_number"),time(r,"create_time"),time(r,"update_time"));
    private static Loan loan(ResultSet r) throws SQLException { return new Loan(uuid(r,"id"),uuid(r,"book_id"),uuid(r,"reader_id"),time(r,"borrow_time"),time(r,"return_time"),time(r,"update_time")); }
    private static final RowMapper<Loan> LOAN=(r,n)->loan(r);
    /** 当前借阅的关联查询投影，保持单次 SQL，避免逐条查询图书。 */
    public record CurrentLoanRow(Loan loan,String title,String author) {}
    public OffsetDateTime now() { return jdbc.queryForObject("SELECT clock_timestamp()",OffsetDateTime.class); }
    public void lockTimeout() { jdbc.execute("SET LOCAL lock_timeout = '2s'"); }
    public Book createBook(Book book) {
        return jdbc.queryForObject("INSERT INTO books(id,title,author,description,total_copies,available_copies,create_time,update_time) VALUES(?,?,?,?,?,?,?,?) RETURNING *",BOOK,
            book.getId(),book.getTitle(),book.getAuthor(),book.getDescription(),book.getTotalCopies(),book.getAvailableCopies(),book.getCreateTime(),book.getUpdateTime());
    }
    public Reader createReader(Reader reader) {
        return jdbc.queryForObject("INSERT INTO readers(id,name,student_number,create_time,update_time) VALUES(?,?,?,?,?) RETURNING *",READER,
            reader.getId(),reader.getName(),reader.getStudentNumber(),reader.getCreateTime(),reader.getUpdateTime());
    }
    public boolean readerExists(UUID id) { return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM readers WHERE id=?)",Boolean.class,id)); }
    public List<Book> books(int page,int size) { return jdbc.query("SELECT * FROM books ORDER BY create_time DESC,id DESC LIMIT ? OFFSET ?",BOOK,size,(long)(page-1)*size); }
    public Book book(UUID id,boolean lock) {
        return jdbc.query("SELECT * FROM books WHERE id=?"+(lock?" FOR UPDATE":""),BOOK,id).stream().findFirst().orElseThrow(()->new BusinessException(404,"BOOK_NOT_FOUND","图书不存在"));
    }
    public boolean activeLoan(UUID reader,UUID book) { return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM loans WHERE reader_id=? AND book_id=? AND return_time IS NULL)",Boolean.class,reader,book)); }
    /** 写入领域对象计算后的库存，并以原库存值作为条件兜底。调用方必须先持有图书行锁。 */
    public int decrement(Book updated) {
        return jdbc.update("UPDATE books SET available_copies=?,update_time=? WHERE id=? AND available_copies=? AND available_copies>0",updated.getAvailableCopies(),updated.getUpdateTime(),updated.getId(),updated.getAvailableCopies()+1);
    }
    public int increment(Book updated) {
        return jdbc.update("UPDATE books SET available_copies=?,update_time=? WHERE id=? AND available_copies=? AND available_copies<total_copies",updated.getAvailableCopies(),updated.getUpdateTime(),updated.getId(),updated.getAvailableCopies()-1);
    }
    public Loan createLoan(Loan loan) {
        return jdbc.queryForObject("INSERT INTO loans(id,reader_id,book_id,borrow_time,return_time,update_time) VALUES(?,?,?,?,?,?) RETURNING *",LOAN,
            loan.getId(),loan.getReaderId(),loan.getBookId(),loan.getBorrowTime(),loan.getReturnTime(),loan.getUpdateTime());
    }
    public Loan ownedLoan(UUID id,UUID reader,boolean lock) {
        return jdbc.query("SELECT * FROM loans WHERE id=? AND reader_id=?"+(lock?" FOR UPDATE":""),LOAN,id,reader).stream().findFirst().orElseThrow(()->new BusinessException(404,"LOAN_NOT_FOUND","借阅不存在"));
    }
    public Loan markReturned(Loan loan) {
        return jdbc.queryForObject("UPDATE loans SET return_time=?,update_time=? WHERE id=? AND return_time IS NULL RETURNING *",LOAN,loan.getReturnTime(),loan.getUpdateTime(),loan.getId());
    }
    public List<CurrentLoanRow> currentLoans(UUID reader,int page,int size) {
        return jdbc.query("SELECT l.*,b.title,b.author FROM loans l JOIN books b ON b.id=l.book_id WHERE l.reader_id=? AND l.return_time IS NULL ORDER BY l.borrow_time DESC,l.id DESC LIMIT ? OFFSET ?",(r,n)->new CurrentLoanRow(loan(r),r.getString("title"),r.getString("author")),reader,size,(long)(page-1)*size);
    }
}
