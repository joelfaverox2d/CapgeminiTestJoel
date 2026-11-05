package com.capgemini.test.code.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.capgemini.test.code.clients.DniClient;
import com.capgemini.test.code.clients.NotificationClient;
import com.capgemini.test.code.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integración de controlador + servicio + JPA + H2.
 * Los clientes de la API externa se sustituyen por mocks.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerIntegrationTest {

  private static final String VALID_USER = """
      {
        "name": "pablo",
        "email": "email@email.com",
        "phone": "677998899",
        "rol": "admin",
        "dni": "23454234W"
      }
      """;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @MockBean
  private DniClient dniClient;

  @MockBean
  private NotificationClient notificationClient;

  @Test
  void postUsers_validUser_returns201AndStoresUser() throws Exception {
    mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(VALID_USER))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber());

    assertThat(userRepository.existsByEmail("email@email.com")).isTrue();
  }

  @Test
  void postUsers_invalidName_returns409WithErrorBody() throws Exception {
    String body = VALID_USER.replace("\"pablo\"", "\"pablito\"");

    mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(409))
        .andExpect(jsonPath("$.message").value("error validation userName"));

    assertThat(userRepository.count()).isZero();
  }

  @Test
  void getUser_afterCreatingIt_returnsUserData() throws Exception {
    String response = mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON).content(VALID_USER))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    String id = response.replaceAll("\\D", "");

    mockMvc.perform(get("/users/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("pablo"))
        .andExpect(jsonPath("$.rol").value("admin"))
        .andExpect(jsonPath("$.roomId").value(1));
  }

  @Test
  void getUser_unknownId_returns404() throws Exception {
    mockMvc.perform(get("/users/{id}", 9999))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(404));
  }

  private String createValidUser() throws Exception {
    String response = mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON).content(VALID_USER))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    return response.replaceAll("\\D", "");
  }

  @Test
  void patchRoom_existingRoom_returns200WithNewRoom() throws Exception {
    String id = createValidUser();

    mockMvc.perform(patch("/users/{id}/room", id)
            .contentType(MediaType.APPLICATION_JSON).content("{\"roomId\": 2}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.roomId").value(2));

    assertThat(userRepository.findById(Long.valueOf(id)).orElseThrow().getRoom().getId())
        .isEqualTo(2L);
  }

  @Test
  void patchRoom_roomDoesNotExist_returns404() throws Exception {
    String id = createValidUser();

    mockMvc.perform(patch("/users/{id}/room", id)
            .contentType(MediaType.APPLICATION_JSON).content("{\"roomId\": 999}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("room not found 999"));
  }

  @Test
  void patchRoom_userDoesNotExist_returns404() throws Exception {
    mockMvc.perform(patch("/users/{id}/room", 9999)
            .contentType(MediaType.APPLICATION_JSON).content("{\"roomId\": 2}"))
        .andExpect(status().isNotFound());
  }
}
