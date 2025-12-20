package com.imagemanager.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("image_tags")
public class ImageTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long imageId;
    private Long tagId;
}