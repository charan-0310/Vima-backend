package com.vimainsurance.vimaadmin.service.serviceimpl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.vimainsurance.vimaadmin.audit.AuditedOperation;
import com.vimainsurance.vimaadmin.dto.AgentTargetRequestDto;
import com.vimainsurance.vimaadmin.dto.AgentTargetResponseDto;
import com.vimainsurance.vimaadmin.dto.BaseResponse;
import com.vimainsurance.vimaadmin.dto.ResponseDto;
import com.vimainsurance.vimaadmin.dto.UsernameDto;
import com.vimainsurance.vimaadmin.entity.AdminUser;
import com.vimainsurance.vimaadmin.entity.AgentTarget;
import com.vimainsurance.vimaadmin.entity.IncentivePackage;
import com.vimainsurance.vimaadmin.repository.AgentTargetRepository;
import com.vimainsurance.vimaadmin.repository.IAdminUserRepository;
import com.vimainsurance.vimaadmin.repository.IIncentivePackageRepository;
import com.vimainsurance.vimaadmin.service.AgentTargetService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AgentTargetServiceImpl implements AgentTargetService {
    private static final Logger logger = LoggerFactory.getLogger(AgentTargetServiceImpl.class);
    private final AgentTargetRepository agentTargetRepository;
    private final IIncentivePackageRepository incentivePackageRepository;
    private final IAdminUserRepository adminUserRepository;

    @Override
    @AuditedOperation(schemaName = "admin", tableName = "agent_targets", entityType = "AGENT_TARGET", action = "CREATE")
    public ResponseEntity<String> create(AgentTargetRequestDto dto) {
        try {
            Optional<IncentivePackage> pkgOpt = incentivePackageRepository.findById(dto.getPackageId());
            if (pkgOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Incentive package not found");
            }
            for (String username : dto.getAgentUsernames()) {
                Optional<AdminUser> agentOpt = adminUserRepository.findByUsername(username);
                if (agentOpt.isEmpty()) {
                    continue; // skip missing users
                }
                AgentTarget entity = new AgentTarget();
                entity.setAgent(agentOpt.get());
                entity.setMonth(dto.getMonth());
                entity.setYear(dto.getYear());
                entity.setIncentivePackage(pkgOpt.get());
                agentTargetRepository.save(entity);
            }
            return ResponseEntity.ok("Assigned successfully");
        } catch (Exception e) {
            logger.error("Error creating agent targets", e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @Override
    @AuditedOperation(schemaName = "admin", tableName = "agent_targets", entityType = "AGENT_TARGET", action = "UPDATE")
    public ResponseEntity<ResponseDto<AgentTargetResponseDto>> update(Long id, AgentTargetRequestDto dto) {
        BaseResponse<AgentTargetResponseDto> responseObj = new BaseResponse<>();
        try {
            Optional<AgentTarget> entityOpt = agentTargetRepository.findById(id);
            if (entityOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Agent target not found"));
            }
            Optional<IncentivePackage> pkgOpt = incentivePackageRepository.findById(dto.getPackageId());
            if (pkgOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Incentive package not found"));
            }
            // Use the first username in the list for update
            String username = (dto.getAgentUsernames() != null && !dto.getAgentUsernames().isEmpty()) ? dto.getAgentUsernames().get(0) : null;
            if (username == null) {
                return responseObj.render(responseObj.formErrorResponse("Agent username is required"));
            }
            Optional<AdminUser> agentOpt = adminUserRepository.findByUsername(username);
            if (agentOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Agent user not found"));
            }
            AgentTarget entity = entityOpt.get();
            entity.setAgent(agentOpt.get());
            entity.setMonth(dto.getMonth());
            entity.setYear(dto.getYear());
            entity.setIncentivePackage(pkgOpt.get());
            AgentTarget saved = agentTargetRepository.save(entity);
            return responseObj.render(responseObj.formSuccessResponse("Agent target updated"));
        } catch (Exception e) {
            logger.error("Error updating agent target", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<AgentTargetResponseDto>> getById(Long id) {
        BaseResponse<AgentTargetResponseDto> responseObj = new BaseResponse<>();
        try {
            Optional<AgentTarget> entityOpt = agentTargetRepository.findById(id);
            if (entityOpt.isEmpty()) {
                return responseObj.render(responseObj.formErrorResponse("Agent target not found"));
            }
            return responseObj.render(responseObj.formSuccessResponse("Agent target found"));
        } catch (Exception e) {
            logger.error("Error getting agent target", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<AgentTargetResponseDto>>> getAll() {
        BaseResponse<List<AgentTargetResponseDto>> responseObj = new BaseResponse<>();
        try {
            List<AgentTarget> entities = agentTargetRepository.findAll();
            List<AgentTargetResponseDto> dtos = entities.stream().map(this::toResponseDto).collect(Collectors.toList());
            return responseObj.render(responseObj.formSuccessResponse("Agent targets fetched", dtos, dtos.size()));
        } catch (Exception e) {
            logger.error("Error getting agent targets", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    @AuditedOperation(schemaName = "admin", tableName = "agent_targets", entityType = "AGENT_TARGET", action = "DELETE")
    public ResponseEntity<ResponseDto<String>> delete(Long id) {
        BaseResponse<String> responseObj = new BaseResponse<>();
        try {
            if (!agentTargetRepository.existsById(id)) {
                return responseObj.render(responseObj.formErrorResponse("Agent target not found"));
            }
            agentTargetRepository.deleteById(id);
            return responseObj.render(responseObj.formSuccessResponse("Agent target deleted", null));
        } catch (Exception e) {
            logger.error("Error deleting agent target", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<AdminUser>>> getAllAgentsByRole(String role) {
        BaseResponse<List<AdminUser>> responseObj = new BaseResponse<>();
        try {
            List<AdminUser> users = adminUserRepository.findByRole(role);
            return responseObj.render(responseObj.formSuccessResponse("Users fetched", users, users.size()));
        } catch (Exception e) {
            logger.error("Error getting users by role", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ResponseDto<List<UsernameDto>>> getAllAgentUsernamesByRole(String role) {
        BaseResponse<List<UsernameDto>> responseObj = new BaseResponse<>();
        try {
            List<UsernameDto> usernames = adminUserRepository.findByRole(role)
                .stream()
                .map(user -> new UsernameDto(user.getId(), user.getUsername(), user.getEmail()))
                .collect(Collectors.toList());
            return responseObj.render(responseObj.formSuccessResponse("Usernames fetched", usernames, usernames.size()));
        } catch (Exception e) {
            logger.error("Error getting usernames by role", e);
            return responseObj.render(responseObj.formErrorResponse(e.getMessage()));
        }
    }

    private AgentTargetResponseDto toResponseDto(AgentTarget entity) {
        AgentTargetResponseDto dto = new AgentTargetResponseDto();
        dto.setId(entity.getId());
        dto.setAgentUsername(entity.getAgent() != null ? entity.getAgent().getUsername() : null);
        dto.setMonth(entity.getMonth());
        dto.setYear(entity.getYear());
        dto.setPackageId(entity.getIncentivePackage() != null ? entity.getIncentivePackage().getId() : null);
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
} 