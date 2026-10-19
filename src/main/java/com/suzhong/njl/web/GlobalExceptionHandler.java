package com.suzhong.njl.web;

import jakarta.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理，把各类异常映射成「HTTP 状态码 + 失败信封」。
 *
 * <p>映射关系：
 * <ul>
 *   <li>{@code ApiException} → 自带状态码，code 与之相同，message 原样输出；</li>
 *   <li>参数校验/类型不匹配/请求体不可读 → 400；</li>
 *   <li>方法不支持 → 405；无控制器、无静态资源 → 404「接口不存在」；</li>
 *   <li>其余异常 → 500「服务器繁忙」，只记录日志，不把堆栈或 SQL 细节暴露给前端。</li>
 * </ul>
 *
 * <p>Security 过滤链内的 401/403 不经过这里，由 {@code ApiErrorWriter} 直接写响应。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 未知路径的统一提示，与 README 中约定的 404 信封一致。 */
    static final String ROUTE_NOT_FOUND_MESSAGE = "接口不存在";

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常：状态码和提示都由抛出方决定，这里只做信封包装。 */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.fail(ex.getStatus().value(), ex.getMessage()));
    }

    /**
     * Bean Validation 失败：只取第一条字段错误，避免一次返回一长串提示。
     *
     * <p>没有默认消息时退化为「参数不正确」，保证前端始终有可展示文案。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage() == null ? "参数不正确" : error.getDefaultMessage())
                .findFirst()
                .orElse("参数不正确");
        return ResponseEntity.badRequest().body(ApiResponse.fail(HttpStatus.BAD_REQUEST.value(), message));
    }

    /** 路径或查询参数类型转换失败（如 id 传了非数字）统一按 400 处理，不暴露内部类型名。 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(HttpStatus.BAD_REQUEST.value(), "参数不正确"));
    }

    /** 路径存在但 HTTP 方法不对，返回 405 而不是 404，便于前端定位调用方式问题。 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.fail(HttpStatus.METHOD_NOT_ALLOWED.value(), "不支持的请求"));
    }

    /** JSON 语法错误、字段类型不匹配等读取失败，统一提示「请求体格式不正确」。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(HttpStatus.BAD_REQUEST.value(), "请求体格式不正确"));
    }

    /**
     * 没有控制器、也没有静态资源时，Spring 抛出这两个异常。
     * 不能落到 {@link #handleUnexpected}，否则未实现的路径（含支付）会变成 500。
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleMissingRoute(ServletException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(HttpStatus.NOT_FOUND.value(), ROUTE_NOT_FOUND_MESSAGE));
    }

    /** 兜底：记录完整堆栈供排查，响应只给通用文案，避免泄露实现细节。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("未处理异常", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务器繁忙"));
    }
}
