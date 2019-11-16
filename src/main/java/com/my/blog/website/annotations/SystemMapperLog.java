package com.my.blog.website.annotations;

import java.lang.annotation.*;

/**
 * 作用：用于配置需要记录的操作接口
 *
 * @author 文希
 * @create 2019-11-16 21:21
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SystemMapperLog {

    /**
     * 要执行的具体操作比如：添加用户
     **/
//操作详情
    String operation() default "";

    //操作表
    String table() default "";

    //操作列
//查询新旧值使用，非更新不需要填写
    String columns() default "";

    //操作列名
//查询新旧值使用，非更新不需要填写
    String columnsName() default "";

    //操作模块
    String operateObject() default "";

    //参数-拼接操作详情使用
    String param() default "";

    //类型-
    String type() default "";

    //条件-新增时会用到
    String condition() default "";
}
