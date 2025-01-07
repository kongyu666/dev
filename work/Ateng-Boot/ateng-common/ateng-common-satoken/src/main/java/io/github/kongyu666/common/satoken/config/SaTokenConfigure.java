package io.github.kongyu666.common.satoken.config;

import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.httpauth.basic.SaHttpBasicUtil;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import io.github.kongyu666.common.core.constant.AppCodeEnum;
import io.github.kongyu666.common.core.factory.YmlPropertySourceFactory;
import io.github.kongyu666.common.core.utils.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 权限认证 配置类
 * https://sa-token.cc/doc.html#/use/at-check
 *
 * @author 孔余
 * @since 2024-05-21 17:17
 */
@AutoConfiguration
@PropertySource(value = "classpath:common-satoken.yml", factory = YmlPropertySourceFactory.class)
public class SaTokenConfigure implements WebMvcConfigurer {
    private static final Logger log = LoggerFactory.getLogger(SaTokenConfigure.class);

    // 注册 Sa-Token 拦截器，打开注解式鉴权功能
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，打开注解式鉴权功能
        registry
                .addInterceptor(
                        new SaInterceptor(
                                // 登录校验
                                handle -> StpUtil.checkLogin()
                        ).isAnnotation(true) // 注解鉴权
                )
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**", "/demo/**");
    }

    /*
    注册 Sa-Token 全局过滤器
     */
    @Bean
    public SaServletFilter getSaServletFilter() {
        return new SaServletFilter()
                .addInclude("/actuator/shutdown")
                .setAuth(obj -> SaHttpBasicUtil.check("admin:Admin@123"))
                .setError(e -> {
                    log.error(e.getMessage());
                    return Result.error(AppCodeEnum.OPERATION_CANCELED.getCode(), AppCodeEnum.OPERATION_CANCELED.getDescription());
                });
    }
}
