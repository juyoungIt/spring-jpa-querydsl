package hello.jdbc.repository;

import hello.jdbc.domain.Member;
import hello.jdbc.repository.exception.MyDbException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * SQLExceptionTranslator 추가
 */
@Slf4j
public class MemberRepositoryV4_2 implements MemberRepository {

    private final DataSource dataSource;
    private final SQLExceptionTranslator exTranslator;

    public MemberRepositoryV4_2(DataSource dataSource) {
        this.dataSource = dataSource;
        this.exTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
    }

    @Override
    public Member save(Member member) {
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
            throw exTranslator.translate("save", sql, e);
        } finally {
            close(conn, pstmt, null);
        }
    }

    @Override
    public Member findById(String memberId) {
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
            throw exTranslator.translate("findById", sql, e);
        } finally {
            close(conn, pstmt, rs);
        }
    }

    @Override
    public List<Member> findAll() {
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
            throw exTranslator.translate("findAll", sql, e);
        } finally {
            close(conn, stmt, rs);
        }
    }

    @Override
    public int updateById(String memberId, int money) {
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
            throw exTranslator.translate("updateById", sql, e);
        } finally {
            close(conn, pstmt, null);
        }
    }

    @Override
    public int deleteById(String memberId) {
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
            throw exTranslator.translate("deleteById", sql, e);
        } finally {
            close(conn, pstmt, null);
        }
    }

    @Override
    public int clearAll() {
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
            throw exTranslator.translate("clearAll", sql, e);
        } finally {
            close(conn, stmt, null);
        }
    }

    /**
     * DataSourceUtils.releaseConnection();
     * -> 커넥션을 conn.close() 로 직접 닫아버리면 커넥션 유지가 불가능하다.
     * -> 커넥션은 이후 로직과 트랜잭션을 종료하는 시점까지 유지되어야 한다.
     * 해당 클래스의 메서드는 다음과 같이 동작한다.
     * 1. 트랜잭션을 사용하기 위해 동기화된 커넥션은 닫지 않고, 그대로 유지
     * 2. 트랜잭션 동기화 매니저가 관리하지 않는 커넥션이 없는 경우 해당 커넥션을 닫는다.
     * @param conn
     * @param stmt
     * @param rs
     */
    private void close(Connection conn, Statement stmt, ResultSet rs) {
        JdbcUtils.closeResultSet(rs);
        JdbcUtils.closeStatement(stmt);
        // 주의! - 트랜잭션 동기화를 사용하려면 DataSourceUtils 를 사용해야 한다.
        DataSourceUtils.releaseConnection(conn, dataSource);
    }

    /**
     * DataSourceUtils.getConnection()
     * -> Spring 은 트랜잭션 컨텍스트에 따른 커넥션 관리. 즉, 트랜잭션 동기화를 DataSourceUtils 클래스를 통해 지원한다.
     * 해당 클래스의 메서드는 다음과 같이 동작한다.
     * 1. 트랜잭션 동기화 매니저가 관리하는 커넥션이 존재하는 경우 해당 커넥션을 반환
     * 2. 트랜잭션 동기화 매니저가 관리하는 커넥션이 존재하지 않는 경우 새롭게 커넥션을 생성하여 반환
     * @return
     */
    private Connection getConnection() {
        return DataSourceUtils.getConnection(dataSource);
    }

}
