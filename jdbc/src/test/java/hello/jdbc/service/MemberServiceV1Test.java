package hello.jdbc.service;

import hello.jdbc.domain.Member;
import hello.jdbc.repository.MemberRepositoryV1;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.SQLException;

import static hello.connection.ConnectionConst.*;
import static org.assertj.core.api.Assertions.*;

/**
 * 트랜잭션 개념적용 X
 * -> 예외가 발생하는 경우에도 롤백 매커니즘이 동작하지 않는다.
 * -> 돈을 보낸 사람은 있지만 돈을 받은 사람은 없는 문제가 발생한다.
 * -> 다른 용어로 이를 데이터 무결성, 정합성이 깨졌다고 표현한다.
 */
@Slf4j
class MemberServiceV1Test {

    public static final String MEMBER_A = "memberA";
    public static final String MEMBER_B = "memberB";
    public static final String MEMBER_EX = "ex";

    public MemberRepositoryV1 memberRepository;
    public MemberServiceV1 memberService;

    @BeforeEach
    void initDateSource() {
        // 테스트 별로 Connection, Service, Repository 를 새롭게 구성한다
        DriverManagerDataSource dataSource = new DriverManagerDataSource(URL, USERNAME, PASSWORD);
        memberRepository = new MemberRepositoryV1(dataSource);
        memberService = new MemberServiceV1(memberRepository);
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
    @DisplayName("예외이체 - 예외가 발생하였으나 롤백 매커니즘이 없어 예외 발생 이전의 반영 내용들이 취소되고 반영된다")
    void account_transfer_fail_test() throws SQLException {
        // given
        Member memberA = new Member(MEMBER_A, 10_000);
        Member memberEx = new Member(MEMBER_EX, 10_000);
        memberRepository.save(memberA);
        memberRepository.save(memberEx);

        // when
        assertThatThrownBy(() -> memberService
                .accountTransfer(memberA.getMemberId(), memberEx.getMemberId(), 2_000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이체 중 예외 발생");

        // then
        Member findMemberA = memberRepository.findById(memberA.getMemberId());
        Member findMemberEx = memberRepository.findById(memberEx.getMemberId());
        assertThat(findMemberA.getMoney()).isEqualTo(8_000);
        assertThat(findMemberEx.getMoney()).isEqualTo(10_000);
    }

}