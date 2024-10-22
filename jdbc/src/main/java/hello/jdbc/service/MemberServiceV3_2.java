package hello.jdbc.service;

import hello.jdbc.domain.Member;
import hello.jdbc.repository.MemberRepositoryV3;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.SQLException;

/**
 * Transaction Template
 * Spring 이 지원하는 트랜잭션 템플릿을 통해 트랜잭션 사용 시 반복되는 패턴의 코드를 간소화 한다.
 * -
 * 이전보다 많이 개선되었지만 여전히 다음과 같은 문제가 남아있다.
 * 1. 서비스 계층에 비즈니스 로직 뿐 아니라 트랜잭션을 처리하는 기술 로직이 포함되어 있다.
 * 2. 서비스 계층이 비즈니스 로직과 트랜잭션 처리를 모두 담당하게 되며 두 책임을 가지게 된다. -> 클린코드 규칙에 위배
 * -> 여기서 한 단계 더 개선하기 위해서는 Spring 이 지원하는 AOP 의 도움이 필요하다
 */
@Slf4j
public class MemberServiceV3_2 {

    private final TransactionTemplate transactionTemplate;
    private final MemberRepositoryV3 memberRepository;

    /**
     * TransactionTemplate 을 사용하려면 TransactionManager 가 필요함
     * @param transactionManager
     * @param memberRepository
     */
    public MemberServiceV3_2(PlatformTransactionManager transactionManager, MemberRepositoryV3 memberRepository) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.memberRepository = memberRepository;
    }

    /**
     * 트랜잭션 템플릿을 적용하여 트랜잭션을 시작, 커밋 / 롤백 하는 코드가 모두 제거됨
     * 트랜잭션의 템플릿의 경우 다음 원칙으로 동작한다.
     * 1. 비즈니스 로직이 정상수행되면 커밋
     * 2. 언체크 예외가 발생하는 경우 롤백. 그 외의 경우는 커밋 (즉, 체크 예외인 경우에는 커밋한다)
     * @param fromId
     * @param toId
     * @param money
     * @throws SQLException
     */
    public void accountTransfer(String fromId, String toId, int money) {
        transactionTemplate.executeWithoutResult((status) -> {
            try {
                bizLogic(fromId, toId, money);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
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
