package com.huynqb.laundrylocker.loyalty.service;

import com.huynqb.laundrylocker.loyalty.dto.AdjustPointsRequest;
import com.huynqb.laundrylocker.loyalty.dto.LoyaltyAccountResponse;
import com.huynqb.laundrylocker.loyalty.dto.PointTransactionResponse;
import com.huynqb.laundrylocker.loyalty.dto.RedeemPointsRequest;
import com.huynqb.laundrylocker.loyalty.dto.RedeemStampRequest;
import com.huynqb.laundrylocker.loyalty.model.LoyaltyAccount;
import com.huynqb.laundrylocker.loyalty.model.PointTransaction;
import com.huynqb.laundrylocker.loyalty.repository.LoyaltyAccountRepository;
import com.huynqb.laundrylocker.loyalty.repository.PointTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class LoyaltyService {

  private final LoyaltyAccountRepository accountRepository;
  private final PointTransactionRepository transactionRepository;

  @Transactional
  public LoyaltyAccountResponse adjustPoints(AdjustPointsRequest request) {
    LoyaltyAccount account =
        accountRepository
            .findByUserId(request.userId())
            .orElseGet(
                () -> {
                  LoyaltyAccount created = new LoyaltyAccount();
                  created.setUserId(request.userId());
                  return created;
                });
    account.setPoints(account.getPoints() + request.points());
    account.setTier(resolveTier(account.getPoints()));

    PointTransaction transaction = new PointTransaction();
    transaction.setUserId(request.userId());
    transaction.setOrderId(request.orderId());
    transaction.setPoints(request.points());
    transaction.setType(StringUtils.hasText(request.type()) ? request.type() : "ADJUSTMENT");
    transactionRepository.save(transaction);

    return toResponse(accountRepository.save(account));
  }

  @Transactional(readOnly = true)
  public LoyaltyAccountResponse getByUser(Long userId) {
    LoyaltyAccount account = accountRepository.findByUserId(userId).orElseGet(() -> {
      LoyaltyAccount created = new LoyaltyAccount();
      created.setUserId(userId);
      return created;
    });
    return toResponse(account);
  }

  @Transactional
  public LoyaltyAccountResponse redeemPoints(RedeemPointsRequest request) {
    return adjustPoints(new AdjustPointsRequest(request.userId(), null, -Math.abs(request.points()), "REDEEM"));
  }

  @Transactional
  public LoyaltyAccountResponse redeemStamp(RedeemStampRequest request) {
    LoyaltyAccount account = accountRepository.findByUserId(request.userId()).orElseThrow(() -> new com.huynqb.laundrylocker.common.exception.NotFoundException("LoyaltyAccount", request.userId()));
    account.setStamps(Math.max(0, account.getStamps() - Math.abs(request.stamps())));
    return toResponse(accountRepository.save(account));
  }

  @Transactional
  public LoyaltyAccountResponse addStamp(Long userId, Integer count) {
    LoyaltyAccount account = accountRepository.findByUserId(userId).orElseGet(() -> {
      LoyaltyAccount created = new LoyaltyAccount();
      created.setUserId(userId);
      return created;
    });
    account.setStamps(account.getStamps() + (count == null ? 1 : count));
    return toResponse(accountRepository.save(account));
  }

  @Transactional(readOnly = true)
  public java.util.List<PointTransactionResponse> history(Long userId) {
    return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toTransaction).toList();
  }

  private String resolveTier(int points) {
    if (points >= 5000) return "PLATINUM";
    if (points >= 2000) return "GOLD";
    if (points >= 500) return "SILVER";
    return "BRONZE";
  }

  private LoyaltyAccountResponse toResponse(LoyaltyAccount account) {
    return new LoyaltyAccountResponse(account.getId(), account.getUserId(), account.getPoints(), account.getStamps(), account.getTier());
  }

  private PointTransactionResponse toTransaction(PointTransaction transaction) {
    return new PointTransactionResponse(
        transaction.getId(),
        transaction.getUserId(),
        transaction.getOrderId(),
        transaction.getPoints(),
        transaction.getType(),
        transaction.getCreatedAt());
  }
}
