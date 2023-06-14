package codekoi.apiserver.domain.code.review.domain;

import codekoi.apiserver.domain.model.TimeBaseEntity;
import codekoi.apiserver.domain.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class CodeReview extends TimeBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "code_review_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "code_reviewee_users_id")
    private User user;

    private String title;
    private String content;

    @Enumerated(EnumType.STRING)
    private CodeReviewStatus status;

    @Builder
    private CodeReview(Long id, User user, String title, String content, CodeReviewStatus status) {
        this.id = id;
        this.user = user;
        this.title = title;
        this.content = content;
        this.status = status;
    }
}
