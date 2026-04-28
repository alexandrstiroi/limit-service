package org.shtiroy.module1.hm08.service;

import org.shtiroy.module1.hm08.dto.DefaultLimitResponse;
import org.shtiroy.module1.hm08.dto.LimitSnapshotResponse;
import org.shtiroy.module1.hm08.dto.OperationResponse;
import org.shtiroy.module1.hm08.entity.LimitOperation;
import org.shtiroy.module1.hm08.entity.LimitSettings;
import org.shtiroy.module1.hm08.entity.OperationStatus;
import org.shtiroy.module1.hm08.entity.OperationType;
import org.shtiroy.module1.hm08.entity.UserLimitAccount;
import org.shtiroy.module1.hm08.exception.ConflictException;
import org.shtiroy.module1.hm08.exception.InsufficientLimitException;
import org.shtiroy.module1.hm08.exception.NotFoundException;
import org.shtiroy.module1.hm08.repository.LimitOperationRepository;
import org.shtiroy.module1.hm08.repository.LimitSettingsRepository;
import org.shtiroy.module1.hm08.repository.UserLimitAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class LimitService {
    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final UserLimitAccountRepository userLimitAccountRepository;
    private final LimitOperationRepository limitOperationRepository;
    private final LimitSettingsRepository limitSettingsRepository;

    public LimitService(UserLimitAccountRepository userLimitAccountRepository, LimitOperationRepository limitOperationRepository, LimitSettingsRepository limitSettingsRepository) {
        this.userLimitAccountRepository = userLimitAccountRepository;
        this.limitOperationRepository = limitOperationRepository;
        this.limitSettingsRepository = limitSettingsRepository;
    }


    @Transactional
    public LimitSnapshotResponse getLimit(String userId) {
        LimitSettings settings = getSettings();
        UserLimitAccount account = getOrCreateAccountForUpdate(userId);
        applyDailyResetIfNeeded(account, settings.getDefaultLimit(), LocalDate.now(), LocalDateTime.now());
        return toSnapshot(account, settings.getDefaultLimit());
    }

    @Transactional
    public OperationResponse reserve(String userId, String operationId, BigDecimal amount) {
        BigDecimal normalizedAmount = normalizeAmount(amount);
        LimitOperation existingOperation = limitOperationRepository.findByOperationIdForUpdate(operationId).orElse(null);
        if (existingOperation != null) {
            return handleExistingOperation(existingOperation, userId, normalizedAmount, OperationType.RESERVATION, OperationStatus.RESERVED);
        }

        LimitSettings settings = getSettings();
        UserLimitAccount account = getOrCreateAccountForUpdate(userId);
        applyDailyResetIfNeeded(account, settings.getDefaultLimit(), LocalDate.now(),  LocalDateTime.now());
        ensureEnoughAvailable(account, normalizedAmount);

        LocalDateTime dateTime = LocalDateTime.now();
        account.setAvailableAmount(account.getAvailableAmount().subtract(normalizedAmount));
        account.setReservedAmount(account.getReservedAmount().add(normalizedAmount));
        account.setUpdatedAt(dateTime);

        LimitOperation operation = new LimitOperation();
        operation.setOperationId(operationId);
        operation.setAccount(account);
        operation.setAmount(normalizedAmount);
        operation.setType(OperationType.RESERVATION);
        operation.setStatus(OperationStatus.RESERVED);
        operation.setCanceledAt(dateTime);
        operation.setUpdatedAt(dateTime);

        userLimitAccountRepository.save(account);
        limitOperationRepository.save(operation);
        return toOperationResponse(operation, account, settings.getDefaultLimit());
    }

    @Transactional
    public OperationResponse confirm(String operationId) {

        LimitOperation operation = limitOperationRepository.findByOperationIdForUpdate(operationId)
                .orElseThrow(() -> new NotFoundException("Операция % не нашлась".formatted(operationId)));
        LimitSettings settings = getSettings();
        if (operation.getStatus() == OperationStatus.CONFIRMED) {
            UserLimitAccount account = getOrCreateAccountForUpdate(operation.getAccount().getUserId());
            applyDailyResetIfNeeded(account, settings.getDefaultLimit(), LocalDate.now(), LocalDateTime.now());
            return toOperationResponse(operation, account, getSettings().getDefaultLimit());
        }

        if (operation.getStatus() != OperationStatus.RESERVED || operation.getType() != OperationType.RESERVATION) {
            throw new ConflictException("Подтверждение возможно только для зарезервированного лимита");
        }

        UserLimitAccount account = getOrCreateAccountForUpdate(operation.getAccount().getUserId());
        applyDailyResetIfNeeded(account, settings.getDefaultLimit(), LocalDate.now(), LocalDateTime.now());
        LocalDateTime dateTime = LocalDateTime.now();
        account.setReservedAmount(account.getReservedAmount().subtract(operation.getAmount()));
        account.setUpdatedAt(dateTime);

        operation.setAccount(account);
        operation.setStatus(OperationStatus.CONFIRMED);
        operation.setConfirmedAt(dateTime);
        operation.setUpdatedAt(dateTime);
        userLimitAccountRepository.save(account);
        limitOperationRepository.save(operation);
        return toOperationResponse(operation, account, settings.getDefaultLimit());
    }

    @Transactional
    public OperationResponse cancel(String operationId) {
        LimitOperation operation = limitOperationRepository.findByOperationIdForUpdate(operationId)
                .orElseThrow(() -> new NotFoundException("Операция %s не найдена".formatted(operationId)));
        LimitSettings settings = getSettings();
        LocalDateTime dateTime = LocalDateTime.now();

        if (operation.getStatus() == OperationStatus.CANCELED) {
            UserLimitAccount account = getOrCreateAccountForUpdate(operation.getAccount().getUserId());
            applyDailyResetIfNeeded(account, settings.getDefaultLimit(), dateTime.toLocalDate(), dateTime);
            return toOperationResponse(operation, account, settings.getDefaultLimit());
        }

        if (operation.getStatus() != OperationStatus.RESERVED || operation.getType() != OperationType.RESERVATION) {
            throw new ConflictException("Только операции резервирования могут быть отклонены");
        }

        UserLimitAccount account = getOrCreateAccountForUpdate(operation.getAccount().getUserId());
        applyDailyResetIfNeeded(account, settings.getDefaultLimit(), dateTime.toLocalDate(), dateTime);

        account.setReservedAmount(account.getReservedAmount().subtract(operation.getAmount()));
        account.setAvailableAmount(account.getAvailableAmount().add(operation.getAmount()));
        account.setUpdatedAt(dateTime);

        operation.setAccount(account);
        operation.setStatus(OperationStatus.CANCELED);
        operation.setCanceledAt(dateTime);
        operation.setUpdatedAt(dateTime);

        userLimitAccountRepository.save(account);
        limitOperationRepository.save(operation);
        return toOperationResponse(operation, account, settings.getDefaultLimit());
    }

    @Transactional
    public OperationResponse revert(String operationId) {
        LimitOperation operation = limitOperationRepository.findByOperationIdForUpdate(operationId)
                .orElseThrow(() -> new NotFoundException("Операция %s не найдена".formatted(operationId)));

        LimitSettings settings = getSettings();
        LocalDateTime dateTime = LocalDateTime.now();

        if (operation.getStatus() == OperationStatus.REVERTED) {
            UserLimitAccount account = getOrCreateAccountForUpdate(operation.getAccount().getUserId());
            applyDailyResetIfNeeded(account, settings.getDefaultLimit(), dateTime.toLocalDate(), dateTime);
            return toOperationResponse(operation, account, settings.getDefaultLimit());
        }

        if (operation.getStatus() != OperationStatus.CONFIRMED) {
            throw new ConflictException("Только операции резервирования могут быть отклонены");
        }

        UserLimitAccount account = getOrCreateAccountForUpdate(operation.getAccount().getUserId());
        applyDailyResetIfNeeded(account, settings.getDefaultLimit(), dateTime.toLocalDate(), dateTime);

        if (shouldRestoreFunds(operation, dateTime.toLocalDate())) {
            account.setAvailableAmount(account.getAvailableAmount().add(operation.getAmount()));
        }
        account.setUpdatedAt(dateTime);

        operation.setAccount(account);
        operation.setStatus(OperationStatus.REVERTED);
        operation.setRevertedAt(dateTime);
        operation.setUpdatedAt(dateTime);

        userLimitAccountRepository.save(account);
        limitOperationRepository.save(operation);
        return toOperationResponse(operation, account, settings.getDefaultLimit());
    }

    @Transactional(readOnly = true)
    public DefaultLimitResponse getDefaultLimit() {
        LimitSettings settings = getSettings();
        return new DefaultLimitResponse(settings.getDefaultLimit(), settings.getUpdatedAt());
    }

    @Transactional
    public DefaultLimitResponse updateDefaultLimit(BigDecimal amount) {
        BigDecimal normalizedAmount = normalizeAmount(amount);
        LimitSettings settings = getSettings();
        settings.setDefaultLimit(normalizedAmount);
        settings.setUpdatedAt(LocalDateTime.now());
        limitSettingsRepository.save(settings);
        return new DefaultLimitResponse(settings.getDefaultLimit(), settings.getUpdatedAt());
    }

    @Transactional
    public int resetAllUsersToDefault() {
        LimitSettings settings = getSettings();
        return userLimitAccountRepository.resetAllToDefault(
                settings.getDefaultLimit(),
                LocalDate.now(),
                LocalDateTime.now()
        );
    }

    private UserLimitAccount getOrCreateAccountForUpdate(String userId) {
        return userLimitAccountRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> createAccount(userId));
    }

    private UserLimitAccount createAccount(String userId) {
        LimitSettings settings = getSettings();
        UserLimitAccount account = new UserLimitAccount();
        account.setUserId(userId);
        account.setAvailableAmount(settings.getDefaultLimit());
        account.setReservedAmount(ZERO);
        account.setLastResetDate(LocalDate.now());
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        try {
            return userLimitAccountRepository.saveAndFlush(account);
        } catch (DataIntegrityViolationException exception) {
            return userLimitAccountRepository.findByUserIdForUpdate(userId)
                    .orElseThrow(() -> exception);
        }
    }

    private LimitSettings getSettings() {
        return limitSettingsRepository.findById(LimitSettings.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("Лимит по умолчанию не инициализирован"));
    }

    private void applyDailyResetIfNeeded(UserLimitAccount account,
                                         BigDecimal defaultLimit,
                                         LocalDate businessDate,
                                         LocalDateTime dateTime) {
        if (account.getLastResetDate() == null || account.getLastResetDate().isBefore(businessDate)) {
            BigDecimal recalculatedAvailable = defaultLimit.subtract(account.getReservedAmount());
            if (recalculatedAvailable.compareTo(ZERO) < 0) {
                recalculatedAvailable = ZERO;
            }
            account.setAvailableAmount(recalculatedAvailable);
            account.setLastResetDate(businessDate);
            account.setUpdatedAt(dateTime);
        }
    }

    private LimitSnapshotResponse toSnapshot(UserLimitAccount account, BigDecimal defaultLimit) {
        return new LimitSnapshotResponse(
                account.getUserId(),
                account.getAvailableAmount(),
                account.getReservedAmount(),
                defaultLimit,
                account.getLastResetDate());
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Сумма должна быть указана");
        }
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Сумма должна быть положительна");
        }

        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private OperationResponse handleExistingOperation(LimitOperation existingOperation,
                                                      String userId,
                                                      BigDecimal amount,
                                                      OperationType expectedType,
                                                      OperationStatus expectedStatus) {
        if (existingOperation.getType() != expectedType || existingOperation.getStatus() != expectedStatus
                || !Objects.equals(existingOperation.getAccount().getUserId(), userId)
                || existingOperation.getAmount().compareTo(amount) != 0) {
            throw new ConflictException("Операция %s уже существует".formatted(existingOperation.getOperationId()));
        }

        UserLimitAccount account = getOrCreateAccountForUpdate(existingOperation.getAccount().getUserId());
        LimitSettings settings = getSettings();
        applyDailyResetIfNeeded(account, settings.getDefaultLimit(), LocalDate.now(), LocalDateTime.now());
        existingOperation.setAccount(account);
        return toOperationResponse(existingOperation, account, settings.getDefaultLimit());
    }

    private OperationResponse toOperationResponse(LimitOperation operation, UserLimitAccount account, BigDecimal defaultLimit) {
        return new OperationResponse(operation.getOperationId(),
                account.getUserId(),
                operation.getAmount(),
                operation.getType(),
                operation.getStatus(),
                toSnapshot(account, defaultLimit));
    }

    private void ensureEnoughAvailable(UserLimitAccount account, BigDecimal amount) {
        if (account.getAvailableAmount().compareTo(amount) < 0) {
            throw new InsufficientLimitException("Недостаточный доступный лимит для пользователя %s".formatted(account.getUserId()));
        }
    }

    private boolean shouldRestoreFunds(LimitOperation operation, LocalDate businessDate) {
        return operation.getConfirmedAt() != null
                && operation.getConfirmedAt().toLocalDate().isEqual(businessDate);
    }
}
