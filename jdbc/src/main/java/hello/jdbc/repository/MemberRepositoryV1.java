package hello.jdbc.repository;

import hello.jdbc.domain.Member;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.support.JdbcUtils;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * JDBC - DataSource, JdbcUtils 사용
 */
@Slf4j
public class MemberRepositoryV1 {

    private final DataSource dataSource;

    public MemberRepositoryV1(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Member save(Member member) throws SQLException {
        String sql = "insert into member(member_id, money) values (?, ?)";

        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, member.getMemberId());
            pstmt.setInt(2, member.getMoney());
            int insertedCount = pstmt.executeUpdate();
            log.info("{} record successfully inserted...", insertedCount);
            return member;
        } catch (SQLException e) {
            log.error("{} member info save failed...", member, e);
            throw e;
        } finally {
            close(conn, pstmt, null);
        }
    }

    public Member findById(String memberId) throws SQLException {
        String sql = "select * from member where member_id = ?";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, memberId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                Member findMember = new Member();
                findMember.setMemberId(rs.getString("member_id"));
                findMember.setMoney(rs.getInt("money"));
                return findMember;
            } else {
                throw new NoSuchElementException("memberId = " + memberId + " does not exist...");
            }
        } catch (SQLException e) {
            log.error("memberId={} member info find failed...", memberId, e);
            throw e;
        } finally {
            close(conn, pstmt, rs);
        }
    }

    public List<Member> findAll() throws SQLException {
        String sql = "select * from member";

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);

            List<Member> members = new ArrayList<>();
            while (rs.next()) {
                Member findMember = new Member();
                findMember.setMemberId(rs.getString("member_id"));
                findMember.setMoney(rs.getInt("money"));
                members.add(findMember);
            }
            return members;
        } catch (SQLException e) {
            log.error("member info find failed...", e);
            throw e;
        } finally {
            close(conn, stmt, rs);
        }
    }

    public int updateById(String memberId, int money) throws SQLException {
        String sql = "update member set money = ? where member_id = ?";

        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, money);
            pstmt.setString(2, memberId);
            int updatedCount = pstmt.executeUpdate();
            log.info("{} row updated by memberId = {}", updatedCount, memberId);
            return updatedCount;
        } catch (SQLException e) {
            log.error("memberId={} member info update failed...", memberId, e);
            throw e;
        } finally {
            close(conn, pstmt, null);
        }
    }

    public int deleteById(String memberId) throws SQLException {
        String sql = "delete from member where member_id = ?";

        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, memberId);
            int deletedCount = pstmt.executeUpdate();
            log.info("{} row deleted by memberId = {}", deletedCount, memberId);
            return deletedCount;
        } catch (SQLException e) {
            log.error("memberId={} member info deleted failed...", memberId, e);
            throw e;
        } finally {
            close(conn, pstmt, null);
        }
    }

    public int clearAll() throws SQLException {
        String sql = "delete from member";

        Connection conn = null;
        Statement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.createStatement();
            int deletedCount = stmt.executeUpdate(sql);
            log.info("{} member row cleared...", deletedCount);
            return deletedCount;
        } catch (SQLException e) {
            log.error("member info clear failed...", e);
            throw e;
        } finally {
            close(conn, stmt, null);
        }
    }

    /**
     * JdbcUtils
     * -> Spring 수준에서 Jdbc 를 더 쉽게 다룰 수 있도록 제공하는 클래스
     * -> 리소스 회수를 위해 작성했던 반복적인 코드들이 모두 없어지고,
     *    ResultSet, Statement, Connection 을 모두 심플한 코드로 회수할 수 있게 됨.
     * @param conn
     * @param stmt
     * @param rs
     */
    private void close(Connection conn, Statement stmt, ResultSet rs) {
        JdbcUtils.closeResultSet(rs);
        JdbcUtils.closeStatement(stmt);
        JdbcUtils.closeConnection(conn);
    }

    private Connection getConnection() throws SQLException {
        // DataSource 를 사용해서 커넥션 획득
        return dataSource.getConnection();
    }

}
