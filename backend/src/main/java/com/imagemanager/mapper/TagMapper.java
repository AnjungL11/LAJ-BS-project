package com.imagemanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.imagemanager.entity.Image;
import com.imagemanager.entity.Tag;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {
    // 联合查询，查出标签信息和关联的图片数量
    // 按照sort_order升序，tag_id降序排列
    @Select("SELECT t.*, COUNT(it.image_id) as count " +
            "FROM tags t " +
            "LEFT JOIN image_tags it ON t.tag_id = it.tag_id " +
            "GROUP BY t.tag_id " +
            "ORDER BY t.sort_order ASC, t.tag_id DESC")
    List<Tag> selectTagsWithCount();

    @Delete("DELETE FROM tags WHERE tag_id NOT IN (SELECT DISTINCT tag_id FROM image_tags)")
    void deleteUnusedTags();
    
    @Select("SELECT i.* FROM images i " +
            "INNER JOIN image_tags it ON i.image_id = it.image_id " +
            "WHERE it.tag_id = #{tagId} " +
            "ORDER BY i.uploaded_at DESC, i.image_id DESC") 
    List<Image> selectImagesByTagId(@Param("tagId") Long tagId);
}