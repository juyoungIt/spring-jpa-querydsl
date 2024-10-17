package hello.jdbc.repository;

import hello.connection.DBConnectionUtil;
import hello.jdbc.domain.Member;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static hello.connection.ConnectionConst.*;

/**
 * JDBC - DriverManager 사용
 */
@Slf4j
public class MemberRepositoryV0 {

    public Member save(Member member) throws SQLException {
        // DB에 요청할 SQL 구문을 직접 작성한다 - insert 구문
        String sql = "insert into member(member_id, money) values (?, ?)";

        // finally 구문에서 리소스 해제할 수 있도록 하기 위해 Connection, Statement 를 외부에 선언
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = getConnection();
            pstmt = conn.prepareStatement(sql);
            // SQL-Injection 공격을 예방하려면 다음과 같이 Parameter Binding 방식을 사용해야 한다.
            pstmt.setString(1, member.getMemberId()); // parameter binding (1)
            pstmt.setInt(2, member.getMoney());       // parameter binding (2)
            int insertedCount = pstmt.executeUpdate(); // 획득한 커넥션을 통해 DB로 SQL 문 전달, 영향받은 DB row 수를 반환
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
            rs = pstmt.executeQuery(); // 데이터를 조회하는 경우 사용, 조회 결과를 ResultSet 으로 반환한다

            /*
             * ResultSet 내부에는 cursor 라는 개념이 존재한다.
             * -> rs.next() 를 통해 커서를 다음으로 이동하며, 최초의 cursor 는 데이터를 가리키고 있지 않다.
             * -> rs.get...(???) -> 현재 cursor 가 가리키고 있는 위치의 ??? 이름을 가진 데이터를 가져온다
             * -> 더 이상 조회할 데이터가 존재하지 않는 경우 rs.next() 는 false 를 반환한다
             */
            if (rs.next()) {
                Member findMember = new Member();
                findMember.setMemberId(rs.getString("member_id"));
                findMember.setMoney(rs.getInt("money"));
                return findMember;
            } else {
                // 인자로 전달한 memberId 에 대한 record 가 없는 경우 예외를 발생시키도록 의도적으로 작성함
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

    // 테스트를 반복하기 위한 목적으로 작성 -> member table 에 저장된 모든 row 를 삭제한다
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
     * 리소스 정리
     * -> 쿼리를 실행하였다면, 생성했던 리소스를 정리해야 한다
     * -> 리소스를 정리할 때에는 항상 역순으로 수행해야 한다 (Connection 을 통해 PreparedStatement 를 생성하기 때문)
     * -> 리소스 정리를 제대로 하지 않으면 리소스 누수가 발생하여 커넥션 부족으로 인한 장애가 발생할 수 있으므로 매우 중요한 단계이다.
     * @param conn
     * @param stmt
     * @param rs
     */
    private void close(Connection conn, Statement stmt, ResultSet rs) {
        // ResultSet close...
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                log.error("failed to close ResultSet...", e);
            }
        }
        // Statement close...
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                log.error("failed to close Statement...", e);
            }
        }
        // Connection close...
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                log.error("failed to close Connection...", e);
            }
        }
    }

    private Connection getConnection() {
        // DriverManager 를 사용해서 DB Connection 을 획득
        return DBConnectionUtil.getConnection(URL, USERNAME, PASSWORD);
    }

}
