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
 * DB 트랜잭션 개념을 사용하면 이 문제를 해결할 수 있음을 안다. 그리고 이어서 다음 질문으로 시작할 수 있다
 * -> 애플리케이션에서 트랜잭션을 어떤 계층에 걸어야 하는 가? (트랜잭션을 시작하고 커밋 or 롤백 하는 동작을 어느 계층에서 해야하는 가?)
 * -> 비즈니스 로직이 존재하는 서비스 계층에서 수행해야 한다. -> 문제 발생 시 비즈니스로직의 영향을 받는 모든 부분을 롤백하기 위함
 * -> 그런데 트랜잭션을 시작하기 위해서는 커넥션이 필요하다. -> 즉, 서비스 계층에서 트랜잭션을 만들고 종료하는 작업을 수행해야 한다.
 * -> 애플리케이션에서 한 트랜잭션 내에서의 모든 요청들은 같은 커넥션 내에서 이뤄져야 한다 -> DB 세션 개념과 연결됨
 * -> 그래서 이에 대한 가장 간단한 대안으로 repository 로직 호출 시 서비스 계층에서 생성한 커넥션을 인자로 전달하는 것이다.
 * JDBC - Connection 을 Parameter 로 전달하는 방식
 */
@Slf4j
public class MemberRepositoryV2 {

    private final DataSource dataSource;

    public MemberRepositoryV2(DataSource dataSource) {
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

    /**
     * 기존 findById() 메서드와 로직은 기본적으로 동일하나, 인자로 전달한 Connection 을 사용한다는 점에서 다르다.
     * @param conn
     * @param memberId
     * @return
     * @throws SQLException
     */
    public Member findById(Connection conn, String memberId) throws SQLException {
        String sql = "select * from member where member_id = ?";

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
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
            // Connection 을 지속해서 사용해야하므로 여기서 닫지 않는다.
            JdbcUtils.closeResultSet(rs);
            JdbcUtils.closeStatement(pstmt);
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

    /**
     * 기존 updateById() 메서드와 로직은 기본적으로 동일하나, 인자로 전달한 Connection 을 사용한다는 점에서 다르다.
     * @param conn
     * @param memberId
     * @param money
     * @return
     * @throws SQLException
     */
    public int updateById(Connection conn, String memberId, int money) throws SQLException {
        String sql = "update member set money = ? where member_id = ?";

        PreparedStatement pstmt = null;
        try {
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
            // Connection 을 지속해서 사용해야하므로 여기서 닫지 않는다.
            JdbcUtils.closeStatement(pstmt);
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
