package com.ccweb.babytracker.article;

import com.ccweb.babytracker.article.dto.ArticleRequest;
import com.ccweb.babytracker.article.dto.ArticleResponse;
import com.ccweb.babytracker.domain.article.Article;
import com.ccweb.babytracker.domain.article.ArticleRepository;
import com.ccweb.babytracker.domain.user.User;
import com.ccweb.babytracker.domain.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ArticleService {

    private final ArticleRepository articleRepo;
    private final UserRepository userRepo;

    public ArticleService(ArticleRepository articleRepo, UserRepository userRepo) {
        this.articleRepo = articleRepo;
        this.userRepo = userRepo;
    }

    public ArticleResponse create(UUID userId, ArticleRequest req) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!"ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Article article = new Article();
        article.setTitle(req.title());
        article.setContent(req.content());
        article.setCategory(req.category());
        articleRepo.save(article);

        return toResponse(article);
    }

    @Transactional(readOnly = true)
    public List<ArticleResponse> list() {
        return articleRepo.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ArticleResponse getById(UUID id) {
        return articleRepo.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private ArticleResponse toResponse(Article a) {
        return new ArticleResponse(a.getId(), a.getTitle(), a.getContent(), a.getCategory(), a.getCreatedAt());
    }
}
