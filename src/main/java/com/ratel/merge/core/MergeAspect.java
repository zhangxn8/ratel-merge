package com.ratel.merge.core;

import com.ratel.merge.utils.MergeUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author zhangxn
 * @date 2021/12/27  23:10
 */
@Aspect
@Component
@ConditionalOnProperty(name = "ratel.merge.enabled", matchIfMissing = false)
@Slf4j
public class MergeAspect {

    @Autowired
    private MergeUtil mergeUtil;

    @Pointcut("@annotation(com.ratel.merge.annotation.MergeData)")
    public void methodPointcut() {

    }


    @Around("methodPointcut()")
    public Object interceptor(ProceedingJoinPoint pjp) throws Throwable {
        try {
            return mergeUtil.mergeData(pjp);
        }catch(Exception e){
            return pjp.proceed();
        }
    }



}
