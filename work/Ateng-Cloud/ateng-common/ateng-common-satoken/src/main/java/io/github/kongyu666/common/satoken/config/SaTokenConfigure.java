package io.github.kongyu666.common.satoken.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.httpauth.basic.SaHttpBasicUtil;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.same.SaSameUtil;
import cn.dev33.satoken.stp.StpUtil;
import io.github.kongyu666.common.core.constant.AppCodeEnum;
import io.github.kongyu666.common.core.factory.YmlPropertySourceFactory;
import io.github.kongyu666.common.core.utils.Result;
import io.github.kongyu666.common.core.utils.SpringUtils;
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

    /**
     * 注册 Sa-Token 拦截器，打开注解式鉴权功能
     * https://sa-token.cc/doc.html#/use/route-check
     */
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
    校验是否从网关转发
     */
    @Bean
    public SaServletFilter getSaServletFilter() {
        String username = SpringUtils.getProperty("spring.cloud.nacos.username");
        String password = SpringUtils.getProperty("spring.cloud.nacos.password");
        return new SaServletFilter()
                .addInclude("/**")
                .addExclude("/demo/**")
                .setAuth(obj -> {
                    // 对 /actuator/shutdown 接口做基础鉴权
                    SaRouter
                            .match("/actuator/shutdown")
                            .check(() -> SaHttpBasicUtil.check(username + ":" + password));
                    // 校验 Same-Token 身份凭证，以下两句代码可简化为：SaSameUtil.checkCurrentRequestToken();
                    String token = SaHolder.getRequest().getHeader(SaSameUtil.SAME_TOKEN);
                    SaRouter
                            .notMatch("/actuator/**")
                            .match("/**", () -> SaSameUtil.checkToken(token));
                })
                .setError(e -> {
                    log.error(e.getMessage());
                    return Result.failure(AppCodeEnum.OPERATION_CANCELED.getCode(), AppCodeEnum.OPERATION_CANCELED.getDescription()).withData("不允许直接访问微服务，只能通过网关访问！");
                });
    }
}
