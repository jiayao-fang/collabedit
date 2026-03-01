package com.example.collabedit.modules.system.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogOperation {
    String value() default ""; // 操作描述
    String resource() default ""; // 操作资源，如"document"
}