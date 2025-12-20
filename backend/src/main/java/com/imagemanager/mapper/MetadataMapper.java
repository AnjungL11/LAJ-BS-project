package com.imagemanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.imagemanager.entity.ImageMetadata;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MetadataMapper extends BaseMapper<ImageMetadata> {
}