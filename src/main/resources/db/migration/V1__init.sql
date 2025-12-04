CREATE DATABASE IF NOT EXISTS board
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE board;

CREATE TABLE users (
  `user_id` BIGINT NOT NULL AUTO_INCREMENT primary key,
  `email`    VARCHAR(254) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `nickname` VARCHAR(20)  NOT NULL,
  `role` ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uq_users_email` (`email`),
  UNIQUE KEY `uq_users_nickname` (`nickname`)
  );

create table images (
	image_id bigint not null auto_increment primary key,
	s3_key varchar(512) not null,
	user_id bigint null,
	constraint fk_images_user
		foreign key(user_id) references users(user_id)
		on delete cascade
);

create table posts (
	post_id bigint not null auto_increment primary key,
	title varchar(100) not null,
	contents text not null,
	created_at datetime not null default current_timestamp,
	updated_at datetime not null default current_timestamp on update current_timestamp,
	user_id bigint not null,
	constraint fk_posts_user
		foreign key(user_id) references users(user_id)
		on delete cascade
);

create table comments(
	comment_id bigint not null auto_increment primary key,
	contents varchar(1000) not null,
	created_at datetime not null default current_timestamp,
	updated_at datetime not null default current_timestamp on update current_timestamp,
	post_id bigint not null,
	user_id bigint not null,
	constraint fk_comments_post
		foreign key(post_id) references posts(post_id)
		on delete cascade,
	constraint fk_comments_user
		foreign key(user_id) references users(user_id)
		on delete cascade
);


create table post_images (
	post_id bigint not null,
	image_id bigint not null,
	orders int not null,
	primary key (post_id, image_id),

	constraint fk_pi_post
		foreign key (post_id) references posts(post_id)
		on delete cascade,

	constraint fk_pi_image
		foreign key (image_id) references images(image_id)
		on delete cascade
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

create table counts (
	post_id bigint not null,
	likes_cnt int not null default 0,
	cmt_cnt int not null default 0,
	view_cnt int not null default 0,

	primary key (post_id),

	constraint fk_counts_post
		foreign key (post_id) references posts(post_id)
		on delete cascade
	)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

create table user_like_posts (
	user_id bigint not null,
	post_id bigint not null,
	liked_at datetime not null default current_timestamp,

	primary key (user_id, post_id),

	key idx_ulp_post_id (post_id),
	key idx_ulp_user_like_at (user_id, liked_at desc, post_id desc),

	constraint fk_ulp_user
		foreign key (user_id) references users(user_id)
		on delete cascade,

	constraint flk_ulp_post
		foreign key (post_id) references posts(post_id)
		on delete cascade
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

create table refresh_entity(
	refresh_id bigint not null auto_increment primary key,
	user_id bigint not null,
	refresh varchar(255) not null,
	expiration varchar(255) not null
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
