package com.example.library.controller;


import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.UUID;
import static com.example.library.model.Models.*;

import com.example.library.service.StudentService;

/** 学生录入接口。 */
@RestController
@RequestMapping("/api/v1")
@Validated
public class StudentController {
    private final StudentService service;
    public StudentController(StudentService service) { this.service=service; }

    /** 录入学生。入参 name 必填且最多 100 字，student_number 必填且最多 64 字，学号唯一，允许同名。
     * 返回 201 以及 id、name、student_number、create_time、update_time；学号重复返回 409 STUDENT_NUMBER_ALREADY_EXISTS。
     * 学生沿用 Reader 模型，返回 id 可作为借阅接口的 X-Reader-Id。演示接口无需身份。 */
    @PostMapping("/students/insert")
    @ResponseStatus(HttpStatus.CREATED)
    public Student createStudent(@Valid @RequestBody CreateStudentRequest request) { return service.createStudent(request); }

}
