package com.imagemanager.controller;

import com.imagemanager.entity.Tag;
import com.imagemanager.mapper.TagMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.imagemanager.mapper.ImageTagMapper;
import com.imagemanager.entity.Image;
import com.imagemanager.entity.ImageTag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private ImageTagMapper imageTagMapper;

    // 获取标签列表
    @GetMapping
    public List<Tag> listTags() {
        return tagMapper.selectTagsWithCount();
    }

    // 创建新标签
    @PostMapping
    public String createTag(@RequestBody Map<String, String> params) {
        String name = params.get("name");
        Tag tag = new Tag();
        tag.setTagName(name);
        tag.setTagType("custom");
        tag.setCoverType("color");
        // 默认蓝色
        tag.setCoverColor("#409EFF");
        tag.setSortOrder(0);
        tagMapper.insert(tag);
        return "success";
    }

    // 重命名标签
    @PutMapping("/{id}")
    public String renameTag(@PathVariable Long id, @RequestBody Map<String, String> params) {
        String newName = params.get("name");
        Tag tag = new Tag();
        tag.setTagId(id);
        tag.setTagName(newName);
        tagMapper.updateById(tag);
        return "success";
    }

    @GetMapping("/{id}")
    public Tag getTag(@PathVariable Long id) {
        return tagMapper.selectById(id);
    }

    @GetMapping("/{id}/images")
    public List<Image> getImagesByTag(@PathVariable Long id) {
        return tagMapper.selectImagesByTagId(id);
    }

    // 修改标签样式
    @PutMapping("/{id}/style")
    public String updateStyle(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        Tag tag = new Tag();
        tag.setTagId(id);
        
        if (params.containsKey("coverType")) {
            tag.setCoverType((String) params.get("coverType"));
        }
        if (params.containsKey("coverColor")) {
            tag.setCoverColor((String) params.get("coverColor"));
            // 如果切回纯色，清空图片URL
            if ("color".equals(tag.getCoverType())) {
                tag.setCoverUrl(null);
            }
        }
        if (params.containsKey("coverUrl")) {
            tag.setCoverUrl((String) params.get("coverUrl"));
        }
        
        tagMapper.updateById(tag);
        return "success";
    }

    // // 拖拽排序
    // @PostMapping("/reorder")
    // public String reorderTags(@RequestBody Map<String, List<Long>> params) {
    //     List<Long> ids = params.get("ids");
    //     if (ids != null) {
    //         for (int i = 0; i < ids.size(); i++) {
    //             // 更新sort_order=列表中的索引
    //             Tag tag = new Tag();
    //             tag.setTagId(ids.get(i));
    //             tag.setSortOrder(i);
    //             tagMapper.updateById(tag);
    //         }
    //     }
    //     return "success";
    // }
    public static class ReorderDTO {
        // 对应前端传来的 { "ids": [1, 2, 3] }
        private List<Long> ids;

        public List<Long> getIds() { return ids; }
        public void setIds(List<Long> ids) { this.ids = ids; }
    }

    // 拖拽排序
    @PostMapping("/reorder")
    public String reorderTags(@RequestBody ReorderDTO reorderDTO) {
        try {
            // 获取ID列表
            List<Long> ids = reorderDTO.getIds();
            
            if (ids == null || ids.isEmpty()) {
                // 列表为空，直接返回
                return "empty list";
            }
            // 打印日志确认收到了数据
            System.out.println("收到排序请求，IDs: " + ids);

            for (int i = 0; i < ids.size(); i++) {
                Long tagId = ids.get(i);
                
                // 构建更新对象
                Tag tag = new Tag();
                tag.setTagId(tagId);
                // 将索引作为排序权重
                tag.setSortOrder(i);
                
                // 执行更新
                tagMapper.updateById(tag);
            }
            return "success";
            
        } catch (Exception e) {
            // 强制打印错误堆栈到控制台
            e.printStackTrace(); 
            throw new RuntimeException("排序保存失败: " + e.getMessage());
        }
    }

    // 删除标签
    @DeleteMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public String deleteTag(@PathVariable Long id) {
        imageTagMapper.delete(new QueryWrapper<ImageTag>().eq("tag_id", id));
        // 删除标签本身
        tagMapper.deleteById(id);
        
        // 删除image_tags中所有关联关系
        // imageTagMapper.delete(new QueryWrapper<ImageTag>().eq("tag_id", id));
        
        return "success";
    }

    @DeleteMapping("/empty")
    public String clearEmptyTags() {
        tagMapper.deleteUnusedTags();
        return "success";
    }
}
