package com.example.library.dto.persistence;

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

/** Book 的独立 JDBC 数据访问组件。 */
@Repository
public class BookDao {
    private final JdbcTemplate jdbc;
    public BookDao(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    private static UUID uuid(ResultSet r,String key) throws SQLException { return r.getObject(key,UUID.class); }
    private static OffsetDateTime time(ResultSet r,String key) throws SQLException { return r.getObject(key,OffsetDateTime.class); }
    private static final RowMapper<Book> BOOK=(r,n)->new Book(uuid(r,"id"),r.getString("title"),r.getString("author"),r.getString("description"),r.getInt("total_copies"),r.getInt("available_copies"),time(r,"create_time"),time(r,"update_time"));
    public Book createBook(Book book) {
        book.validate();
        return jdbc.queryForObject("INSERT INTO books(id,title,author,description,total_copies,available_copies,create_time,update_time) VALUES(?,?,?,?,?,?,?,?) RETURNING *",BOOK,
            book.getId(),book.getTitle(),book.getAuthor(),book.getDescription(),book.getTotalCopies(),book.getAvailableCopies(),book.getCreateTime(),book.getUpdateTime());
    }
    public List<Book> books(int page,int size) { return jdbc.query("SELECT * FROM books ORDER BY create_time DESC,id DESC LIMIT ? OFFSET ?",BOOK,size,(long)(page-1)*size); }
    public Book book(UUID id,boolean lock) {
        return jdbc.query("SELECT * FROM books WHERE id=?"+(lock?" FOR UPDATE":""),BOOK,id).stream().findFirst().orElseThrow(()->new BusinessException(404,"BOOK_NOT_FOUND","图书不存在"));
    }
    public int decrement(Book updated) {
        updated.validate();
        return jdbc.update("UPDATE books SET available_copies=?,update_time=? WHERE id=? AND available_copies=? AND available_copies>0",updated.getAvailableCopies(),updated.getUpdateTime(),updated.getId(),updated.getAvailableCopies()+1);
    }
    public int increment(Book updated) {
        updated.validate();
        return jdbc.update("UPDATE books SET available_copies=?,update_time=? WHERE id=? AND available_copies=? AND available_copies<total_copies",updated.getAvailableCopies(),updated.getUpdateTime(),updated.getId(),updated.getAvailableCopies()-1);
    }
}
