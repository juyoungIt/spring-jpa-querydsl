package hello.jdbc.service;

import hello.jdbc.domain.Member;
import hello.jdbc.repository.MemberRepositoryV3;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.sql.SQLException;

/**
 * Spring 이 지원하는 트랜잭션 매니저 (PlatformTransactionManager) 도입
 */
@Slf4j
@RequiredArgsConstructor
public class MemberServiceV3_1 {

    private final PlatformTransactionManager transactionManager;
    private final MemberRepositoryV3 memberRepository;

    /**
     * 트랜잭션을 시작하고 비즈니스 로직을 수행한 뒤, 비즈니스 로직 수행 결과에 따라서 트랜잭션을 커밋 or 롤백 후 종료한다
     * @param fromId
     * @param toId
     * @param money
     * @throws SQLException
     */
    public void accountTransfer(String fromId, String toId, int money) {
        // 트랜잭션 시작
        // -> TransactionStatus 는 현재 트랜잭션의 상태에 대한 정보를 포함한다.
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            bizLogic(fromId, toId, money);
            transactionManager.commit(status);
        } catch (Exception e) {
            transactionManager.rollback(status);
            throw new IllegalStateException(e);
        }
    }

    /**
     * 실질적인 계좌이체 동작을 수행하는 비즈니스 로직을 담는 메서드
     * @param fromId
     * @param toId
     * @param money
     */
    private void bizLogic(String fromId, String toId, int money) throws SQLException {
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
