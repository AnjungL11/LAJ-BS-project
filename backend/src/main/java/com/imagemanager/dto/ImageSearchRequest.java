package com.imagemanager.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;
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
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") 
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;
    // 拍摄结束时间
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;
}