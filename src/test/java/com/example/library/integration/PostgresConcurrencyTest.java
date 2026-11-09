package com.example.library.integration;

import com.example.library.service.BookService;
import com.example.library.service.StudentService;
import com.example.library.service.LoanService;
import com.example.library.exception.BusinessException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.IntFunction;
import static org.junit.jupiter.api.Assertions.*;
import static com.example.library.model.Models.*;

/** 使用专用 PostgreSQL 测试库和独立事务；无数据库时明确跳过，不用 H2 模拟行锁。 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named="TEST_DB_URL",matches=".+")
class PostgresConcurrencyTest {
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",()->System.getenv("TEST_DB_URL"));
        r.add("spring.datasource.username",()->System.getenv().getOrDefault("TEST_DB_USER","library"));
        r.add("spring.datasource.password",()->System.getenv().getOrDefault("TEST_DB_PASSWORD","library"));
    }
    @Autowired BookService books;
    @Autowired StudentService students;
    @Autowired LoanService loans;
    @Autowired JdbcTemplate jdbc;
    UUID book;
    List<UUID> readers;
    @BeforeEach void setup() {
        book=UUID.randomUUID(); readers=new ArrayList<>();
        jdbc.update("INSERT INTO books(id,title,author,total_copies,available_copies) VALUES(?,'test','author',1,1)",book);
        for(int i=0;i<20;i++) { var id=UUID.randomUUID();readers.add(id);jdbc.update("INSERT INTO readers(id,name,student_number) VALUES(?,'test',?)",id,id.toString()); }
    }
    @AfterEach void cleanup() {
        jdbc.update("DELETE FROM loans WHERE book_id=?",book); jdbc.update("DELETE FROM books WHERE id=?",book);
        readers.forEach(id->jdbc.update("DELETE FROM readers WHERE id=?",id));
    }
    List<Object> parallel(IntFunction<Object> action) throws Exception {
        var gate=new CyclicBarrier(20);
        try(var pool=Executors.newFixedThreadPool(20)) {
            var futures=new ArrayList<Future<Object>>();
            for(int i=0;i<20;i++) { final int index=i;futures.add(pool.submit(()->{
                gate.await(10,TimeUnit.SECONDS);
                try { return action.apply(index); } catch(BusinessException e) { return e.getCode(); }
            })); }
            var results=new ArrayList<Object>();
            for(var f:futures) results.add(f.get(20,TimeUnit.SECONDS));
            return results;
        }
    }
    void stock(int expected) {
        assertEquals(expected,jdbc.queryForObject("SELECT available_copies FROM books WHERE id=?",Integer.class,book));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM books b WHERE id=? AND available_copies<>total_copies-(SELECT count(*) FROM loans WHERE book_id=b.id AND return_time IS NULL)",Integer.class,book));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM (SELECT reader_id FROM loans WHERE book_id=? AND return_time IS NULL GROUP BY reader_id HAVING count(*)>1) t",Integer.class,book));
    }
    @Test void lastCopyOnlyOneWinner() throws Exception {
        var results=parallel(i->loans.borrow(readers.get(i),book));
        assertEquals(1,results.stream().filter(Loan.class::isInstance).count());
        assertEquals(19,results.stream().filter("BOOK_OUT_OF_STOCK"::equals).count());stock(0);
    }
    @Test void sameReaderOnlyBorrowsOnce() throws Exception {
        jdbc.update("UPDATE books SET total_copies=20,available_copies=20 WHERE id=?",book);
        var results=parallel(i->loans.borrow(readers.getFirst(),book));
        assertEquals(1,results.stream().filter(Loan.class::isInstance).count());
        assertEquals(19,results.stream().filter("BOOK_ALREADY_BORROWED"::equals).count());stock(19);
    }
    @Test void parallelReturnOnlyReleasesOneCopy() throws Exception {
        var loan=loans.borrow(readers.getFirst(),book);
        var results=parallel(i->loans.returnBook(readers.getFirst(),loan.id()));
        assertTrue(results.stream().allMatch(Loan.class::isInstance));
        assertEquals(1,results.stream().map(v->((Loan)v).returnTime()).distinct().count());stock(1);
        assertEquals("BORROWED",loans.borrow(readers.getFirst(),book).status());stock(0);
    }
    @Test void insertFailureRollsBackStock() {
        // 真实 FK 错误发生于扣库存之后，整笔事务必须回滚。
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->loans.borrow(UUID.randomUUID(),book));
        stock(1);
    }
    @Test void returnStockFailureRollsBackLoan() {
        var loan=loans.borrow(readers.getFirst(),book);
        // 注入跨表不一致，使库存增加失败。归还标记必须随事务回滚。
        jdbc.update("UPDATE books SET available_copies=total_copies WHERE id=?",book);
        assertThrows(IllegalStateException.class,()->loans.returnBook(readers.getFirst(),loan.id()));
        assertNull(jdbc.queryForObject("SELECT return_time FROM loans WHERE id=?",java.time.OffsetDateTime.class,loan.id()));
        jdbc.update("UPDATE books SET available_copies=0 WHERE id=?",book);stock(0);
    }
    @Test void otherReaderCannotReturnLoan() {
        var loan=loans.borrow(readers.getFirst(),book);
        var e=assertThrows(BusinessException.class,()->loans.returnBook(readers.get(1),loan.id()));
        assertEquals("LOAN_NOT_FOUND",e.getCode());stock(0);
    }
    @Test void enteredStudentAndBookCanBorrowAndReturn() {
        var student=students.createStudent(new CreateStudentRequest("  新学生  ","  "+UUID.randomUUID()+"  "));
        readers.add(student.id());
        var created=books.createBook(new CreateBookRequest("  Java 入门  ","作者",null,2));
        try {
            assertEquals("新学生",student.name());
            assertEquals(student.createTime(),student.updateTime());
            assertEquals("Java 入门",created.title());
            assertEquals(2,created.availableCopies());
            assertEquals(created.createTime(),created.updateTime());
            assertEquals(student.id(),students.authenticate(student.id().toString()));
            var loan=loans.borrow(student.id(),created.id());
            assertEquals(1,books.book(created.id()).availableCopies());
            assertEquals(1,loans.current(student.id(),1,20).items().size());
            assertEquals("RETURNED",loans.returnBook(student.id(),loan.id()).status());
            assertEquals(2,books.book(created.id()).availableCopies());
            assertTrue(loans.current(student.id(),1,20).items().isEmpty());
        } finally {
            jdbc.update("DELETE FROM loans WHERE book_id=?",created.id());
            jdbc.update("DELETE FROM books WHERE id=?",created.id());
        }
    }
    @Test void studentNumberIsUniqueButNamesMayRepeat() {
        String number=UUID.randomUUID().toString();
        var first=students.createStudent(new CreateStudentRequest("同名学生",number));
        readers.add(first.id());
        assertEquals(number,first.studentNumber());
        assertThrows(org.springframework.dao.DuplicateKeyException.class,
            ()->students.createStudent(new CreateStudentRequest("另一学生","  "+number+"  ")));
        var second=students.createStudent(new CreateStudentRequest("同名学生",UUID.randomUUID().toString()));
        readers.add(second.id());
        assertNotEquals(first.id(),second.id());
    }
}
