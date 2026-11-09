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

/** Reader 的独立 JDBC 数据访问组件。 */
@Repository
public class ReaderDao {
    private final JdbcTemplate jdbc;
    public ReaderDao(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    private static UUID uuid(ResultSet r,String key) throws SQLException { return r.getObject(key,UUID.class); }
    private static OffsetDateTime time(ResultSet r,String key) throws SQLException { return r.getObject(key,OffsetDateTime.class); }
    private static final RowMapper<Reader> READER=(r,n)->new Reader(uuid(r,"id"),r.getString("name"),r.getString("student_number"),time(r,"create_time"),time(r,"update_time"));
    public Reader createReader(Reader reader) {
        reader.validate();
        return jdbc.queryForObject("INSERT INTO readers(id,name,student_number,create_time,update_time) VALUES(?,?,?,?,?) RETURNING *",READER,
            reader.getId(),reader.getName(),reader.getStudentNumber(),reader.getCreateTime(),reader.getUpdateTime());
    }
    public boolean readerExists(UUID id) { return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM readers WHERE id=?)",Boolean.class,id)); }
}
