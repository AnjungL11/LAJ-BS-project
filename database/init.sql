CREATE DATABASE IF NOT EXISTS image_manager DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE image_manager;

/*用户表 (Users)*/
CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '系统内部唯一标识用户的ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '电子邮箱',
    password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt加密后的密码',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间'
);

/*图片表 (Images)*/
CREATE TABLE IF NOT EXISTS images (
    image_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '关联Users表',
    original_filename VARCHAR(255) NOT NULL COMMENT '原始文件名',
    storage_path VARCHAR(500) NOT NULL COMMENT '原图存储路径',
    thumbnail_path VARCHAR(500) NOT NULL COMMENT '缩略图路径',
    file_size BIGINT NOT NULL COMMENT '文件大小(字节)',
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

/*标签表 (Tags)*/
CREATE TABLE IF NOT EXISTS tags (
    tag_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tag_name VARCHAR(50) NOT NULL COMMENT '标签内容',
    tag_type ENUM('system', 'ai', 'custom') NOT NULL COMMENT '标签类型: system(EXIF), ai(模型), custom(用户)'
);

/*图片元数据表 (Image_Metadata)*/
CREATE TABLE IF NOT EXISTS image_metadata (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    image_id BIGINT NOT NULL UNIQUE COMMENT '一对一关联Images',
    taken_time DATETIME COMMENT '拍摄时间',
    camera_model VARCHAR(100) COMMENT '相机型号',
    gps_latitude DOUBLE COMMENT 'GPS纬度',
    gps_longitude DOUBLE COMMENT 'GPS经度',
    width INT COMMENT '宽度',
    height INT COMMENT '高度',
    FOREIGN KEY (image_id) REFERENCES images(image_id) ON DELETE CASCADE
);

/*图片-标签关联表 (Image_tags)*/
CREATE TABLE IF NOT EXISTS image_tags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    image_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    UNIQUE KEY uk_image_tag (image_id, tag_id),
    FOREIGN KEY (image_id) REFERENCES images(image_id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(tag_id) ON DELETE CASCADE
);