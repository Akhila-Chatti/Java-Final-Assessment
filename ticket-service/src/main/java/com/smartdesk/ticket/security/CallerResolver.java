package com.smartdesk.ticket.security;

import com.smartdesk.ticket.entity.Department;
import com.smartdesk.ticket.exception.ApiException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.UUID;

/** Lets any controller method declare a {@link Caller} parameter. */
@Configuration
public class CallerResolver implements HandlerMethodArgumentResolver, WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(this);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == Caller.class;
    }

    @Override
    public Caller resolveArgument(MethodParameter p, ModelAndViewContainer m, NativeWebRequest request, WebDataBinderFactory f) {
        String id = request.getHeader("X-User-Id");
        String role = request.getHeader("X-User-Role");
        if (id == null || role == null) throw ApiException.unauthorized("Call through the API gateway");
        String dept = request.getHeader("X-User-Dept");
        return new Caller(UUID.fromString(id), request.getHeader("X-User-Email"), request.getHeader("X-User-Name"),
                Role.valueOf(role), dept == null ? null : Department.valueOf(dept));
    }
}
