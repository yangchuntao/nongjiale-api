package com.nongjiale.repository;

import com.nongjiale.domain.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    Page<Article> findByStatusOrderByPublishedAtDesc(String status, Pageable pageable);
    Page<Article> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    Page<Article> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
