package com.capgemini.test.code.acceptance;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.capgemini.test.code.api.dto.ChangeRoomRequestDto;
import com.capgemini.test.code.api.dto.CreateUserRequestDto;
import com.capgemini.test.code.api.dto.ErrorResponseDto;
import com.capgemini.test.code.api.dto.IdResponseDto;
import com.capgemini.test.code.api.dto.UserResponseDto;
import com.capgemini.test.code.repository.UserRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Tests de aceptación: la aplicación arranca entera en un puerto real y se prueba
 * por HTTP, como lo haría un cliente. La API externa (check-dni, email, sms) se
 * simula con WireMock, igual que el MockServer de docker.
 * Cada test corresponde a un requisito del README.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = "external.service.url=http://localhost:${wiremock.server.port}")
@ActiveProfiles("test")
class UserAcceptanceTest {

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private WireMockServer wireMock;

  @Autowired
  private UserRepository userRepository;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
    wireMock.resetAll();

    // Mismo comportamiento que docker/mockserver-config/init.json
    wireMock.stubFor(patch(urlEqualTo("/check-dni"))
        .willReturn(okJson("{\"message\": \"Valid DNI\"}")));
    wireMock.stubFor(patch(urlEqualTo("/check-dni"))
        .withRequestBody(matchingJsonPath("$.dni", equalTo("99999999w")))
        .willReturn(aResponse().withStatus(409)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"message\": \"Invalid DNI\"}")));
    wireMock.stubFor(post(urlEqualTo("/email"))
        .willReturn(okJson("{\"message\": \"valid email\"}")));
    wireMock.stubFor(post(urlEqualTo("/sms"))
        .willReturn(okJson("{\"message\": \"valid sms\"}")));
  }

  private static CreateUserRequestDto readmeUser() {
    return new CreateUserRequestDto()
        .name("pablo")
        .email("email@email.com")
        .phone("677998899")
        .rol("admin")
        .dni("23454234W");
  }

  @Test
  void adminUser_isStoredInRoom1_returnsId_andIsNotifiedByEmail() {
    ResponseEntity<IdResponseDto> created =
        restTemplate.postForEntity("/users", readmeUser(), IdResponseDto.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    Long id = created.getBody().getId();
    assertThat(id).isNotNull();

    ResponseEntity<UserResponseDto> found =
        restTemplate.getForEntity("/users/{id}", UserResponseDto.class, id);
    assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(found.getBody().getRoomId()).isEqualTo(1L);

    wireMock.verify(1, postRequestedFor(urlEqualTo("/email"))
        .withRequestBody(matchingJsonPath("$.email", equalTo("email@email.com")))
        .withRequestBody(matchingJsonPath("$.message", equalTo("usuario guardado"))));
    wireMock.verify(0, postRequestedFor(urlEqualTo("/sms")));
  }

  @Test
  void superadminUser_isNotifiedBySms() {
    CreateUserRequestDto superadmin = readmeUser().rol("superadmin");

    ResponseEntity<IdResponseDto> created =
        restTemplate.postForEntity("/users", superadmin, IdResponseDto.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    wireMock.verify(1, postRequestedFor(urlEqualTo("/sms"))
        .withRequestBody(matchingJsonPath("$.phone", equalTo("677998899")))
        .withRequestBody(matchingJsonPath("$.message", equalTo("usuario guardado"))));
    wireMock.verify(0, postRequestedFor(urlEqualTo("/email")));
  }

  @Test
  void invalidDni_returns409_andUserIsNotStored() {
    CreateUserRequestDto invalidDni = readmeUser().dni("99999999w");

    ResponseEntity<ErrorResponseDto> response =
        restTemplate.postForEntity("/users", invalidDni, ErrorResponseDto.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody().getCode()).isEqualTo(409);
    assertThat(response.getBody().getMessage()).isEqualTo("error validation dni");
    assertThat(userRepository.count()).isZero();
  }

  @Test
  void duplicatedEmail_returns409() {
    restTemplate.postForEntity("/users", readmeUser(), IdResponseDto.class);

    CreateUserRequestDto sameEmail = readmeUser().dni("11111111A").phone("600000001");
    ResponseEntity<ErrorResponseDto> response =
        restTemplate.postForEntity("/users", sameEmail, ErrorResponseDto.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody().getMessage()).isEqualTo("error validation email");
  }

  @Test
  void invalidFields_return409WithFieldName() {
    assertValidationError(readmeUser().name("pablito"), "error validation userName");
    assertValidationError(readmeUser().email("emailemail"), "error validation email");
    assertValidationError(readmeUser().rol("user"), "error validation rol");
    assertThat(userRepository.count()).isZero();
  }

  @Test
  void unknownUser_returns404() {
    ResponseEntity<ErrorResponseDto> response =
        restTemplate.getForEntity("/users/{id}", ErrorResponseDto.class, 9999);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  private void assertValidationError(CreateUserRequestDto request, String expectedMessage) {
    ResponseEntity<ErrorResponseDto> response =
        restTemplate.postForEntity("/users", request, ErrorResponseDto.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody().getMessage()).isEqualTo(expectedMessage);
  }

  @Test
  void userChangesToExistingRoom_isMovedAndNoLongerVisibleInRoom1() {
    Long id = restTemplate.postForEntity("/users", readmeUser(), IdResponseDto.class)
        .getBody().getId();

    ResponseEntity<UserResponseDto> moved = restTemplate.exchange(
        "/users/{id}/room", HttpMethod.PATCH,
        new HttpEntity<>(new ChangeRoomRequestDto().roomId(2L)), UserResponseDto.class, id);

    assertThat(moved.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(moved.getBody().getRoomId()).isEqualTo(2L);

    // GET /users/{id} solo busca en la sala 1
    ResponseEntity<ErrorResponseDto> inRoom1 =
        restTemplate.getForEntity("/users/{id}", ErrorResponseDto.class, id);
    assertThat(inRoom1.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void userChangesToNonExistingRoom_returns404AndStaysInRoom1() {
    Long id = restTemplate.postForEntity("/users", readmeUser(), IdResponseDto.class)
        .getBody().getId();

    ResponseEntity<ErrorResponseDto> response = restTemplate.exchange(
        "/users/{id}/room", HttpMethod.PATCH,
        new HttpEntity<>(new ChangeRoomRequestDto().roomId(999L)), ErrorResponseDto.class, id);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody().getMessage()).isEqualTo("room not found 999");

    ResponseEntity<UserResponseDto> stillInRoom1 =
        restTemplate.getForEntity("/users/{id}", UserResponseDto.class, id);
    assertThat(stillInRoom1.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(stillInRoom1.getBody().getRoomId()).isEqualTo(1L);
  }
}
