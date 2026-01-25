package com.example.blog.mapper;

import com.example.blog.entity.Tag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {
    // 根据文章ID查询所有标签名
    @Select("SELECT t.name FROM tags t JOIN post_tags pt ON t.id = pt.tag_id WHERE pt.post_id = #{postId}")
    List<String> selectTagsByPostId(Long postId);

    // [新增] 查询所有有文章关联的标签名 (DISTINCT 去重)
    // 只有在 post_tags 表里有记录的标签才会被查出来
    @Select("SELECT DISTINCT t.name FROM tags t JOIN post_tags pt ON t.id = pt.tag_id")
    List<String> selectTagsWithPosts();
}