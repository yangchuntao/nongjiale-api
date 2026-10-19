package com.suzhong.njl.web;

import org.springframework.http.HttpStatus;

/**
 * 业务异常，携带要返回的 HTTP 状态码与中文提示。
 *
 * <p>用于可预期的失败，由 {@code GlobalExceptionHandler} 转成「HTTP 状态码 + 失败信封」，
 * message 会原样给前端展示，因此要写用户能看懂的话。常见语义：
 * <ul>
 *   <li>400：参数不正确、排序或关键词非法；</li>
 *   <li>401：未登录或令牌失效；403：角色不符、未绑定手机号；</li>
 *   <li>404：资源不存在；409：状态冲突或乐观锁失败；</li>
 *   <li>410：门店已下架或所属城市未开通；503：微信登录未配置。</li>
 * </ul>
 *
 * <p>不使用该异常表示程序缺陷，那些情况交给兜底的 500 处理并记录日志。
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    /** 要写入 HTTP 响应的状态码。 */
    public HttpStatus getStatus() {
        return status;
    }
}
