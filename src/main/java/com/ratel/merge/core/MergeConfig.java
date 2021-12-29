package com.ratel.merge.core;

import com.ratel.merge.utils.MergeUtil;
import com.ratel.merge.utils.SpringUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author zhangxn
 * @date 2021/12/27  23:22
 */
@Configuration
@ComponentScan("com.ratel.merge.core")
@ConditionalOnProperty(name = "ratel.merge.enabled", matchIfMissing = false)
public class MergeConfig {

    @Bean
    @ConditionalOnMissingBean
    public SpringUtil springUtil() {
        return new SpringUtil();
    }


    @Bean
    @ConditionalOnMissingBean
    public MergeUtil mergeUtil() {
        return new MergeUtil();
    }


}
