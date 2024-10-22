package hello.jdbc.service;

import hello.jdbc.domain.Member;
import hello.jdbc.repository.MemberRepositoryV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Parameter 를 통한 Connection 연동 + Connection Pool 을 고려한 종료
 * -
 * 문제는 해결했지만 남아있는 문제
 * 1. 서비스 계층 로직이 매우 지저분 해진다.
 * 2. 트랜잭션을 사용하기 위해 반복적인 코드가 지속적으로 발생한다.
 * 3. JDBC 의존성을 서비스 계층이 가지게 되면서 비즈니스 로직이 독립성을 잃어버리게 된다.
 */
@Slf4j
@RequiredArgsConstructor
public class MemberServiceV2 {

    private final DataSource dataSource;
    private final MemberRepositoryV2 memberRepository;

    /**
     * 트랜잭션을 시작하고 비즈니스 로직을 수행한 뒤, 비즈니스 로직 수행 결과에 따라서 트랜잭션을 커밋 or 롤백 후 종료한다
     * @param fromId
     * @param toId
     * @param money
     * @throws SQLException
     */
    public void accountTransfer(String fromId, String toId, int money) throws SQLException {
        Connection conn = dataSource.getConnection();
        try {
            conn.setAutoCommit(false); // 수동커밋모드로 전환 (= 트랜잭션을 시작한다)
            bizLogic(conn, fromId, toId, money);
            conn.commit(); // 비즈니스 로직이 정상수행 되었다면 커밋한다
        } catch (Exception e) {
            conn.rollback(); // 비즈니스 로직 수행 중 예외가 발생한 경우 롤백을 수행한다
            throw new IllegalStateException(e);
        } finally {
            release(conn); // 커넥션을 반환한다
        }
    }

    /**
     * 실질적인 계좌이체 동작을 수행하는 비즈니스 로직을 담는 메서드
     * @param conn
     * @param fromId
     * @param toId
     * @param money
     */
    private void bizLogic(Connection conn, String fromId, String toId, int money) throws SQLException {
        Member fromMember = memberRepository.findById(fromId);
        Member toMember = memberRepository.findById(toId);

        // 다음 과정이 하나의 연결된 프로세스로서 동작해야 한다.
        memberRepository.updateById(conn, fromId, fromMember.getMoney() - money);
        validation(toMember);
        memberRepository.updateById(conn, toId, toMember.getMoney() + money);
    }

    /**
     * 계좌 이체 중에 예외가 발생한 상황을 만들기 위해 작성한 메서드
     * toMember 의 ID가 'ex' 인 경우 IllegalStateException 을 일으킨다.
     * @param toMember
     */
    private void validation(Member toMember) {
        if (toMember.getMemberId().equals("ex")) {
            throw new IllegalStateException("이체 중 예외 발생");
        }
    }

    /**
     * Connection 을 종료한다.
     * @param conn
     */
    private void release(Connection conn) {
        if (conn != null) {
            try {
                /*
                 * 쓰레드풀 사용 시 커넥션은 완전히 종료되지 않고, 커넥션 풀에 반환된다.
                 * 때문에 auto commit 을 true 로 설정하지 않으면 해당 커넥션을 재사용 하는 다른 로직에서 영향을 받을 수 있다.
                 * 때문에 Connection 을 반환하기 전 다음과 같이 auto commit 을 다시 원래대로 복구시켜줘야 한다.
                 */
                conn.setAutoCommit(true); // 다시 자동커밋 모드로 변경
                conn.close();
            } catch (SQLException e) {
                log.error("release connection failed... {}", conn, e);
            }
        }
    }

}
