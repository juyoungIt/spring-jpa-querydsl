package hello.jdbc.service;

import hello.jdbc.domain.Member;
import hello.jdbc.repository.MemberRepository;
import hello.jdbc.repository.MemberRepositoryV5;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JdbcTemplate 사용
 */
@Slf4j
@SpringBootTest
class MemberServiceV5Test {

    public static final String MEMBER_A = "memberA";
    public static final String MEMBER_B = "memberB";
    public static final String MEMBER_EX = "ex";

    @Autowired
    public MemberRepository memberRepository;
    @Autowired
    public MemberServiceV4 memberService;

    @TestConfiguration
    static class TestConfig {

        @Autowired
        public DataSource dataSource;

        @Bean
        MemberRepository memberRepository() {
            return new MemberRepositoryV5(dataSource);
        }

        @Bean
        MemberServiceV4 memberServiceV4() {
            return new MemberServiceV4(memberRepository());
        }
    }

    @AfterEach
    void clearAll() throws SQLException {
        // 테스트의 지속적인 반복을 위해 member table 에 insert 된 정보를 삭제
        memberRepository.clearAll();
    }

    /**
     * Transaction AOP 를 적용한 Service 클래스의 경우 Spring 에 의해 AOP 프록시가 생성되는 반면,
     * Transaction AOP 를 적용하지 않은 Repository 클래스의 경우 AOP 프록시가 아닌 원본 인스턴스가 된다.
     */
    @Test
    @DisplayName("@Transactional AOP 를 적용한 Service 클래스가 AOP 프록시가 된다")
    void aop_proxy_test() {
        log.info("memberService = {}, class = {}", memberService, memberService.getClass());
        log.info("memberRepository = {}, class = {}", memberRepository, memberRepository.getClass());
        assertThat(AopUtils.isAopProxy(memberService)).isTrue();
        assertThat(AopUtils.isAopProxy(memberRepository)).isFalse();
    }

    @Test
    @DisplayName("정상이체 - 예외가 발생하지 않았으므로 요청한 이체가 정상처리 된다.")
    void account_transfer_success_test() {
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
    void account_transfer_fail_test() {
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