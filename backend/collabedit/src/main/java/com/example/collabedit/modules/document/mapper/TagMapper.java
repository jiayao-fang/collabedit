package com.example.collabedit.modules.document.mapper;

import com.example.collabedit.modules.document.entity.Tag;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface TagMapper {
    //创建标签
    @Insert("INSERT INTO tag(tag_name, create_time) VALUES(#{tagName}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Tag tag);

    //查询标签
    @Select("SELECT * FROM tag WHERE tag_name = #{tagName}")
    Tag findByName(String tagName);

    //查询标签
    @Select("SELECT * FROM tag WHERE id = #{id}")
    Tag findById(Long id);

    //获取满足条件的标签，用于获取文档的标签
    @Select("SELECT * FROM tag WHERE id IN (${tagIds})") 
    List<Tag> findByIds(@Param("tagIds") String tagIds);

    @Select("SELECT * FROM tag")
    List<Tag> findAll();
    // 统计每个标签的文档数量
    @Select("""
        SELECT 
            t.id, 
            t.tag_name as tagName, 
            COUNT(d.id) as docCount 
        FROM tag t
        LEFT JOIN document d ON FIND_IN_SET(t.id, d.tag_ids)
        and d.is_delete=0
        GROUP BY t.id, t.tag_name
    """)
    List<Map<String, Object>> findAllWithDocCount();

    //搜索标签
    @Select("select * from tag where tag_name LIKE CONCAT('%', #{keyword}, '%') " )
    List<Tag> findTagsByKey(@Param("keyword") String keyword);

    //标签中的文档数
    @Select({
        "SELECT COUNT(*) FROM document ",
        "WHERE id IS NOT NULL and is_delete=0",
        "AND CONCAT(',', tag_ids, ',') LIKE CONCAT('%,', #{tagId}, ',%')"
    })
    Integer countDocInTag(@Param("tagId") Long tagId);

    //获取集合中的标签
      @Select({
        "<script>",
        "SELECT id FROM tag WHERE id IN ",
        "<foreach collection='tagIds' item='id' open='(' separator=',' close=')'>",
        "#{id}",
        "</foreach>",
        "</script>"
    })
    List<Long> findExistIdsByIds(@Param("tagIds") List<Long> tagIds);

}