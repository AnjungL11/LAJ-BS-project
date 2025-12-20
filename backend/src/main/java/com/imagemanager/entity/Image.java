package com.imagemanager.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("images")
public class Image {
    @TableId(type = IdType.AUTO)
    private Long imageId;
    private Long userId;
    private String originalFilename;
    private String storagePath;
    private String thumbnailPath;
    private Long fileSize;
    private LocalDateTime uploadedAt;
    
    // 非数据库字段，用于返回给前端
    @TableField(exist = false)
    private ImageMetadata metadata;
}