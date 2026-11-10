package com.smartticket.kb.repository;

import com.smartticket.kb.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    List<Article> findByCategory(String category);

    List<Article> findByTitleContaining(String keyword);
}
