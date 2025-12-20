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
}