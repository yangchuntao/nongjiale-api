package com.suzhong.njl.web.admin;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.domain.enums.ArticleStatus;
import com.suzhong.njl.service.admin.AdminArticleService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.admin.dto.ArticleAdminResponse;
import com.suzhong.njl.web.admin.dto.ArticleWriteRequest;

import jakarta.validation.Valid;

/**
 * 运营端攻略管理接口（{@code /api/admin/articles}）。
 *
 * <p>所属端：运营后台。除登录外的 {@code /api/admin/**} 均要求
 * {@code Authorization: Bearer <token>}，角色为 {@code OPERATOR} 或 {@code AGENT}。
 *
 * <p>职责：攻略的列表、详情、新建、全量更新与物理删除。列表可按 {@code DRAFT}/{@code PUBLISHED}
 * 筛选；只有已发布攻略会进入门户。写操作由服务层维护发布时间并失效攻略/首页缓存。
 */
@RestController
@RequestMapping("/api/admin/articles")
public class AdminArticleController {

    private final AdminArticleService adminArticleService;

    public AdminArticleController(AdminArticleService adminArticleService) {
        this.adminArticleService = adminArticleService;
    }

    /**
     * {@code GET /api/admin/articles} — 分页查询攻略，含草稿。
     *
     * <p>按更新时间倒序、ID 倒序，保证翻页稳定；{@code status} 为空时草稿与已发布都返回。
     *
     * @param status   发布状态筛选，取值 {@code DRAFT} 或 {@code PUBLISHED}，为空不筛选
     * @param page     页码，从 1 开始，为空取 1
     * @param pageSize 每页条数，1 到 100，为空取 20；越界返回 400「分页参数不正确」
     * @return 当页攻略、总数、页码与页大小
     */
    @GetMapping
    public ApiResponse<PageResult<ArticleAdminResponse>> list(
            @RequestParam(required = false) ArticleStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return ApiResponse.ok(adminArticleService.list(status, page, pageSize));
    }

    /**
     * {@code POST /api/admin/articles} — 新建攻略。
     *
     * <p>字段校验：标题必填且不超过 200 字，封面不超过 512 字，摘要不超过 500 字，状态必填；
     * 以 {@code PUBLISHED} 创建时立即写入发布时间，创建人取当前运营账号。
     *
     * @param authentication 当前运营身份，用于取账号 ID 写入创建人
     * @param request        攻略内容与发布状态；多传未知字段会被拒绝
     * @return 新建后的攻略详情
     * @throws com.suzhong.njl.web.ApiException 字段校验失败 400；{@code regionId} 查不到 400「地区不存在」
     */
    @PostMapping
    public ApiResponse<ArticleAdminResponse> create(
            Authentication authentication,
            @Valid @RequestBody ArticleWriteRequest request) {
        return ApiResponse.ok(adminArticleService.create(AdminPrincipal.adminId(authentication), request));
    }

    /**
     * {@code GET /api/admin/articles/{id}} — 攻略详情，草稿也可查看。
     *
     * @param id 攻略 ID
     * @return 攻略完整字段，包括正文、状态与发布时间
     * @throws com.suzhong.njl.web.ApiException 攻略不存在 404「攻略不存在」
     */
    @GetMapping("/{id}")
    public ApiResponse<ArticleAdminResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(adminArticleService.detail(id));
    }

    /**
     * {@code PUT /api/admin/articles/{id}} — 全量更新攻略。
     *
     * <p>按请求体覆盖标题、封面、摘要、正文、状态与地区；只有从非发布态切到
     * {@code PUBLISHED} 时才补写发布时间，已发布日期保持不变。
     *
     * @param id      攻略 ID
     * @param request 攻略内容与发布状态，字段规则同新建
     * @return 更新后的攻略详情
     * @throws com.suzhong.njl.web.ApiException 攻略不存在 404；字段校验失败或地区不存在 400
     */
    @PutMapping("/{id}")
    public ApiResponse<ArticleAdminResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ArticleWriteRequest request) {
        return ApiResponse.ok(adminArticleService.update(id, request));
    }

    /**
     * {@code DELETE /api/admin/articles/{id}} — 物理删除攻略。
     *
     * <p>不做软删除；删除成功后失效攻略与首页缓存，门户直链随即不可访问。
     *
     * @param id 攻略 ID
     * @return 固定结构 {@code {"id": 攻略ID}}
     * @throws com.suzhong.njl.web.ApiException 攻略不存在 404「攻略不存在」
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Long>> delete(@PathVariable Long id) {
        adminArticleService.delete(id);
        return ApiResponse.ok(Map.of("id", id));
    }
}
