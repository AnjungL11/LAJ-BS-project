package com.imagemanager.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("tags")
public class Tag {
    @TableId(type = IdType.AUTO)
    private Long tagId;
    private String tagName;
    private String tagType; // system, ai, custom
    private String coverType;  // "color"或"image"
    private String coverColor; // 封面颜色
    private String coverUrl;   // 图片封面的URL
    private Integer sortOrder; // 排序权重
    @TableField(exist = false)
    private Long count;
}