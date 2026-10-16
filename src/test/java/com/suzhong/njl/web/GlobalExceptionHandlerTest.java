package com.suzhong.njl.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void apiExceptionKeepsItsStatus() {
        var response = handler.handleApi(new ApiException(HttpStatus.CONFLICT, "不能从待跟进改为已入住"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo(409);
        assertThat(response.getBody().message()).isEqualTo("不能从待跟进改为已入住");
    }

    @Test
    void validationUsesTheFirstFieldMessage() throws Exception {
        Method method = Probe.class.getDeclaredMethod("probe", String.class);
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Probe(), "probe");
        binding.addError(new FieldError("probe", "phone", "手机号不能为空"));
        binding.addError(new FieldError("probe", "password", null));

        var response = handler.handleValidation(new MethodArgumentNotValidException(new MethodParameter(method, 0), binding));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("手机号不能为空");
    }

    @Test
    void missingFieldMessageAndEmptyErrorsUseTheFallback() throws Exception {
        Method method = Probe.class.getDeclaredMethod("probe", String.class);
        BeanPropertyBindingResult blank = new BeanPropertyBindingResult(new Probe(), "probe");
        blank.addError(new FieldError("probe", "phone", null));
        var missing = handler.handleValidation(new MethodArgumentNotValidException(new MethodParameter(method, 0), blank));
        assertThat(missing.getBody().message()).isEqualTo("参数不正确");

        BeanPropertyBindingResult empty = new BeanPropertyBindingResult(new Probe(), "probe");
        var none = handler.handleValidation(new MethodArgumentNotValidException(new MethodParameter(method, 0), empty));
        assertThat(none.getBody().message()).isEqualTo("参数不正确");
    }

    @Test
    void typeMethodAndBodyErrorsStayClientErrors() {
        var type = handler.handleTypeMismatch(
                new MethodArgumentTypeMismatchException("x", Integer.class, "page", null, new IllegalArgumentException("x")));
        var method = handler.handleMethodNotAllowed(new HttpRequestMethodNotSupportedException("PATCH"));
        var body = handler.handleUnreadable(new HttpMessageNotReadableException("bad", new IllegalArgumentException("bad"), null));

        assertThat(type.getBody().message()).isEqualTo("参数不正确");
        assertThat(method.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(method.getBody().message()).isEqualTo("不支持的请求");
        assertThat(body.getBody().message()).isEqualTo("请求体格式不正确");
    }

    @Test
    void unexpectedErrorsDoNotLeakTheCause() {
        var response = handler.handleUnexpected(new IllegalStateException("secret"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).isEqualTo("服务器繁忙");
        assertThat(response.getBody().message()).doesNotContain("secret");
    }

    static final class Probe {
        @SuppressWarnings("unused")
        void probe(String phone) {
        }
    }
}
