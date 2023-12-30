package team.rescue.fridge.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import team.rescue.fridge.notification.Notification;
import team.rescue.fridge.review.entity.Cook;
import team.rescue.fridge.review.entity.Review;

@Entity
@Table(name = "member")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Member {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "member_id")
	private Long id;

	@Column(name = "name", nullable = false, length = 15)
	private String name;

	@Column(name = "nickname", nullable = false, length = 15)
	private String nickname;

	@Column(name = "email", unique = true, nullable = false, length = 50)
	private String email;

	@Column(name = "password", nullable = false, length = 100)
	private String password;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 10)
	private RoleType role;

	@Enumerated(EnumType.STRING)
	@Column(name = "provider", nullable = false, length = 10)
	private ProviderType provider;

	@Column(name = "provider_id", length = 100)
	private String providerId;

	// Email 가입 시 인증 코드
	@Column(name = "email_code", length = 10)
	private String emailCode;

	@Column(name = "jwt_token")
	private String token;

	// 알림 조회
	@OneToMany(mappedBy = "member")
	private final List<Notification> notificationList = new ArrayList<>();

	// 요리 완료 조회
	@OneToMany(mappedBy = "member")
	private final List<Cook> cookList = new ArrayList<>();

	// 레시피 후기 조회
	@OneToMany(mappedBy = "member")
	private final List<Review> reviewList = new ArrayList<>();

	@CreatedDate
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "modified_at")
	private LocalDateTime modifiedAt;

	public void updateEmailCode(String emailCode) {
		this.emailCode = emailCode;
	}
}
