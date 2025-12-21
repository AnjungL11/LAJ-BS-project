package com.imagemanager.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ImageSearchRequest {
    private Integer page = 1;
    private Integer size = 20;
    
    // 文件名关键字
    private String filename;
    // 选中的标签列表
    private List<String> tags;
    // 相机设备型号
    private String cameraModel;
    // 拍摄开始时间
    private LocalDateTime startTime;
    // 拍摄结束时间
    private LocalDateTime endTime;
}