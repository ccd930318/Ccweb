package com.ccweb.babytracker.article;

import com.ccweb.babytracker.article.dto.ArticleRequest;
import com.ccweb.babytracker.article.dto.ArticleResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ArticleResponse create(@Valid @RequestBody ArticleRequest req, Authentication auth) {
        return articleService.create(UUID.fromString(auth.getName()), req);
    }

    @GetMapping
    public List<ArticleResponse> list(Authentication auth) {
        return articleService.list();
    }

    @GetMapping("/{id}")
    public ArticleResponse getById(@PathVariable UUID id, Authentication auth) {
        return articleService.getById(id);
    }
}
