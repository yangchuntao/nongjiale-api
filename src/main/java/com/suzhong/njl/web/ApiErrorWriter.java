package com.suzhong.njl.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 在 Spring Security 过滤链内直接写错误响应。
 *
 * <p>未认证（401）与权限不足（403）发生在进入 DispatcherServlet 之前，此时
 * {@code @RestControllerAdvice} 还没机会介入，异常处理器接不到。因此这里手工设置
 * 状态码、UTF-8 与 {@code application/json}，再用 ObjectMapper 写出与业务接口一致的
 * {@code ApiResponse} 失败信封，保证前端只需要一套解析逻辑。
 */
@Component
public class ApiErrorWriter {

    private final ObjectMapper objectMapper;

    public ApiErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 写出失败信封。
     *
     * <p>必须显式设置 UTF-8：直接取 {@code getWriter()} 时容器默认编码不一定是 UTF-8，
     * 中文提示会乱码。
     *
     * @param response 当前响应
     * @param status   HTTP 状态码，同时作为信封 code
     * @param message  中文提示
     * @throws IOException 写响应失败
     */
    public void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(status.value(), message));
    }
}
