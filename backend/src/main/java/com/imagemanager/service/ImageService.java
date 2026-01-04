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
import com.imagemanager.dto.ImageSearchRequest;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.nio.file.Files;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.net.URLEncoder;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class ImageService {

    @Value("${app.storage-path}")
    private String storagePath;
    @Value("${app.thumbnail-path}")
    private String thumbPath;

    @Autowired private ImageMapper imageMapper;
    @Autowired private MetadataMapper metadataMapper;
    @Autowired private TagMapper tagMapper;
    @Autowired private ImageTagMapper imageTagMapper;

    // 上传图片处理
    @Transactional
    public Image upload(MultipartFile file, Long userId) throws IOException {
        // 准备文件路径
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        File destFile = new File(storagePath + fileName);
        File thumbFile = new File(thumbPath + fileName);
        
        if (!destFile.getParentFile().exists()) destFile.getParentFile().mkdirs();
        if (!thumbFile.getParentFile().exists()) thumbFile.getParentFile().mkdirs();
        // 保存原图
        file.transferTo(destFile);
        // 生成缩略图
        ImageProcessUtil.createThumbnail(destFile, thumbFile);
        // 保存数据库记录
        Image image = new Image();
        image.setUserId(userId);
        image.setOriginalFilename(file.getOriginalFilename());
        image.setStoragePath(destFile.getAbsolutePath());
        image.setThumbnailPath(thumbFile.getAbsolutePath());
        image.setFileSize(file.getSize());
        image.setUploadedAt(LocalDateTime.now());
        imageMapper.insert(image);

        // 提取EXIF并保存
        ImageMetadata meta = ImageProcessUtil.extractMetadata(destFile);
        meta.setImageId(image.getImageId());
        metadataMapper.insert(meta);
        
        // 异步调用AI服务生成标签
        // aiService.analyze(image);
        createSystemTagsFromMetadata(image.getImageId(), meta);

        return image;
    }

    // 辅助方法，将元数据转换为Tag
    private void createSystemTagsFromMetadata(Long imageId, ImageMetadata meta) {
        if (meta == null) return;

        // 相机型号标签
        if (meta.getCameraModel() != null && !meta.getCameraModel().isEmpty()) {
            addSystemTag(imageId, meta.getCameraModel());
        }

        // 时间标签
        if (meta.getTakenTime() != null) {
            addSystemTag(imageId, meta.getTakenTime().getYear() + "年");
            addSystemTag(imageId, meta.getTakenTime().getMonthValue() + "月");
        }

        // 分辨率标签
        if (meta.getWidth() != null && meta.getWidth() > 3000) {
            addSystemTag(imageId, "高清大图");
        }
    }

    /**
     * 批量更新图片AI标签
     * @param imageId 图片ID
     * @param tags 标签列表
     */
    @Transactional
    public void updateImageTags(Long imageId, List<String> tags) {
        if (tags == null || tags.isEmpty()) return;

        for (String tagName : tags) {
            // AI识别出的标签会被标记为"system" 类型
            addSystemTag(imageId, tagName);
        }
    }

    /**
     * 简单的按ID获取图片信息
     */
    public Image getById(Long imageId) {
        return imageMapper.selectById(imageId);
    }

    // 获取图片完整详情
    public Image getDetail(Long imageId) {
        // 查主表
        Image image = imageMapper.selectById(imageId);
        if (image == null) return null;
        // 查元数据
        ImageMetadata meta = metadataMapper.selectOne(
            new QueryWrapper<ImageMetadata>().eq("image_id", imageId)
        );
        image.setMetadata(meta);
        // 查标签
        List<Object> tagIds = imageTagMapper.selectObjs(
            new QueryWrapper<ImageTag>().select("tag_id").eq("image_id", imageId)
        );
        
        if (tagIds != null && !tagIds.isEmpty()) {
            List<Object> tagNames = tagMapper.selectObjs(
                new QueryWrapper<Tag>().select("tag_name").in("tag_id", tagIds)
            );
            // 转换类型
            List<String> names = tagNames.stream().map(Object::toString).toList();
            image.setTags(names);
        } else {
            image.setTags(new ArrayList<>());
        }
        return image;
    }

    // 添加标签
    @Transactional
    public void addTag(Long imageId, String tagName) {
        if (tagName == null || tagName.trim().isEmpty()) return;
        // 查找标签是否已经存在，不存在则新建
        Tag tag = tagMapper.selectOne(new QueryWrapper<Tag>().eq("tag_name", tagName));
        if (tag == null) {
            tag = new Tag();
            tag.setTagName(tagName);
            // 用户自定义标签
            tag.setTagType("custom");
            tagMapper.insert(tag);
        }
        // 建立关联
        Long tagId = tag.getTagId();
        Long count = imageTagMapper.selectCount(
            new QueryWrapper<ImageTag>()
                .eq("image_id", imageId)
                .eq("tag_id", tagId)
        );
        if (count == 0) {
            ImageTag relation = new ImageTag();
            relation.setImageId(imageId);
            relation.setTagId(tagId);
            imageTagMapper.insert(relation);
        }
    }

    // 移除标签
    @Transactional
    public void removeTag(Long imageId, String tagName) {
        // 找到标签ID
        Tag tag = tagMapper.selectOne(new QueryWrapper<Tag>().eq("tag_name", tagName));
        if (tag == null) return;
        // 删除关联表中的记录
        imageTagMapper.delete(
            new QueryWrapper<ImageTag>()
                .eq("image_id", imageId)
                .eq("tag_id", tag.getTagId())
        );
    }

    // 创建标签并关联，防止重复
    private void addSystemTag(Long imageId, String tagName) {
        // 检查标签是否存在，不存在则创建
        Tag tag = tagMapper.selectOne(new QueryWrapper<Tag>().eq("tag_name", tagName));
        if (tag == null) {
            tag = new Tag();
            tag.setTagName(tagName);
            // 标记为系统生成的标签
            tag.setTagType("system");
            tagMapper.insert(tag);
        }

        // 建立图片与标签的关联，避免重复关联
        QueryWrapper<ImageTag> relationQuery = new QueryWrapper<ImageTag>()
                .eq("image_id", imageId)
                .eq("tag_id", tag.getTagId());
        
        if (imageTagMapper.selectCount(relationQuery) == 0) {
            ImageTag relation = new ImageTag();
            relation.setImageId(imageId);
            relation.setTagId(tag.getTagId());
            imageTagMapper.insert(relation);
        }
    }
    
    // 多条件查询
    public Page<Image> search(Integer page, Integer size, String keyword, Long userId) {
        Page<Image> pageParam = new Page<>(page, size);
        QueryWrapper<Image> query = new QueryWrapper<>();
        query.eq("user_id", userId);
        if (keyword != null && !keyword.isEmpty()) {
            query.like("original_filename", keyword);
        }
        query.orderByDesc("uploaded_at");
        Page<Image> result = imageMapper.selectPage(pageParam, query);

        // 遍历结果集，填充metadata和tags
        result.getRecords().forEach(img -> {
            // 填充Metadata
            ImageMetadata meta = metadataMapper.selectOne(
                new QueryWrapper<ImageMetadata>().eq("image_id", img.getImageId())
            );
            img.setMetadata(meta);

            // 填充Tags
            // 先查中间表image_tags拿到tag_id列表
            List<Object> tagIds = imageTagMapper.selectObjs(
                new QueryWrapper<ImageTag>()
                    // 只查tag_id字段
                    .select("tag_id")
                    .eq("image_id", img.getImageId())
            );
            
            if (tagIds != null && !tagIds.isEmpty()) {
                // 再查tags表拿到tag_name
                List<Object> tagNames = tagMapper.selectObjs(
                    new QueryWrapper<Tag>()
                        .select("tag_name")
                        .in("tag_id", tagIds)
                );
                // 类型转换
                List<String> names = tagNames.stream()
                        .map(Object::toString)
                        .toList();
                img.setTags(names);
            }
        });
        return result;
    }

    // 完善增强查询功能
    public Page<Image> searchAdvanced(ImageSearchRequest req, Long userId) {
        Page<Image> pageParam = new Page<>(req.getPage(), req.getSize());
        QueryWrapper<Image> query = new QueryWrapper<>();
        // 只能查自己上传的图
        query.eq("user_id", userId);

        // 按照文件名模糊查询
        if (StringUtils.hasText(req.getFilename())) {
            query.like("original_filename", req.getFilename());
        }

        // 按照标签查询
        // 查找拥有指定标签的image_id对应的图片
        if (req.getTags() != null && !req.getTags().isEmpty()) {
            // 拼接SQL语句
            // String tagsStr = "'" + String.join("','", req.getTags()) + "'";
            // 单引号转义防止SQL注入
            List<String> safeTags = req.getTags().stream()
                    .map(t -> t.replace("'", "''")) 
                    .toList();
            String tagsStr = "'" + String.join("','", safeTags) + "'";
            int tagCount = safeTags.size();
            // String subQuery = String.format(
            //     "SELECT image_id FROM image_tags WHERE tag_id IN (SELECT tag_id FROM tags WHERE tag_name IN (%s))", 
            //     tagsStr
            // );
            String subQuery = String.format(
                "SELECT it.image_id FROM image_tags it " +
                "JOIN tags t ON it.tag_id = t.tag_id " +
                "WHERE t.tag_name IN (%s) " +
                "GROUP BY it.image_id " +
                "HAVING COUNT(DISTINCT t.tag_name) = %d", 
                tagsStr, tagCount
            );
            query.inSql("image_id", subQuery);
        }

        // 按照图片元数据查询
        boolean hasMetadataCondition = StringUtils.hasText(req.getCameraModel()) || req.getStartTime() != null || req.getEndTime() != null;
                                    
        if (hasMetadataCondition) {
            StringBuilder metaSubQuery = new StringBuilder("SELECT image_id FROM image_metadata WHERE 1=1 ");
            
            // 设备型号筛选
            if (StringUtils.hasText(req.getCameraModel())) {
                metaSubQuery.append(" AND camera_model LIKE '%").append(req.getCameraModel()).append("%'");
            }
            
            // 时间范围筛选
            if (req.getStartTime() != null) {
                metaSubQuery.append(" AND taken_time >= '").append(req.getStartTime()).append("'");
            }
            if (req.getEndTime() != null) {
                metaSubQuery.append(" AND taken_time <= '").append(req.getEndTime()).append("'");
            }
            
            query.inSql("image_id", metaSubQuery.toString());
        }
        query.orderByDesc("uploaded_at");

        // 执行查询
        Page<Image> result = imageMapper.selectPage(pageParam, query);
        // 手动填充tags和metadata信息
        populateAdditionalInfo(result); 
        return result;
    }

    // 填充逻辑
    private void populateAdditionalInfo(Page<Image> result) {
        result.getRecords().forEach(img -> {
            // 填充Metadata
            ImageMetadata meta = metadataMapper.selectOne(new QueryWrapper<ImageMetadata>().eq("image_id", img.getImageId()));
            img.setMetadata(meta);
            // 填充Tags
            List<Object> tagIds = imageTagMapper.selectObjs(new QueryWrapper<ImageTag>().select("tag_id").eq("image_id", img.getImageId()));
            if (tagIds != null && !tagIds.isEmpty()) {
                List<Object> tagNames = tagMapper.selectObjs(new QueryWrapper<Tag>().select("tag_name").in("tag_id", tagIds));
                img.setTags(tagNames.stream().map(Object::toString).toList());
            }
        });
    }

    // 批量删除图片
    @Transactional
    public void deleteBatch(List<Long> imageIds, Long userId) {
        for (Long id : imageIds) {
            // 查找图片，只能删除用户自己的图片
            Image img = imageMapper.selectOne(new QueryWrapper<Image>()
                    .eq("image_id", id)
                    .eq("user_id", userId));
            if (img != null) {
                // 删除物理文件，包括原图和缩略图
                deleteFile(img.getStoragePath());
                deleteFile(img.getThumbnailPath());
                // 删除数据库关联数据
                imageTagMapper.delete(new QueryWrapper<ImageTag>().eq("image_id", id));
                metadataMapper.delete(new QueryWrapper<ImageMetadata>().eq("image_id", id));
                // 删除主表数据
                imageMapper.deleteById(id);
            }
        }
    }

    // 删除文件辅助函数
    private void deleteFile(String path) {
        if (path != null) {
            File file = new File(path);
            if (file.exists()) file.delete();
        }
    }

    //更新图片内容
    @Transactional
    public void updateImageContent(Long imageId, MultipartFile newFile, Long userId) throws IOException {
        // 校验权限和存在性
        Image image = imageMapper.selectOne(new QueryWrapper<Image>()
                .eq("image_id", imageId)
                .eq("user_id", userId));
        if (image == null) {
            throw new RuntimeException("图片不存在或无权操作");
        }
        File originalFile = new File(image.getStoragePath());
        File thumbFile = new File(image.getThumbnailPath());
        // 覆盖原图 ，如果文件不存在会自动创建
        // 使用java.nio.file.Files.copy来保存，覆盖已存在的文件
        Files.copy(newFile.getInputStream(), originalFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        // 重新生成缩略图，自动覆盖旧的缩略图
        ImageProcessUtil.createThumbnail(originalFile, thumbFile);
        // 更新数据库信息
        image.setFileSize(newFile.getSize());
        // 更新上传时间
        image.setUploadedAt(LocalDateTime.now());
        imageMapper.updateById(image);
    }

    // 重命名图片
    @Transactional
    public void renameImage(Long imageId, String newName) {
        if (newName == null || newName.trim().isEmpty()) return;
        Image image = imageMapper.selectById(imageId);
        if (image != null) {
            // 只修改数据库中的显示名称，不修改物理文件
            image.setOriginalFilename(newName);
            imageMapper.updateById(image);
        }
    }

    // 下载图片流处理
    public void downloadImage(Long imageId, HttpServletResponse response) {
        try {
            Image image = imageMapper.selectById(imageId);
            if (image == null) throw new RuntimeException("图片不存在");
            File file = new File(image.getStoragePath());
            if (!file.exists()) throw new RuntimeException("物理文件丢失");
            // 设置响应头
            // 处理中文文件名乱码问题
            String encodedFilename = URLEncoder.encode(image.getOriginalFilename(), "UTF-8").replaceAll("\\+", "%20");
            response.reset();
            // response.setContentType("application/octet-stream");
            response.setContentType("image/jpeg");
            response.setCharacterEncoding("utf-8");
            response.setContentLength((int) file.length());
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);
            // 读取文件并写入响应流
            try (FileInputStream fis = new FileInputStream(file);
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    os.write(buffer, 0, len);
                }
                os.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("下载失败");
        }
    }
}
