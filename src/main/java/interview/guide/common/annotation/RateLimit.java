package interview.guide.common.annotation;

import interview.guide.common.aspect.RateLimitAspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /**
     * rate limit for method
     * @see RateLimitAspect
     */
    enum Dimension{
        GLOBAL,
        IP,
        USER
    }

    Dimension[] dimensions() default {Dimension.GLOBAL};

    /**
     * max request number in the time interval
     * @return max token amount
     */
    double count();


    /**
     * time window size, default is 1
     */
    long interval() default 1;



    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 降级方法名
     * 当限流触发时，调用指定方法进行降级处理
     * 降级方法支持：
     * 1. 无参方法
     * 2. 与原方法参数列表完全一致的方法
     * 降级方法必须在同一个类中，返回值类型与原方法兼容
     * 如果为空字符串，则抛出 RateLimitExceededException 异常
     *
     * @return 降级方法名
     */
    String fallback() default "";


    /**
     * 时间单位枚举
     */
    enum TimeUnit {
        MILLISECONDS, SECONDS, MINUTES, HOURS, DAYS
    }

}
