package com.ktb.community.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@Table(name = "users")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User extends Timestamped{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;


    @NotBlank // null, 빈문자열, 공백 문자열 모두 허용하지 않음
    @Email // 이메일 형식 검증
    @Size(min = 6, max = 254)
    @Column(nullable = false, unique = true) // DB 레벨에서 NOT NULL, UNIQUE 제약조건 추가
    private String email;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String password;

    @NotBlank
    @Setter
    @Size(min = 2, max = 10)
    @Column(nullable = false, unique = true)
    private String nickname;

    @Enumerated(EnumType.STRING)
    private Role role;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private OauthUser oauthUser;

    // 유저를 삭제하면 관련 image 삭제, 이미지를 바꾸면 기존 이미지 삭제
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Image image;

    // Post와의 일대다 관계 설정
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Post> postList = new ArrayList<>();

    // 한 명의 유저는 여러 '좋아요'를 누를 수 있음 (OneToMany)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<UserLikePosts> likesList = new ArrayList<>();

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updatePassword(String newEncodedPassword) {
        this.password = newEncodedPassword;
    }

    public void updateProfileImage(Image image) {
        this.image = image;
    }
}