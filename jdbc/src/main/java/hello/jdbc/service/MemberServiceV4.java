package hello.jdbc.service;

import hello.jdbc.domain.Member;
import hello.jdbc.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;

/**
 * 해당 버전에서는 다음과 같은 개선 사항들을 포함한다
 * 1. 예외 누수문제 해결
 * 2. SQLException 과 같은 특정 기술에 종속된 예외로 발생하는 종속성 제거
 * 3. MemberRepository 인터페이스 의존
 */
@Slf4j
@RequiredArgsConstructor
public class MemberServiceV4 {

    private final MemberRepository memberRepository;

    /**
     * Transactional AOP 를 적용했다. 어노테이션으로 간단하게 적용할 수 있다.
     * -> 이렇게 함으로써 서비스 계층에는 순수한 비즈니스 로직만 남게 된다.
     * -> 기존에 있던 트랜잭션 처리 코드는 AOP 프록시에 위임했다.
     * -> 해당 어노테이션의 경우 메서드나 클래스에 모두 적용가능하며, 클래스에 적용하는 경우 클래스 내 모든 public 메서드가 적용대상이 된다.
     * -> 이러한 방식의 트랜잭션 관리 방식을 '선언적 트랜잭션 관리' 라고 표현한다.
     * @param fromId
     * @param toId
     * @param money
     * @throws SQLException
     */
    @Transactional
    public void accountTransfer(String fromId, String toId, int money) {
        bizLogic(fromId, toId, money);
    }

    /**
     * 실질적인 계좌이체 동작을 수행하는 비즈니스 로직을 담는 메서드
     * @param fromId
     * @param toId
     * @param money
     */
    private void bizLogic(String fromId, String toId, int money) {
        Member fromMember = memberRepository.findById(fromId);
        Member toMember = memberRepository.findById(toId);

        // 다음 과정이 하나의 연결된 프로세스로서 동작해야 한다.
        memberRepository.updateById(fromId, fromMember.getMoney() - money);
        validation(toMember);
        memberRepository.updateById(toId, toMember.getMoney() + money);
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

}
