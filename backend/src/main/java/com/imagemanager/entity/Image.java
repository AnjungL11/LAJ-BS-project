package com.imagemanager.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

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
    
    // 元数据对象
    @TableField(exist = false)
    private ImageMetadata metadata;

    // 标签列表
    @TableField(exist = false)
    private List<String> tags;
}