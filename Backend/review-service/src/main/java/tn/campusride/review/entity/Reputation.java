
package tn.campusride.review.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "reputations")
public class Reputation {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(
            name = "average_rating",
            nullable = false,
            precision = 3,
            scale = 2
    )
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "review_count", nullable = false)
    private Integer reviewCount = 0;

    public Reputation() {
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(BigDecimal averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }
}
