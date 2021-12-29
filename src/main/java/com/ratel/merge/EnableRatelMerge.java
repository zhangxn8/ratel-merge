package com.ratel.merge;

import com.ratel.merge.core.MergeConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/***
 * 配置加载开关类
 * @author zhangxn
 * @date 2021/12/30 0:22
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import({MergeConfig.class})
public @interface EnableRatelMerge {
}
