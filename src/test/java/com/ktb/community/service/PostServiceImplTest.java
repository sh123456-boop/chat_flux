package com.ktb.community.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import com.ktb.community.dto.response.PostResponseDto;
import com.ktb.community.dto.response.PostSliceResponseDto;
import com.ktb.community.dto.response.PostSummaryDto;
import com.ktb.community.repository.ImageRepository;
import com.ktb.community.repository.PostRepository;
import com.ktb.community.repository.UserLikePostsRepository;
import com.ktb.community.repository.UserRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @InjectMocks
    private PostServiceImpl postService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private UserRepository userRepository;

    @Mock
    private S3ServiceImpl s3Service;

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private UserLikePostsRepository userLikePostsRepository;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    private static final int PAGE_SIZE = 10;

    @Test
    @DisplayName("게시글 목록 조회(getPostSlice) 단일 스레드 단위 테스트")
    void getPostSlice_singleThread_returnsMappedSlice() {
        // given
        ReflectionTestUtils.setField(postService, "cloudfrontDomain", "test.cloudfront.net");
        ReflectionTestUtils.setField(postService, "defaultProfileImageKey", "default-key");

        User userWithProfile = User.builder()
                .id(1L)
                .email("user1@test.com")
                .password("password")
                .nickname("user1")
                .build();

        Image profileImage = Image.builder()
                .id(101L)
                .s3Key("profiles/user1.png")
                .build();
        userWithProfile.updateProfileImage(profileImage);

        Post firstPost = Post.builder()
                .id(10L)
                .title("First Title")
                .contents("First Content")
                .build();
        firstPost.setUser(userWithProfile);
        ReflectionTestUtils.setField(firstPost.getPostCount(), "view_cnt", 5);
        ReflectionTestUtils.setField(firstPost.getPostCount(), "likes_cnt", 3);
        ReflectionTestUtils.setField(firstPost.getPostCount(), "cmt_cnt", 1);

        User userWithoutProfile = User.builder()
                .id(2L)
                .email("user2@test.com")
                .password("password")
                .nickname("user2")
                .build();

        Post secondPost = Post.builder()
                .id(11L)
                .title("Second Title")
                .contents("Second Content")
                .build();
        secondPost.setUser(userWithoutProfile);
        ReflectionTestUtils.setField(secondPost.getPostCount(), "view_cnt", 2);
        ReflectionTestUtils.setField(secondPost.getPostCount(), "likes_cnt", 1);
        ReflectionTestUtils.setField(secondPost.getPostCount(), "cmt_cnt", 0);

        Slice<Post> mockSlice = new SliceImpl<>(
                List.of(firstPost, secondPost),
                PageRequest.of(0, PAGE_SIZE),
                true
        );
        when(postRepository.findSliceByOrderByIdDesc(PageRequest.of(0, PAGE_SIZE))).thenReturn(mockSlice);

        // when
        PostSliceResponseDto response = postService.getPostSlice(null);

        // then
        assertThat(response).isNotNull();
        assertThat(response.isHasNext()).isTrue();
        assertThat(response.getPosts()).hasSize(2);

        PostSummaryDto summaryFirst = response.getPosts().get(0);
        assertThat(summaryFirst.getPostId()).isEqualTo(10L);
        assertThat(summaryFirst.getTitle()).isEqualTo("First Title");
        assertThat(summaryFirst.getAuthorNickname()).isEqualTo("user1");
        assertThat(summaryFirst.getViewCount()).isEqualTo(5);
        assertThat(summaryFirst.getLikeCount()).isEqualTo(3);
        assertThat(summaryFirst.getCommentCount()).isEqualTo(1);
        assertThat(summaryFirst.getAuthorProfileImageUrl()).isEqualTo("https://test.cloudfront.net/profiles/user1.png");

        PostSummaryDto summarySecond = response.getPosts().get(1);
        assertThat(summarySecond.getPostId()).isEqualTo(11L);
        assertThat(summarySecond.getAuthorNickname()).isEqualTo("user2");
        assertThat(summarySecond.getAuthorProfileImageUrl()).isEqualTo("https://test.cloudfront.net/default-key");

        verify(postRepository).findSliceByOrderByIdDesc(PageRequest.of(0, PAGE_SIZE));
        verifyNoMoreInteractions(postRepository);
    }

