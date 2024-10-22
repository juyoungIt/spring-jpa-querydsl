package hello.jdbc.service;

import hello.jdbc.domain.Member;
import hello.jdbc.repository.MemberRepositoryV3;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;

import java.sql.SQLException;

import static hello.connection.ConnectionConst.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Spring 이 지원하는 TransactionTemplate 을 서비스 계층에 적용
 * -> 이를 통해 트랜잭션 사용 시 반복적으로 등장하는 트랜잭션 시작, 커밋 / 롤백 코드를 제거했다.
 */
@Slf4j
class MemberServiceV3_2Test {

    public static final String MEMBER_A = "memberA";
    public static final String MEMBER_B = "memberB";
    public static final String MEMBER_EX = "ex";

    public MemberRepositoryV3 memberRepository;
    public MemberServiceV3_2 memberService;

    @BeforeEach
    void initDateSource() {
        // 테스트 별로 Connection, Service, Repository 를 새롭게 구성한다
        DriverManagerDataSource dataSource = new DriverManagerDataSource(URL, USERNAME, PASSWORD);
        PlatformTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
        memberRepository = new MemberRepositoryV3(dataSource);
        memberService = new MemberServiceV3_2(transactionManager, memberRepository);
    }

    @AfterEach
    void clearAll() throws SQLException {
        // 테스트의 지속적인 반복을 위해 member table 에 insert 된 정보를 삭제
        memberRepository.clearAll();
    }

    @Test
    @DisplayName("정상이체 - 예외가 발생하지 않았으므로 요청한 이체가 정상처리 된다.")
    void account_transfer_success_test() throws SQLException {
        // given
        Member memberA = new Member(MEMBER_A, 10_000);
        Member memberB = new Member(MEMBER_B, 10_000);
        memberRepository.save(memberA);
        memberRepository.save(memberB);

        // when
        memberService.accountTransfer(memberA.getMemberId(), memberB.getMemberId(), 2_000);

        // then
        Member findMemberA = memberRepository.findById(memberA.getMemberId());
        Member findMemberB = memberRepository.findById(memberB.getMemberId());
        assertThat(findMemberA.getMoney()).isEqualTo(8_000);
        assertThat(findMemberB.getMoney()).isEqualTo(12_000);
    }

    @Test
    @DisplayName("예외이체 - 예외가 발생하여 롤백 매커니즘이 동작하며 송금이전 상태로 돌아간다")
    void account_transfer_fail_test() throws SQLException {
        // given
        Member memberA = new Member(MEMBER_A, 10_000);
        Member memberEx = new Member(MEMBER_EX, 10_000);
        memberRepository.save(memberA);
        memberRepository.save(memberEx);

        // when
        assertThatThrownBy(() -> memberService
                .accountTransfer(memberA.getMemberId(), memberEx.getMemberId(), 2_000))
                .isInstanceOf(IllegalStateException.class);

        // then
        Member findMemberA = memberRepository.findById(memberA.getMemberId());
        Member findMemberEx = memberRepository.findById(memberEx.getMemberId());
        assertThat(findMemberA.getMoney()).isEqualTo(10_000);
        assertThat(findMemberEx.getMoney()).isEqualTo(10_000);
    }

}