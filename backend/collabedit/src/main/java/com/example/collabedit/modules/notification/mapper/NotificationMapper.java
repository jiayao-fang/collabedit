package com.example.collabedit.modules.notification.mapper;

import com.example.collabedit.modules.notification.entity.Notification;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface NotificationMapper {

    /**
     * 插入通知
     */
     @Insert("INSERT INTO notification (receiver_id, sender_id, content, doc_id, comment_id, type, create_time, is_read, related_id, status) " +
            "VALUES (#{receiverId}, #{senderId}, #{content}, #{docId}, #{commentId}, #{type}, NOW(), 0, #{relatedId}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Notification notification);

    /**
     * 按接收人ID查询通知（支持分页、按是否已读、类型筛选）
     */
    @Select("<script>" +
            "SELECT * FROM notification WHERE receiver_id = #{receiverId} " +
            "<if test='isRead != null'>AND is_read = #{isRead}</if> " +
            "<if test='type != null and type != \"\"'>AND type = #{type}</if> " +
            "ORDER BY create_time DESC " +
            "LIMIT #{offset}, #{size}" +
            "</script>")
    List<Notification> selectByReceiverId(@Param("receiverId") Long receiverId,
                                             @Param("isRead") Integer isRead,
                                             @Param("type") String type,
                                             @Param("offset") Integer offset,
                                             @Param("size") Integer size);
    
    /**
     * 更新通知状态（用于联系人请求）
     */
    @Update("UPDATE notification SET status = #{status} WHERE id = #{id} AND receiver_id = #{receiverId}")
    int updateStatus(@Param("id") Long id, @Param("receiverId") Long receiverId, @Param("status") Integer status);
    
    /**
     * 根据relatedId和type更新通知状态（用于联系人请求）
     */
    @Update("UPDATE notification SET status = #{status} WHERE related_id = #{relatedId} AND type = #{type} AND receiver_id = #{receiverId}")
    int updateStatusByRelatedId(@Param("relatedId") Long relatedId, 
                                @Param("type") String type,
                                @Param("receiverId") Long receiverId, 
                                @Param("status") Integer status);

    /**
     * 标记通知为已读
     */
    @Update("UPDATE notification SET is_read = 1, read_time = #{readTime} WHERE id = #{id} AND receiver_id = #{receiverId}")
    int markAsRead(@Param("id") Long id,
                   @Param("receiverId") Long receiverId,
                   @Param("readTime") LocalDateTime readTime);

    /**
     * 批量标记通知为已读
     */
    @Update("<script>" +
            "UPDATE notification SET is_read = 1, read_time = #{readTime} " +
            "WHERE id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "AND receiver_id = #{receiverId}" +
            "</script>")
    int batchMarkAsRead(@Param("ids") List<Long> ids,
                        @Param("receiverId") Long receiverId,
                        @Param("readTime") LocalDateTime readTime);

    /**
     * 查询未读通知数量
     */
    @Select("SELECT COUNT(*) FROM notification WHERE receiver_id = #{receiverId} AND is_read = 0")
    Integer countUnread(@Param("receiverId") Long receiverId);

    /**
     * 删除通知（可选，根据业务需求）
     */
    @Delete("DELETE FROM notification WHERE id = #{id} AND receiver_id = #{receiverId}")
    int delete(@Param("id") Long id, @Param("receiverId") Long receiverId);

    @Select("<script>" +
        "SELECT COUNT(*) FROM notification WHERE receiver_id = #{receiverId} " +
        "<if test='isRead != null'>AND is_read = #{isRead}</if> " +
        "<if test='type != null and type != \"\"'>AND type = #{type}</if> " +
        "</script>")
Integer countByReceiverId(@Param("receiverId") Long receiverId, 
                          @Param("isRead") Integer isRead,
                          @Param("type") String type);
}