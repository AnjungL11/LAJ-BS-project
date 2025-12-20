package com.imagemanager.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.imagemanager.entity.*;
import com.imagemanager.mapper.*;
import com.imagemanager.utils.ImageProcessUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ImageService {

    @Value("${app.storage-path}")
    private String storagePath;
    @Value("${app.thumbnail-path}")
    private String thumbPath;

    @Autowired private ImageMapper imageMapper;
    @Autowired private MetadataMapper metadataMapper;

    // 上传图片处理 [cite: 84, 217]
    @Transactional
    public Image upload(MultipartFile file, Long userId) throws IOException {
        // 1. 准备文件路径
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        File destFile = new File(storagePath + fileName);
        File thumbFile = new File(thumbPath + fileName);
        
        if (!destFile.getParentFile().exists()) destFile.getParentFile().mkdirs();
        if (!thumbFile.getParentFile().exists()) thumbFile.getParentFile().mkdirs();

        // 2. 保存原图
        file.transferTo(destFile);

        // 3. 生成缩略图
        ImageProcessUtil.createThumbnail(destFile, thumbFile);

        // 4. 保存数据库记录
        Image image = new Image();
        image.setUserId(userId);
        image.setOriginalFilename(file.getOriginalFilename());
        image.setStoragePath(destFile.getAbsolutePath());
        image.setThumbnailPath(thumbFile.getAbsolutePath());
        image.setFileSize(file.getSize());
        image.setUploadedAt(LocalDateTime.now());
        imageMapper.insert(image);

        // 5. 提取EXIF并保存 [cite: 89]
        ImageMetadata meta = ImageProcessUtil.extractMetadata(destFile);
        meta.setImageId(image.getImageId());
        metadataMapper.insert(meta);
        
        // 6. 异步调用AI服务生成标签 (增强功能, 这里省略异步代码)
        // aiService.analyze(image);

        return image;
    }
    
    // 多条件查询 [cite: 107]
    public Page<Image> search(Integer page, Integer size, String keyword, Long userId) {
        Page<Image> pageParam = new Page<>(page, size);
        QueryWrapper<Image> query = new QueryWrapper<>();
        query.eq("user_id", userId);
        if (keyword != null && !keyword.isEmpty()) {
            query.like("original_filename", keyword);
            // 实际还应联表查询 tags
        }
        query.orderByDesc("uploaded_at");
        return imageMapper.selectPage(pageParam, query);
    }
}