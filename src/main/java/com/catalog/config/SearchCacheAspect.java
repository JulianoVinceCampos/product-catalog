package com.catalog.config;

import com.catalog.dto.request.ProductSearchFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class SearchCacheAspect {

    private static final String PREFIX = "search::";
    private static final Duration TTL  = Duration.ofSeconds(60);

    private final RedisTemplate<String, Object> redisTemplate;

    @Around("execution(* com.catalog.service.ProductService.search(..)) && args(filter)")
    public Object cacheSearch(ProceedingJoinPoint pjp, ProductSearchFilter filter) throws Throwable {
        if (filter.isHighPage()) return pjp.proceed();
        String key = buildKey(filter);
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) { log.debug("Cache HIT: {}", key); return cached; }
        } catch (Exception e) { log.warn("Redis read error: {}", e.getMessage()); return pjp.proceed(); }
        log.debug("Cache MISS: {}", key);
        Object result = pjp.proceed();
        try { redisTemplate.opsForValue().set(key, result, TTL); } catch (Exception e) { log.warn("Redis write error: {}", e.getMessage()); }
        return result;
    }

    private String buildKey(ProductSearchFilter f) {
        return PREFIX + s(f.getQ()) + ":" + s(f.getCategory()) + ":" + s(f.getMinPrice()) + ":"
               + s(f.getMaxPrice()) + ":" + s(f.getStatus()) + ":" + s(f.getSort()) + ":"
               + s(f.getOrder()) + ":" + f.getPage() + ":" + f.getSize();
    }

    private String s(Object v) { return v != null ? v.toString() : "_"; }
}
