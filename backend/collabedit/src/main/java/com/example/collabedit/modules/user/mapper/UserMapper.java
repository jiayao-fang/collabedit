package com.example.collabedit.modules.user.mapper;

import com.example.collabedit.modules.user.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {

    // 注册：增加创建时间和更新时间字段
    @Insert("INSERT INTO user(username, password, email, phone, status, create_time, update_time, avatar) " +
            "VALUES(#{username}, #{password}, #{email}, #{phone}, #{status}, #{createTime}, #{updateTime}, #{avatar})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id") 
    int register(User user);

    //向user_role表插入数据
    @Insert("INSERT INTO user_role(user_id, role_id) VALUES(#{userId}, #{roleId})")
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Integer roleId);

    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(String username);

    @Select("SELECT * FROM user WHERE email = #{email}")
    User findByEmail(String email);
    
    @Select("SELECT * FROM user WHERE phone = #{phone}")
    User findByPhone(String phone);

    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(Long id);


    @Select("SELECT username FROM user WHERE id = #{id}")
    String findUsernameById(Long id);

     @Select("SELECT avatar FROM user WHERE id = #{id}")
    String findAvatarById(Long id);

    //通过用户ID查询角色名称
    @Select("SELECT r.role_name FROM role r " +
            "JOIN user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} " +
            "LIMIT 1") // 若用户多角色，可去掉LIMIT，返回List<String>
    String findRoleNameByUserId(Long userId);

    // 新增：查询所有用户
    @Select("SELECT * FROM user")
    List<User> findAll();

    // 新增：更新用户状态
    @Update("UPDATE user SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    void updateStatus(@Param("id") Long id, @Param("status") Integer status);


    //更新用户头像
    @Update("UPDATE user SET avatar = #{avatar}, update_time = #{updateTime} WHERE id = #{id}")
    void updateAvatar(User user);

    //更新用户信息
     @Update("UPDATE user SET username = #{username}, email = #{email}, phone = #{phone}, " +
            "signature = #{signature}, update_time = #{updateTime} " +
            "WHERE id = #{id}")
      void updateUserInfo(User user);
      
    //更新密码
    @Update("UPDATE user SET password = #{password}, update_time = #{updateTime} WHERE id = #{id}")
    void updatePassword(User user);
      
     //分页查询用户
     @Select("<script>" +
                "SELECT u.id, u.username, u.email, u.status, u.create_time, u.update_time, u.phone, u.avatar, u.signature " +
                "FROM user u " +
                "LEFT JOIN user_role ur ON u.id = ur.user_id " +
                "WHERE 1=1 " +
                "<if test='roleId != null'>AND ur.role_id = #{roleId}</if> " +
                "<if test='status != null'>AND u.status = #{status}</if> " +
                "ORDER BY u.create_time " +
                "LIMIT #{offset}, #{size}" +
                "</script>")
      List<User> findUsersByPage(@Param("roleId") Long roleId, @Param("status") Integer status, @Param("offset") int offset,@Param("size") int size);

      //查询符合条件的用户总数
      @Select("<script>" +
                "SELECT COUNT(DISTINCT u.id) FROM user u " +
                "LEFT JOIN user_role ur ON u.id = ur.user_id " +
                "WHERE 1=1 " +
                "<if test='roleId != null'>AND ur.role_id = #{roleId}</if> " +
                "<if test='status != null'>AND u.status = #{status}</if> " +
                "</script>")
      long countUsers(@Param("roleId") Long roleId, @Param("status") Integer status);

        @Select("<script>" +
        "SELECT * FROM user WHERE id IN " +
        "<foreach collection='userIds' item='id' open='(' separator=',' close=')'>" +
        "#{id}" +
        "</foreach>" +
        "</script>")
        List<User> findUsersByIds(@Param("userIds") List<Long> userIds);

        @Select("SELECT id FROM user WHERE username = #{username}")
        Long findIdByUsername(String username);

        /**
         * 通过手机号或邮箱搜索用户（排除自己）
         */
        @Select("SELECT * FROM user WHERE " +
                "(phone=#{keyword} OR email=#{keyword} OR username LIKE CONCAT('%', #{keyword}, '%')) "+
                "AND id != #{excludeUserId} AND status=1")
        List<User> findByPhoneOrEmail(@Param("keyword") String keyword, @Param("excludeUserId") Long excludeUserId);
   
}
