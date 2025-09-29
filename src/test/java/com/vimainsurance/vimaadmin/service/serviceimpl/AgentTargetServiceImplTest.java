// package com.vimainsurance.vimaadmin.service.serviceimpl;

// import java.time.OffsetDateTime;
// import java.util.Arrays;
// import java.util.List;
// import java.util.Optional;

// import static org.assertj.core.api.Assertions.assertThat;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.BDDMockito.given;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import static org.mockito.Mockito.doNothing;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.http.ResponseEntity;

// import com.vimainsurance.vimaadmin.dto.AgentTargetRequestDto;
// import com.vimainsurance.vimaadmin.dto.AgentTargetResponseDto;
// import com.vimainsurance.vimaadmin.dto.ResponseDto;
// import com.vimainsurance.vimaadmin.entity.AgentTarget;
// import com.vimainsurance.vimaadmin.entity.IncentivePackage;
// import com.vimainsurance.vimaadmin.repository.AgentTargetRepository;
// import com.vimainsurance.vimaadmin.repository.IIncentivePackageRepository;

// @ExtendWith(MockitoExtension.class)
// class AgentTargetServiceImplTest {
//     @Mock
//     AgentTargetRepository agentTargetRepository;
//     @Mock
//     IIncentivePackageRepository incentivePackageRepository;
//     @InjectMocks
//     AgentTargetServiceImpl service;

//     IncentivePackage pkg;
//     AgentTargetRequestDto requestDto;
//     AgentTarget entity;

//     @BeforeEach
//     void setup() {
//         pkg = new IncentivePackage();
//         pkg.setId(1L);
//         requestDto = new AgentTargetRequestDto();
//         requestDto.setAgentId(10L);
//         requestDto.setMonth(7);
//         requestDto.setYear(2024);
//         requestDto.setTargetCount(5);
//         requestDto.setPackageId(1L);
//         entity = new AgentTarget();
//         entity.setId(100L);
//         entity.setAgentId(10L);
//         entity.setMonth(7);
//         entity.setYear(2024);
//         entity.setTargetCount(5);
//         entity.setIncentivePackage(pkg);
//         entity.setCreatedAt(OffsetDateTime.now());
//     }

//     @Test
//     void create_success() {
//         given(incentivePackageRepository.findById(1L)).willReturn(Optional.of(pkg));
//         given(agentTargetRepository.save(any())).willReturn(entity);
//         ResponseEntity<ResponseDto<AgentTargetResponseDto>> response = service.create(requestDto);
//         assertThat(response.getBody().getPayload()).isNotNull();
//         assertThat(response.getBody().getPayload().getAgentId()).isEqualTo(10L);
//         assertThat(response.getBody().getErrorCode()).isNull();
//     }

//     @Test
//     void create_packageNotFound() {
//         given(incentivePackageRepository.findById(1L)).willReturn(Optional.empty());
//         ResponseEntity<ResponseDto<AgentTargetResponseDto>> response = service.create(requestDto);
//         assertThat(response.getBody().getPayload()).isNull();
//         assertThat(response.getBody().getErrorCode()).isNotNull();
//         assertThat(response.getBody().getMessage()).contains("not found");
//     }

//     @Test
//     void update_success() {
//         given(agentTargetRepository.findById(100L)).willReturn(Optional.of(entity));
//         given(incentivePackageRepository.findById(1L)).willReturn(Optional.of(pkg));
//         given(agentTargetRepository.save(any())).willReturn(entity);
//         ResponseEntity<ResponseDto<AgentTargetResponseDto>> response = service.update(100L, requestDto);
//         assertThat(response.getBody().getPayload()).isNotNull();
//         assertThat(response.getBody().getPayload().getAgentId()).isEqualTo(10L);
//         assertThat(response.getBody().getErrorCode()).isNull();
//     }

//     @Test
//     void update_notFound() {
//         given(agentTargetRepository.findById(100L)).willReturn(Optional.empty());
//         ResponseEntity<ResponseDto<AgentTargetResponseDto>> response = service.update(100L, requestDto);
//         assertThat(response.getBody().getPayload()).isNull();
//         assertThat(response.getBody().getErrorCode()).isNotNull();
//     }

//     @Test
//     void getById_success() {
//         given(agentTargetRepository.findById(100L)).willReturn(Optional.of(entity));
//         ResponseEntity<ResponseDto<AgentTargetResponseDto>> response = service.getById(100L);
//         assertThat(response.getBody().getPayload()).isNotNull();
//         assertThat(response.getBody().getPayload().getId()).isEqualTo(100L);
//         assertThat(response.getBody().getErrorCode()).isNull();
//     }

//     @Test
//     void getById_notFound() {
//         given(agentTargetRepository.findById(100L)).willReturn(Optional.empty());
//         ResponseEntity<ResponseDto<AgentTargetResponseDto>> response = service.getById(100L);
//         assertThat(response.getBody().getPayload()).isNull();
//         assertThat(response.getBody().getErrorCode()).isNotNull();
//     }

//     @Test
//     void getAll_success() {
//         AgentTarget entity2 = new AgentTarget();
//         entity2.setId(101L);
//         entity2.setAgentId(11L);
//         entity2.setMonth(8);
//         entity2.setYear(2024);
//         entity2.setTargetCount(6);
//         entity2.setIncentivePackage(pkg);
//         entity2.setCreatedAt(OffsetDateTime.now());
//         given(agentTargetRepository.findAll()).willReturn(Arrays.asList(entity, entity2));
//         ResponseEntity<ResponseDto<List<AgentTargetResponseDto>>> response = service.getAll();
//         assertThat(response.getBody().getPayload()).hasSize(2);
//         assertThat(response.getBody().getErrorCode()).isNull();
//     }

//     @Test
//     void delete_success() {
//         given(agentTargetRepository.existsById(100L)).willReturn(true);
//         doNothing().when(agentTargetRepository).deleteById(100L);
//         ResponseEntity<ResponseDto<String>> response = service.delete(100L);
//         assertThat(response.getBody().getErrorCode()).isNull();
//         assertThat(response.getBody().getMessage()).contains("deleted");
//     }

//     @Test
//     void delete_notFound() {
//         given(agentTargetRepository.existsById(100L)).willReturn(false);
//         ResponseEntity<ResponseDto<String>> response = service.delete(100L);
//         assertThat(response.getBody().getErrorCode()).isNotNull();
//         assertThat(response.getBody().getPayload()).isNull();
//     }
// } 