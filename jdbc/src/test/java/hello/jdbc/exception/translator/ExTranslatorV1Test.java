package hello.jdbc.exception.translator;

import hello.jdbc.domain.Member;
import hello.jdbc.repository.MemberRepository;
import hello.jdbc.repository.MemberRepositoryV4_1;
import hello.jdbc.repository.exception.MyDbException;
import hello.jdbc.repository.exception.MyDuplicateKeyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;

import static hello.connection.ConnectionConst.*;

/**
 * 1. SQL Error Code 를 통해 데이터베이스에 어떤 오류가 있는 지 확인할 수 있음
 * 2. 예외 변환을 통해 SQLException 을 특정 기술에 의존하지 않는 MyDuplicateKeyException 으로 변환함
 * 3. 예외 변환을 통해 특정 기술에 의존하지 않는 예외를 통해 문제를 복구하고, 서비스 계층의 순수성도 유지할 수 있게 됨
 * - 잔여문제
 * -> SQL Error Code 의 경우 각각 데이터베이스 마다 다르기 때문에 모두 대응도 어렵고 DB 종류에 따라 의존성이 발생한다
 */
public class ExTranslatorV1Test {
    Repository repository;
    Service service;
    MemberRepository memberRepository;

    @BeforeEach
    void init() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(URL, USERNAME, PASSWORD);
        repository = new Repository(dataSource);
        service = new Service(repository);
        memberRepository = new MemberRepositoryV4_1(dataSource);
    }

    @AfterEach
    void clearAll() {
        // 테스트의 반복을 위해 추가한 코드
        memberRepository.clearAll();
    }

    @Slf4j
    @RequiredArgsConstructor
    static class Service {

        private final Repository repository;

        public void create(String memberId) {
            try {
                repository.save(new Member(memberId, 0));
                log.info("saved member id = {}", memberId);
            } catch (MyDuplicateKeyException e) {
                log.info("키 중복 발생, 복구 시도");
                String retryId = generateNewId(memberId);
                log.info("retryId = {}", retryId);
                repository.save(new Member(retryId, 0));
            } catch (MyDbException e) {
                log.error("데이터 접근계층 예외");
                throw e;
            }
        }

        private String generateNewId(String memberId) {
            return memberId + new Random().nextInt(10_000);
        }
    }

    @RequiredArgsConstructor
    static class Repository {

        private final DataSource dataSource;

        public Member save(Member member) {
            String sql = "insert into member(member_id, money) values (?, ?)";

            Connection conn = null;
            PreparedStatement pstmt = null;
            try {
                conn = dataSource.getConnection();
                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, member.getMemberId());
                pstmt.setInt(2, member.getMoney());
                pstmt.executeUpdate();
                return member;
            } catch (SQLException e) {
                // h2 DB 기준으로 작성한 에러코드
                if (e.getErrorCode() == 23505) {
                    throw new MyDuplicateKeyException(e);
                }
                throw new MyDbException(e);
            } finally {
                JdbcUtils.closeStatement(pstmt);
                JdbcUtils.closeConnection(conn);
            }
        }
    }

    @Test
    @DisplayName("Repository 계층에서 올라오는 특정 예외를 잡아서 복구를 시도한다")
    void duplicate_key_save_test() {
        service.create("idA");
        service.create("idA"); // 중복된 ID로 저장시도
    }

}
