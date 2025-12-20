package com.imagemanager.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("image_metadata")
public class ImageMetadata {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long imageId;
    private LocalDateTime takenTime;
    private String cameraModel;
    private Double gpsLatitude;
    private Double gpsLongitude;
    private Integer width;
    private Integer height;
}