//    @Test
//    @DisplayName("게시글 목록 조회(getPostSlice) 동시성 단위 테스트")
//    void getPostSlice() throws InterruptedException{
//
//        // 동시에 요청을 보낼 스레드(사용자) 수
//        int threadCount = 1000;
//
//        // @Value 필드 값 주입
//        ReflectionTestUtils.setField(postService, "cloudfrontDomain", "test.cloudfront.net");
//        ReflectionTestUtils.setField(postService, "defaultProfileImageKey", "default-key");
//
//        // Mock 데이터 생성
//        List<Post> mockPosts = IntStream.range(0, PAGE_SIZE)
//                .mapToObj(i -> {
//                    User user = User.builder().id((long) i).build();
//                    Image image = Image.builder().id((long)i).s3Key("user-profile-" + i).build();
//                    // 리플렉션을 사용해 'image' 라는 이름의 필드에 image 객체를 직접 주입
//                    ReflectionTestUtils.setField(user, "image", image);
//                    return Post.builder().id((long)i).title("Title " + i).user(user).build();
//                })
//                .collect(Collectors.toList());
//
//        // Mock Repository 설정 : 어떤 Pageable이 들어오든 항상 동일한 Slice<Post>를 반환하도록 설정
//        Slice<Post> mockSlice = new SliceImpl<>(mockPosts, PageRequest.of(0, PAGE_SIZE), true);
//        when(postRepository.findSliceByOrderByIdDesc(any(Pageable.class))).thenReturn(mockSlice);
//
//        //when
//        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
//        CountDownLatch latch = new CountDownLatch(threadCount);
//        List<PostSliceResponseDto> results  = Collections.synchronizedList(new ArrayList<>());
//
//
//        for (int i = 0 ; i< threadCount ; i++) {
//            executorService.submit(()->{
//                try {
//                    //서비스 메서드 호출
//                    PostSliceResponseDto result = postService.getPostSlice(null);
//                    results.add(result);
//                } finally {
//                    latch.countDown();
//                }
//            });
//        }
//
//        latch.await(); // 모든 스레드가 작업을 완료할 때까지 대기
//        executorService.shutdown();
//
//        //then
//        // 1. Repository 메서드가 스레드 수만큼 정확히 호출되었는지 확인
//        verify(postRepository, times(threadCount)).findSliceByOrderByIdDesc(any(Pageable.class));
//
//        // 2. 모든 스레드가 결과를 정상적으로 받았는지 확인
//        assertThat(results.size()).isEqualTo(threadCount);
//
//        // 3. 모든 스레드가 받은 결과가 일관성이 있는지 확인 (첫 번째 결과와 마지막 결과를 비교)
//        PostSliceResponseDto firstResult = results.get(0);
//        PostSliceResponseDto lastResult = results.get(threadCount - 1);
//
//        assertThat(firstResult.isHasNext()).isTrue();
//        assertThat(firstResult.getPosts()).hasSize(PAGE_SIZE);
//
//        //DTO 내용 비교( 객체끼리 비교하면 주소값이 달라 실패하므로, 특정 필드 값으로 비교)
//        assertThat(firstResult.getPosts().get(0).getTitle())
//                .isEqualTo(lastResult.getPosts().get(0).getTitle());
//
//        // 생성된 프로필 이미지 URL이 올바른지 확인
//        String expectedProfileUrl = "https://test.cloudfront.net/user-profile-0";
//        assertThat(firstResult.getPosts().get(0).getAuthorProfileImageUrl()).isEqualTo(expectedProfileUrl);
//    }
//
//    @Test
//    @DisplayName("게시글 단건 조회(getPost) 단일 스레드 단위 테스트")
//    void getPost_returnsDetailedDtoAndUpdatesCounters() {
//        // given
//        Long postId = 42L;
//        ReflectionTestUtils.setField(postService, "cloudfrontDomain", "cdn.test.com");
//        ReflectionTestUtils.setField(postService, "defaultProfileImageKey", "default.png");
//
//        User author = User.builder()
//                .id(7L)
//                .email("author@test.com")
//                .password("password123")
//                .nickname("authorNick")
//                .build();
//
//        Image profileImage = Image.builder()
//                .id(3L)
//                .s3Key("profiles/author.png")
//                .user(author)
//                .build();
//        author.updateProfileImage(profileImage);
//
//        Post post = Post.builder()
//                .id(postId)
//                .title("Sample Title")
//                .contents("Sample Content")
//                .build();
//        post.setUser(author);
//
//        PostCount postCount = post.getPostCount();
//        postCount.setPost(post);
//
//        Image postImage = Image.builder()
//                .id(99L)
//                .s3Key("posts/image-1.png")
//                .build();
//        PostImage postImageRelation = PostImage.builder()
//                .post(post)
//                .image(postImage)
//                .orders(1)
//                .build();
//        post.setPostImageList(postImageRelation);
//
//        when(postRepository.findByIdWithPessimisticLock(postId)).thenReturn(Optional.of(post));
//        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
//        when(zSetOperations.incrementScore(anyString(), any(), anyDouble())).thenReturn(1.0);
//        when(redisTemplate.getExpire(anyString())).thenReturn(-1L);
//        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
//        when(userRepository.findById(author.getId())).thenReturn(Optional.of(author));
//        when(userLikePostsRepository.existsByUserAndPost(author, post)).thenReturn(true);
//
//        String expectedDailyKey = "ranking:daily:" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
//        WeekFields weekFields = WeekFields.of(Locale.KOREA);
//        int weekOfYear = LocalDate.now().get(weekFields.weekOfWeekBasedYear());
//        String expectedWeeklyKey = "ranking:weekly:" + LocalDate.now().getYear() + "-W" + String.format("%02d", weekOfYear);
//
//        // when
//        PostResponseDto response = postService.getPost(postId, author.getId());
//
//        // then
//        assertThat(response.getPostId()).isEqualTo(postId);
//        assertThat(response.getTitle()).isEqualTo("Sample Title");
//        assertThat(response.getContent()).isEqualTo("Sample Content");
//        assertThat(response.getNickname()).isEqualTo("authorNick");
//        assertThat(response.getUserId()).isEqualTo(author.getId());
//        assertThat(response.isLikedByUser()).isTrue();
//
//        assertThat(response.getAuthorProfileImageUrl())
//                .isEqualTo("https://cdn.test.com/profiles/author.png");
//        assertThat(response.getImages()).hasSize(1);
//        PostResponseDto.ImageInfo imageInfo = response.getImages().get(0);
//        assertThat(imageInfo.getImageUrl()).isEqualTo("https://cdn.test.com/posts/image-1.png");
//        assertThat(imageInfo.getOrder()).isEqualTo(1);
//        assertThat(imageInfo.getS3_key()).isEqualTo("posts/image-1.png");
//
//        assertThat(post.getPostCount().getView_cnt()).isEqualTo(1);
//        assertThat(response.getViewCount()).isEqualTo(1);
//
//        verify(postRepository).findByIdWithPessimisticLock(postId);
//        verify(redisTemplate).opsForZSet();
//        verify(zSetOperations).incrementScore(expectedDailyKey, postId.toString(), 1.0);
//        verify(zSetOperations).incrementScore(expectedWeeklyKey, postId.toString(), 1.0);
//        verify(redisTemplate).getExpire(expectedDailyKey);
//        verify(redisTemplate).expire(expectedDailyKey, 2L, TimeUnit.DAYS);
//        verify(redisTemplate).getExpire(expectedWeeklyKey);
//        verify(redisTemplate).expire(expectedWeeklyKey, 8L, TimeUnit.DAYS);
//        verify(userRepository).findById(author.getId());
//        verify(userLikePostsRepository).existsByUserAndPost(author, post);
//        verifyNoMoreInteractions(postRepository, redisTemplate, zSetOperations, userLikePostsRepository, userRepository);
//    }
//
//    @Test
//    @DisplayName("게시글 단건 조회(getPost) 동시성 단위 테스트")
//    void getPost_concurrentAccess() throws InterruptedException {
//        Long postId = 99L;
//        int threadCount = 1000;
//        ReflectionTestUtils.setField(postService, "cloudfrontDomain", "cdn.concurrent.com");
//        ReflectionTestUtils.setField(postService, "defaultProfileImageKey", "default.png");
//
//        AtomicInteger viewCounter = new AtomicInteger();
//        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
//        when(zSetOperations.incrementScore(anyString(), any(), anyDouble())).thenReturn(1.0);
//        when(redisTemplate.getExpire(anyString())).thenReturn(-1L);
//        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
//        when(userLikePostsRepository.existsByUserAndPost(any(User.class), any(Post.class))).thenReturn(false);
//        Long viewerId = 1000L;
//        User viewer = User.builder()
//                .id(viewerId)
//                .email("viewer@test.com")
//                .password("password123")
//                .nickname("viewer")
//                .build();
//        when(userRepository.findById(viewerId)).thenReturn(Optional.of(viewer));
//
//        when(postRepository.findByIdWithPessimisticLock(postId)).thenAnswer(invocation -> {
//            User author = User.builder()
//                    .id(7L)
//                    .email("concurrent@test.com")
//                    .password("password123")
//                    .nickname("concurrent")
//                    .build();
//            Image profileImage = Image.builder()
//                    .id(11L)
//                    .s3Key("profiles/concurrent.png")
//                    .user(author)
//                    .build();
//            author.updateProfileImage(profileImage);
//
//            Post post = Post.builder()
//                    .id(postId)
//                    .title("Concurrent Title")
//                    .contents("Concurrent Content")
//                    .build();
//            post.setUser(author);
//
//            Image postImage = Image.builder()
//                    .id(21L)
//                    .s3Key("posts/concurrent.png")
//                    .build();
//            PostImage postImageRelation = PostImage.builder()
//                    .post(post)
//                    .image(postImage)
//                    .orders(1)
//                    .build();
//            post.setPostImageList(postImageRelation);
//
//            PostCount postCount = new PostCount() {
//                @Override
//                public void increaseViewCount() {
//                    int newValue = viewCounter.incrementAndGet();
//                    ReflectionTestUtils.setField(this, "view_cnt", newValue);
//                }
//            };
//            postCount.setPost(post);
//            ReflectionTestUtils.setField(post, "postCount", postCount);
//            return Optional.of(post);
//        });
//
//        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
//        CountDownLatch latch = new CountDownLatch(threadCount);
//        List<PostResponseDto> results = Collections.synchronizedList(new ArrayList<>());
//
//        for (int i = 0; i < threadCount; i++) {
//            executorService.submit(() -> {
//                try {
//                    PostResponseDto response = postService.getPost(postId, viewerId);
//                    results.add(response);
//                } finally {
//                    latch.countDown();
//                }
//            });
//        }
//
//        latch.await();
//        executorService.shutdown();
//
//        assertThat(results).hasSize(threadCount);
//        assertThat(viewCounter.get()).isEqualTo(threadCount);
//        results.forEach(response -> {
//            assertThat(response.getPostId()).isEqualTo(postId);
//            assertThat(response.getAuthorProfileImageUrl()).startsWith("https://cdn.concurrent.com/");
//            assertThat(response.getImages()).hasSize(1);
//        });
//
//        String expectedDailyKey = "ranking:daily:" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
//        WeekFields weekFields = WeekFields.of(Locale.KOREA);
//        int weekOfYear = LocalDate.now().get(weekFields.weekOfWeekBasedYear());
//        String expectedWeeklyKey = "ranking:weekly:" + LocalDate.now().getYear() + "-W" + String.format("%02d", weekOfYear);
//
//        verify(postRepository, times(threadCount)).findByIdWithPessimisticLock(postId);
//        verify(redisTemplate, times(threadCount)).opsForZSet();
//        verify(zSetOperations, times(threadCount)).incrementScore(expectedDailyKey, postId.toString(), 1.0);
//        verify(zSetOperations, times(threadCount)).incrementScore(expectedWeeklyKey, postId.toString(), 1.0);
//        verify(redisTemplate, times(threadCount)).getExpire(expectedDailyKey);
//        verify(redisTemplate, times(threadCount)).expire(expectedDailyKey, 2L, TimeUnit.DAYS);
//        verify(redisTemplate, times(threadCount)).getExpire(expectedWeeklyKey);
//        verify(redisTemplate, times(threadCount)).expire(expectedWeeklyKey, 8L, TimeUnit.DAYS);
//        verify(userLikePostsRepository, times(threadCount)).existsByUserAndPost(any(User.class), any(Post.class));
//    }

}
