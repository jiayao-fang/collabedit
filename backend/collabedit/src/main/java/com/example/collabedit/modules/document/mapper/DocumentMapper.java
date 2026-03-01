package com.example.collabedit.modules.document.mapper;

import com.example.collabedit.modules.document.dto.DocCategoryDTO;
import com.example.collabedit.modules.document.entity.Document;
import com.example.collabedit.modules.document.entity.Tag;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DocumentMapper  {
   // 新增文档
    @Insert("INSERT INTO document(title, content, author_id, folder_id, tag_ids, edit_count, create_time, update_time, is_delete, version) " +
            "VALUES(#{title}, #{content}, #{authorId}, #{folderId}, #{tagIds}, 0, NOW(), NOW(), 0, 1)")
    int insert(Document doc);

    @Select("SELECT * FROM document WHERE id = #{id}")
    Document findById(Long id);

@Select("SELECT author FROM document WHERE id = #{id}")
    String findAuthorId(Long id);

    // 2. 根据用户ID查用户名（作者名）
    @Select("SELECT username FROM user WHERE id = #{userId}")
    String getUsernameById(@Param("userId") Long userId);

    // 3. 根据文件夹ID查文件夹名称
    @Select("SELECT folder_name as folderName FROM folder WHERE id = #{folderId}")
    String getFolderNameById(@Param("folderId") Long folderId);

     // 4. 根据标签ID查标签信息
    @Select("SELECT id, tag_name as tagName FROM tag WHERE id = #{tagId}")
    Tag getTagById(@Param("tagId") Long tagId);

    // 新增：根据ID和用户权限查询文档
        @Select("<script>" +
                "SELECT * FROM document WHERE id = #{id} AND is_delete = 0 " +
                "<choose>" +
                "   <when test='isAdmin'>" + // 管理员可以查看所有文档
                "   </when>" +
                "   <otherwise>" +
                "       AND (" +
                "           (visibility = 2) OR " + // 所有人可见
                "           (author_id = #{userId}) OR " + // 作者自己
                "           (visibility = 1 AND EXISTS (SELECT 1 FROM document_editor WHERE document_id = #{id} AND user_id = #{userId}))" + // 编辑者
                "       )" +
                "   </otherwise>" +
                "</choose>" +
                "</script>")
        Document findByIdWithPermission(@Param("id") Long id, @Param("userId") Long userId, @Param("isAdmin") boolean isAdmin);

    // 新增：更新文档可见性
        @Update("UPDATE document SET visibility = #{visibility}, update_time = NOW() WHERE id = #{id}")
        int updateVisibility(@Param("id") Long id, @Param("visibility") Integer visibility);

        @Select("SELECT * FROM document WHERE author_id = #{authorId}")
        List<Document> findByAuthor(Long authorId);

    // 编辑文档（不再自动增加版本号，版本号仅用于乐观锁）
    @Update("UPDATE document SET title=#{title}, content=#{content}, content_state=#{contentState}, " +
            "folder_id=#{folderId}, tag_ids=#{tagIds}, visibility=#{visibility}, " +
            "edit_count = edit_count + 1, update_time=NOW(), update_by=#{updateBy}, version=version + 1 " +
            "WHERE id=#{id} AND version=#{version} AND is_delete=0")
    int updateWithVersion(Document doc);
    
    // 更新文档（不带版本校验，用于版本回滚等场景）
    @Update("UPDATE document SET title=#{title}, content=#{content}, content_state=#{contentState}, " +
            "folder_id=#{folderId}, tag_ids=#{tagIds}, visibility=#{visibility}, " +
            "update_time=NOW(), update_by=#{updateBy} " +
            "WHERE id=#{id} AND is_delete=0")
    int updateWithoutVersion(Document doc);

    // 逻辑删除
    @Delete("DELETE FROM document WHERE id = #{id}")
    int perDelete(Long id);

    @Update("UPDATE document SET is_delete=1, update_time=NOW() WHERE id = #{id}")
    int logicDelete(Long id);


    // 查询回收站文档
    @Select("SELECT * FROM document WHERE author_id = #{authorId} AND is_delete = 1")
    List<Document> findRecycleByAuthor(Long authorId);

    // 恢复文档
    @Update("UPDATE document SET is_delete=0, update_time=NOW() WHERE id = #{id}")
    int recover(Long id);

    //查询全部文档
    @Select("SELECT * FROM document")
    List<Document> findAll();

    //权限过滤查询
    @Select("<script>" +
        "SELECT * FROM document WHERE is_delete = 0 " +
        "<choose>" +
        "   <when test='isAdmin'>" + // 管理员可以查看所有文档
        "   </when>" +
        "   <otherwise>" +
        "       AND (" +
        "           (visibility = 2) OR " + // 所有人可见
        "           (author_id = #{userId}) OR " + // 作者自己
        "           (visibility = 1 AND EXISTS (SELECT 1 FROM document_editor WHERE document_id = document.id AND user_id = #{userId}))" + // 编辑者
        "       )" +
        "   </otherwise>" +
        "</choose>" +
        "</script>")
    List<Document> findAllWithPermission(@Param("userId") Long userId, @Param("isAdmin") boolean isAdmin);

    //按作者查找
    @Select("SELECT COUNT(*) FROM document WHERE author_id = #{userId}")
    int countDocumentsByAuthor(Long userId);

    @Select("SELECT SUM(edit_count) FROM document WHERE author_id = #{userId}")
    Integer sumEditCountsByAuthor(Long userId);

    @Select("Select * from tag")
    List<Tag> getTags();

    //按tag分类展示
     @Select("SELECT d.id, d.title, d.create_time, u.username AS editorName " +
            "FROM document d " +
            "LEFT JOIN user u ON d.author_id = u.id " +
            "WHERE d.is_delete = 0 AND FIND_IN_SET(#{tagId}, d.tag_ids) " +
            "ORDER BY d.update_time DESC")
    List<DocCategoryDTO> listByTagId(@Param("tagId") Long tagId);

    //按folder分类展示
   @Select("SELECT d.id, d.title, d.create_time, u.username AS editorName " +
            "FROM document d " +
            "LEFT JOIN user u ON d.author_id = u.id " +
            "WHERE d.is_delete = 0 AND d.folder_id = #{folderId} " +
            "ORDER BY d.update_time DESC")
    List<DocCategoryDTO> listByFolderId(@Param("folderId") Long folderId);

     /**
     * 按标签ID查询文档总数（用于分页）
     */
    @Select("SELECT COUNT(*) FROM document " +
            "WHERE is_delete = 0 " +
            "AND FIND_IN_SET(#{tagId}, tag_ids) > 0")
    Integer countByTagId(@Param("tagId") Long tagId);

        // 全文搜索
    @Select("SELECT d.id, d.title, d.create_time, d.author_id, d.folder_id, d.tag_ids, u.username " +
        "FROM document d "+
        "JOIN user u ON d.author_id = u.id "+
        "WHERE d.is_delete = 0 "+
        "AND ("+
        "d.title LIKE CONCAT('%', #{keyword}, '%') "+ 
        "OR d.content LIKE CONCAT('%', #{keyword}, '%') "+
        "OR u.username LIKE CONCAT('%', #{keyword}, '%')) "+
        "ORDER BY d.update_time DESC")
    List<Document> search(@Param("keyword") String keyword);
    
    // 高级搜索
    @Select("<script>" +
            "SELECT id, title, create_time, author_id, folder_id, tag_ids FROM document " +
            "WHERE is_delete = 0 " +
            "<if test='authorId != null'>AND author_id = #{authorId}</if> " +
            "<if test='createTime != null'>AND create_time &gt;= #{createTime}</if> " +
            "<if test='updateTime != null'>AND update_time &lt;= #{updateTime}</if> " +
            "ORDER BY update_time DESC" +
            "</script>")
        List<Document> advancedSearch(
        @Param("authorId") Long authorId,
        @Param("createTime") LocalDateTime createTime,
        @Param("updateTime") LocalDateTime updateTime);


        @Update("UPDATE document SET folder_id = #{folderId}, update_time = NOW() WHERE id = #{docId}")
        int updateFolderId(@Param("docId") Long docId, @Param("folderId") Long folderId);

        @Update("UPDATE document SET tag_ids = #{tagIds}, update_time = NOW() WHERE id = #{docId}")
        int updateTagIds(@Param("docId") Long docId, @Param("tagIds") String tagIds);

        // 锁定文档
        @Update("UPDATE document SET is_locked = 1, locked_by = #{userId}, locked_at = #{lockedAt}, update_time = NOW() WHERE id = #{docId}")
        int lockDocument(@Param("docId") Long docId, @Param("userId") Long userId, @Param("lockedAt") LocalDateTime lockedAt);
        
        // 解锁文档
        @Update("UPDATE document SET is_locked = 0, locked_by = NULL, locked_at = NULL, update_time = NOW() WHERE id = #{docId}")
        int unlockDocument(@Param("docId") Long docId);

}
