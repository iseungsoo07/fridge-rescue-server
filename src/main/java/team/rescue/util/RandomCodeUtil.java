package team.rescue.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class RandomCodeUtil {

	public String generateCode() {

		int digit = 6;

		try {
			Random random = SecureRandom.getInstanceStrong();
			StringBuilder code = new StringBuilder();
			for (int i = 0; i < digit; i++) {
				code.append(random.nextInt());
			}
			return code.toString();
			
		} catch (NoSuchAlgorithmException e) {
			log.error("[랜덤 코드 생성 오류]");
			throw new RuntimeException(e);
			// throw new CustomException(BusinessException.NO_SUCH_ALGORITHM)
		}
	}
}
