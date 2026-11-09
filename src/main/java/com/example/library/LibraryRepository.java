package com.example.library;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import static com.example.library.Models.*;

@Repository
public class LibraryRepository {
    private final JdbcTemplate jdbc;
    public LibraryRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    private static UUID uuid(ResultSet r, String key) throws SQLException { return r.getObject(key, UUID.class); }
    private static OffsetDateTime time(ResultSet r, String key) throws SQLException { return r.getObject(key, OffsetDateTime.class); }
    private static final RowMapper<Book> BOOK = (r, n) -> new Book(uuid(r,"id"),r.getString("title"),r.getString("author"),r.getString("description"),r.getInt("total_copies"),r.getInt("available_copies"),time(r,"create_time"),time(r,"update_time"));
    private static Loan loan(ResultSet r) throws SQLException {
        var returned = time(r,"return_time");
        return new Loan(uuid(r,"id"),uuid(r,"book_id"),uuid(r,"reader_id"),returned==null?"BORROWED":"RETURNED",time(r,"borrow_time"),returned,time(r,"update_time"));
    }
    private static final RowMapper<Loan> LOAN = (r,n) -> loan(r);
    /** 新书初始可借库存等于总库存；库存和时间均由服务端初始化。 */
    public Book createBook(CreateBookRequest request) {
        return jdbc.queryForObject("INSERT INTO books(id,title,author,description,total_copies,available_copies) VALUES(?,?,?,?,?,?) RETURNING *",
            BOOK, UUID.randomUUID(),request.title().strip(),request.author().strip(),request.description(),request.totalCopies(),request.totalCopies());
    }
    /** 学生沿用 readers 表，不按姓名去重，允许同名学生。 */
    public Student createStudent(CreateStudentRequest request) {
        return jdbc.queryForObject("INSERT INTO readers(id,name) VALUES(?,?) RETURNING *",
            (r,n)->new Student(uuid(r,"id"),r.getString("name"),time(r,"create_time"),time(r,"update_time")),UUID.randomUUID(),request.name().strip());
    }
    public boolean readerExists(UUID id) { return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM readers WHERE id=?)",Boolean.class,id)); }
    public List<Book> books(int page, int size) { return jdbc.query("SELECT * FROM books ORDER BY create_time DESC,id DESC LIMIT ? OFFSET ?", BOOK,size,(long)(page-1)*size); }
    public Book book(UUID id, boolean lock) {
        return jdbc.query("SELECT * FROM books WHERE id=?"+(lock?" FOR UPDATE":""),BOOK,id).stream().findFirst().orElseThrow(() -> new BusinessException(404,"BOOK_NOT_FOUND","图书不存在"));
    }
    public boolean activeLoan(UUID reader,UUID book) { return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM loans WHERE reader_id=? AND book_id=? AND return_time IS NULL)",Boolean.class,reader,book)); }
    public void lockTimeout() { jdbc.execute("SET LOCAL lock_timeout = '2s'"); }
    public int decrement(UUID book) { return jdbc.update("UPDATE books SET available_copies=available_copies-1,update_time=clock_timestamp() WHERE id=? AND available_copies>0",book); }
    public int increment(UUID book) { return jdbc.update("UPDATE books SET available_copies=available_copies+1,update_time=clock_timestamp() WHERE id=? AND available_copies<total_copies",book); }
    public Loan createLoan(UUID reader,UUID book) {
        // 单次数据库时钟取值，保证 borrow_time 与 update_time 完全一致。
        return jdbc.queryForObject("WITH t AS MATERIALIZED (SELECT clock_timestamp() AS now) INSERT INTO loans(id,reader_id,book_id,borrow_time,update_time) SELECT ?,?,?,t.now,t.now FROM t RETURNING *",LOAN,UUID.randomUUID(),reader,book);
    }
    public Loan ownedLoan(UUID id,UUID reader,boolean lock) {
        return jdbc.query("SELECT * FROM loans WHERE id=? AND reader_id=?"+(lock?" FOR UPDATE":""),LOAN,id,reader).stream().findFirst().orElseThrow(() -> new BusinessException(404,"LOAN_NOT_FOUND","借阅不存在"));
    }
    public Loan markReturned(UUID id) {
        return jdbc.queryForObject("WITH t AS MATERIALIZED (SELECT clock_timestamp() AS now) UPDATE loans SET return_time=t.now,update_time=t.now FROM t WHERE id=? AND return_time IS NULL RETURNING loans.*",LOAN,id);
    }
    public List<CurrentLoan> currentLoans(UUID reader,int page,int size) {
        return jdbc.query("SELECT l.*,b.title,b.author FROM loans l JOIN books b ON b.id=l.book_id WHERE l.reader_id=? AND l.return_time IS NULL ORDER BY l.borrow_time DESC,l.id DESC LIMIT ? OFFSET ?",(r,n)->{
            var l=loan(r); return new CurrentLoan(l.id(),l.bookId(),l.readerId(),l.status(),l.borrowTime(),l.returnTime(),l.updateTime(),new BookRef(l.bookId(),r.getString("title"),r.getString("author")));
        },reader,size,(long)(page-1)*size);
    }
}
