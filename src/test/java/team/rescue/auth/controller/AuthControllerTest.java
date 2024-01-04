package team.rescue.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import team.rescue.auth.dto.JoinDto.EmailConfirmDto;
import team.rescue.auth.dto.JoinDto.JoinReqDto;
import team.rescue.auth.dto.JoinDto.JoinResDto;
import team.rescue.auth.service.AuthService;
import team.rescue.auth.type.ProviderType;
import team.rescue.auth.type.RoleType;
import team.rescue.member.dto.MemberDto.MemberInfoDto;
import team.rescue.member.entity.Member;
import team.rescue.member.repository.MemberRepository;
import team.rescue.mock.MockMember;
import team.rescue.mock.WithMockMember;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@ActiveProfiles(profiles = "test")
@Transactional
class AuthControllerTest extends MockMember {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private MemberRepository memberRepository;

	@MockBean
	private AuthService authService;

	private Member existMember;

	@BeforeEach
	public void setup() {
		// 기존 유저
		this.existMember = memberRepository.save(
				getNewMember("test", "1234567890", ProviderType.EMAIL, RoleType.GUEST)
		);
	}

	@Test
	@DisplayName("이메일 회원 가입 - 인증 메일이 전송 되고, 인증 전까지 유저는 GUEST 권한을 갖는다.")
	public void email_join_success() throws Exception {

		// given
		JoinReqDto joinReqDto = new JoinReqDto();
		joinReqDto.setName("member");
		joinReqDto.setNickname("member");
		joinReqDto.setEmail("member@gmail.com");
		joinReqDto.setPassword("1234567890");

		String requestBody = objectMapper.writeValueAsString(joinReqDto);

		// Stub: 이메일 회원가입 정상 동작
		Member member = getNewMember(
				joinReqDto.getName(),
				joinReqDto.getPassword(),
				ProviderType.EMAIL,
				RoleType.GUEST
		);
		JoinResDto joinResDto = new JoinResDto(member);
		given(authService.createEmailUser(any(JoinReqDto.class))).willReturn(joinResDto);

		// when
		ResultActions resultActions = mockMvc.perform(
				post("/api/auth/email/join").content(requestBody)
						.contentType(MediaType.APPLICATION_JSON));

		System.out.println(resultActions.andReturn().getResponse().getContentAsString());

		// then
		// Status == 201 Created
		resultActions.andExpect(status().isCreated());
		// Response Body
		resultActions.andExpect(jsonPath("$.name").value(joinReqDto.getName()));
		resultActions.andExpect(jsonPath("$.nickname").value(joinReqDto.getNickname()));
		resultActions.andExpect(jsonPath("$.email").value(joinReqDto.getEmail()));
		resultActions.andExpect(jsonPath("$.role").value(RoleType.GUEST.name()));
	}

	@Test
	@DisplayName("이메일 인증")
	@WithMockMember
	public void confirm_email_code() throws Exception {

		// given
		EmailConfirmDto emailConfirmDto = new EmailConfirmDto();
		emailConfirmDto.setCode("123456");
		String requestBody = objectMapper.writeValueAsString(emailConfirmDto);

		// Stub: 이메일 인증 완료
		this.existMember.updateRole(RoleType.USER);
		MemberInfoDto memberInfoDto = MemberInfoDto.fromEntity(existMember);
		given(authService.confirmEmailCode(anyString(), anyString()))
				.willReturn(memberInfoDto);

		// when
		ResultActions resultActions = mockMvc.perform(
				post("/api/auth/email/confirm").content(requestBody)
						.contentType(MediaType.APPLICATION_JSON));

		System.out.println(resultActions.andReturn().getResponse().getContentAsString());

		// then
		// Status == 200 OK
		resultActions.andExpect(status().isOk());
		resultActions.andExpect(jsonPath("$.id").value(existMember.getId()));
		resultActions.andExpect(jsonPath("$.nickname").value(existMember.getNickname()));
		// Role == USER
		resultActions.andExpect(jsonPath("$.role").value(RoleType.USER.name()));
	}
}
