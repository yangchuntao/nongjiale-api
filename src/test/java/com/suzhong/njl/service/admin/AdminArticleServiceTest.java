package com.suzhong.njl.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.suzhong.njl.domain.entity.Article;
import com.suzhong.njl.domain.entity.Region;
import com.suzhong.njl.domain.enums.ArticleStatus;
import com.suzhong.njl.mapper.ArticleMapper;
import com.suzhong.njl.mapper.RegionMapper;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.PageResult;
import com.suzhong.njl.web.admin.dto.ArticleAdminResponse;
import com.suzhong.njl.web.admin.dto.ArticleWriteRequest;

@ExtendWith(MockitoExtension.class)
class AdminArticleServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-02T03:04:05Z"), ZoneId.of("Asia/Shanghai"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 2, 11, 4, 5);

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private RegionMapper regionMapper;

    @Mock
    private AdminCache adminCache;

    private AdminArticleService service;

    @BeforeEach
    void setUp() {
        service = new AdminArticleService(articleMapper, regionMapper, adminCache, CLOCK);
    }

    @Test
    void listMapsRowsAndTreatsAMissingCountAsZero() {
        Article article = article(4L, ArticleStatus.DRAFT, null);
        when(articleMapper.selectCount(any())).thenReturn(null);
        when(articleMapper.selectList(any())).thenReturn(List.of(article));

        PageResult<ArticleAdminResponse> page = service.list(ArticleStatus.DRAFT, null, null);

        assertThat(page.total()).isZero();
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.pageSize()).isEqualTo(20);
        assertThat(page.items()).hasSize(1);
        assertThat(page.items().get(0).title()).isEqualTo("周末去哪");
        assertThat(page.items().get(0).status()).isEqualTo(ArticleStatus.DRAFT);
    }

    @Test
    void detailAndDeleteRejectAMissingArticle() {
        when(articleMapper.selectById(4L)).thenReturn(null);
        assertThatThrownBy(() -> service.detail(4L))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        when(articleMapper.deleteById(4L)).thenReturn(0);
        assertThatThrownBy(() -> service.delete(4L))
                .isInstanceOf(ApiException.class)
                .hasMessage("攻略不存在");
        verify(adminCache, never()).evictArticles();
    }

    @Test
    void createPublishesWithClockAndRejectsAnUnknownRegion() {
        when(regionMapper.selectById(9L)).thenReturn(null);
        ArticleWriteRequest missing = request(ArticleStatus.PUBLISHED, 9L);
        assertThatThrownBy(() -> service.create(3L, missing))
                .isInstanceOf(ApiException.class)
                .hasMessage("地区不存在");

        AtomicReference<Article> stored = new AtomicReference<>();
        when(articleMapper.insert(any(Article.class))).thenAnswer(invocation -> {
            Article row = invocation.getArgument(0);
            row.setId(row.getStatus() == ArticleStatus.PUBLISHED ? 8L : 9L);
            stored.set(row);
            return 1;
        });
        when(articleMapper.selectById(any())).thenAnswer(invocation -> stored.get());

        ArticleAdminResponse published = service.create(3L, request(ArticleStatus.PUBLISHED, null));
        assertThat(published.id()).isEqualTo(8L);
        assertThat(published.publishedAt()).isEqualTo(NOW);
        assertThat(published.createdBy()).isEqualTo(3L);
        verify(adminCache).evictArticles();

        ArticleAdminResponse draft = service.create(3L, request(ArticleStatus.DRAFT, null));
        assertThat(draft.publishedAt()).isNull();
        assertThat(draft.status()).isEqualTo(ArticleStatus.DRAFT);
        assertThat(draft.id()).isEqualTo(9L);
    }

    @Test
    void updateStampsPublishedAtOnlyOnFirstPublish() {
        Article article = article(8L, ArticleStatus.DRAFT, null);
        when(articleMapper.selectById(8L)).thenReturn(article);
        when(regionMapper.selectById(2L)).thenReturn(new Region());

        ArticleAdminResponse published = service.update(8L, request(ArticleStatus.PUBLISHED, 2L));

        assertThat(published.publishedAt()).isEqualTo(NOW);
        assertThat(published.regionId()).isEqualTo(2L);
        verify(articleMapper).updateById(article);
        verify(adminCache).evictArticles();

        ArticleAdminResponse stillPublished = service.update(8L, request(ArticleStatus.PUBLISHED, 2L));
        assertThat(stillPublished.publishedAt()).isEqualTo(NOW);
    }

    @Test
    void deleteEvictsTheArticleCache() {
        when(articleMapper.deleteById(8L)).thenReturn(1);

        service.delete(8L);

        verify(adminCache).evictArticles();
    }

    private static Article article(Long id, ArticleStatus status, LocalDateTime publishedAt) {
        Article article = new Article();
        article.setId(id);
        article.setTitle("周末去哪");
        article.setCoverUrl(null);
        article.setSummary("摘要");
        article.setBody("正文");
        article.setStatus(status);
        article.setRegionId(null);
        article.setPublishedAt(publishedAt);
        article.setCreatedBy(3L);
        return article;
    }

    private static ArticleWriteRequest request(ArticleStatus status, Long regionId) {
        return new ArticleWriteRequest("周末去哪", null, "摘要", "正文", status, regionId);
    }
}
