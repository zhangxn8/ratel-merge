package com.ratel.merge.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author zhangxn
 * @date 2021/12/27  23:00
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value={ElementType.METHOD,ElementType.TYPE,ElementType.FIELD})
public @interface MergeField {
    /**
     * 目标类
     */
    Class<? extends Object> feign() default Object.class;

    /**
     * 调用方法
     */
    String method() default "";

    /**
     * 聚合之后显示转换的字段名称
     */
    String mergeName() default "";

    /**
     * 聚合群组
     */
    String group() default "";
}
