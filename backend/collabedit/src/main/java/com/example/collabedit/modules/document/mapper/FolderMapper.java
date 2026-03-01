package com.example.collabedit.modules.document.mapper;

import com.example.collabedit.modules.document.entity.Folder;

import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface FolderMapper {
    // 创建文件夹
    @Insert("INSERT INTO folder(folder_name, parent_id, creator_id, create_time, update_time) " +
            "VALUES(#{folderName}, #{parentId}, #{creatorId}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Folder folder);

    // 查询用户的文件夹列表（支持按父文件夹筛选）
    @Select("<script>" + "SELECT * FROM folder WHERE creator_id = #{creatorId} " +
            "<if test='parentId != null'>AND parent_id = #{parentId}</if> " +
            "ORDER BY create_time DESC"+"</script>")
    List<Folder> findByCreator(@Param("creatorId") Long creatorId, @Param("parentId") Long parentId);

    // 删除文件夹
    @Delete("DELETE FROM folder WHERE id = #{id} AND creator_id = #{creatorId}")
    int delete(@Param("id") Long id, @Param("creatorId") Long creatorId);

    // 查询所有文件夹
    @Select("SELECT * FROM folder WHERE id = #{id}")
    Folder findById(Long id);

    // 查询个人文件夹
    @Select("SELECT * FROM folder WHERE id = #{id} AND creator_id = #{creatorId}")
    Folder findByIdAndCreator(@Param("id") Long id, @Param("creatorId") Long creatorId);

    // 递归查询所有子文件夹ID（含自身）
    @Select("<script>" +
            "WITH RECURSIVE folder_tree AS (" +
            "  SELECT id FROM folder WHERE id = #{folderId}" +
            "  UNION ALL" +
            "  SELECT f.id FROM folder f JOIN folder_tree ft ON f.parent_id = ft.id" +
            ")" +
            "SELECT id FROM folder_tree" +
            "</script>")
    List<Long> findAllChildFolderIds(@Param("folderId") Long folderId);

    // 统计单个文件夹（含子文件夹）的文档数
    @Select("<script>" +
            "SELECT COUNT(d.id) as docCount " +
            "FROM document d " +
            "WHERE d.is_delete=0 AND d.folder_id IN " +
            "(WITH RECURSIVE folder_tree AS (" +
            "  SELECT id FROM folder WHERE id = #{folderId}" +
            "  UNION ALL" +
            "  SELECT ft.id FROM folder ft JOIN folder_tree ftt ON ft.parent_id = ftt.id" +
            ")" +
            "SELECT id FROM folder_tree)" +
            "</script>")
    Integer countDocByFolder(@Param("folderId") Long folderId);

    // 查询所有文件夹并携带文档数（含子文件夹）
    @Select("<script>" +
            "SELECT " +
            "  f.id, " +
            "  f.folder_name as folderName, " +
            "  f.parent_id as parentId, " +
            "  f.create_time as createTime, " +
            "  (" +
            "    SELECT COUNT(d.id) " +
            "    FROM document d " +
            "    WHERE d.is_delete=0 and d.folder_id IN " +
            "    (WITH RECURSIVE folder_tree AS (" +
            "      SELECT id FROM folder WHERE id = f.id AND creator_id = #{creatorId}" +
            "      UNION ALL" +
            "      SELECT ft.id FROM folder ft JOIN folder_tree ftt ON ft.parent_id = ftt.id WHERE ft.creator_id = #{creatorId}" +
            "    )" +
            "    SELECT id FROM folder_tree)" +
            "  ) as docCount " +
            "FROM folder f " +
            "WHERE f.creator_id = #{creatorId} " +
            "ORDER BY f.create_time DESC" +
            "</script>")
    List<Map<String, Object>> findAllFoldersWithDocCount(@Param("creatorId") Long creatorId);

    // 查询所有文件夹并携带文档数（含子文件夹）
    @Select("<script>" +
            "SELECT " +
            "  f.id, " +
            "  f.folder_name as folderName, " +
            "  f.parent_id as parentId, " +
            "  f.create_time as createTime, " +
            "  (" +
            "    SELECT COUNT(d.id) " +
            "    FROM document d " +
            "    WHERE d.is_delete=0 and d.folder_id IN " +
            "    (WITH RECURSIVE folder_tree AS (" +
            "      SELECT id FROM folder WHERE id = f.id AND creator_id = #{creatorId}" +
            "      UNION ALL" +
            "      SELECT ft.id FROM folder ft JOIN folder_tree ftt ON ft.parent_id = ftt.id WHERE ft.creator_id = #{creatorId}" +
            "    )" +
            "    SELECT id FROM folder_tree)" +
            "  ) as docCount " +
            "FROM folder f " +
            "ORDER BY f.create_time DESC" +
            "</script>")
    List<Map<String, Object>> AllFolder();

    //搜索文件夹
    @Select("SELECT * " +
        "FROM folder " +
        "WHERE folder_name LIKE CONCAT('%', #{keyword}, '%') " +
        "ORDER BY create_time DESC")
    List<Folder> findFoldersByKey(@Param("keyword") String keyword);
}