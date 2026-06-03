package com.huynqb.laundrylocker.laundry.service;

import com.huynqb.laundrylocker.common.exception.NotFoundException;
import com.huynqb.laundrylocker.laundry.client.LockerClient;
import com.huynqb.laundrylocker.laundry.dto.LaundryCatalogRequest;
import com.huynqb.laundrylocker.laundry.dto.LaundryCatalogResponse;
import com.huynqb.laundrylocker.laundry.model.LaundryCatalogItem;
import com.huynqb.laundrylocker.laundry.repository.LaundryCatalogRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class LaundryCatalogService {

  private final LaundryCatalogRepository repository;
  private final LockerClient lockerClient;

  @Transactional
  public LaundryCatalogResponse create(LaundryCatalogRequest request) {
    LaundryCatalogItem item = new LaundryCatalogItem();
    apply(item, request);
    return toResponse(repository.save(item));
  }

  @Transactional(readOnly = true)
  public LaundryCatalogResponse get(Long id) {
    return toResponse(find(id));
  }

  @Transactional(readOnly = true)
  public List<LaundryCatalogResponse> list(Long storeId) {
    List<LaundryCatalogItem> items = storeId == null ? repository.findAll() : repository.findByStoreId(storeId);
    return items.stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<LaundryCatalogResponse> list(Long storeId, String category) {
    if (StringUtils.hasText(category) && storeId != null) {
      return repository.findByStoreIdAndCategoryAndStatus(storeId, category.toUpperCase(), "ACTIVE").stream().map(this::toResponse).toList();
    }
    if (StringUtils.hasText(category)) {
      return repository.findByCategoryAndStatus(category.toUpperCase(), "ACTIVE").stream().map(this::toResponse).toList();
    }
    return list(storeId);
  }

  @Transactional(readOnly = true)
  public List<LaundryCatalogResponse> listByLocker(Long lockerId, String category) {
    Long storeId = lockerClient.getLocker(lockerId).data().storeId();
    return list(storeId, category);
  }

  @Transactional
  public LaundryCatalogResponse update(Long id, LaundryCatalogRequest request) {
    LaundryCatalogItem item = find(id);
    apply(item, request);
    return toResponse(repository.save(item));
  }

  @Transactional
  public void delete(Long id) {
    repository.delete(find(id));
  }

  @Transactional
  public LaundryCatalogResponse updatePrice(Long id, BigDecimal unitPrice, BigDecimal maxPrice) {
    LaundryCatalogItem item = find(id);
    item.setUnitPrice(unitPrice == null ? BigDecimal.ZERO : unitPrice);
    item.setMaxPrice(maxPrice);
    return toResponse(repository.save(item));
  }

  @Transactional
  public LaundryCatalogResponse updateStatus(Long id, String status) {
    LaundryCatalogItem item = find(id);
    item.setStatus(status);
    return toResponse(repository.save(item));
  }

  @Transactional
  public LaundryCatalogResponse updateImage(Long id, String imageUrl) {
    LaundryCatalogItem item = find(id);
    item.setImage(imageUrl);
    return toResponse(repository.save(item));
  }

  @Transactional(readOnly = true)
  public BigDecimal estimate(List<Long> serviceIds, BigDecimal quantity) {
    BigDecimal qty = quantity == null ? BigDecimal.ONE : quantity;
    return serviceIds.stream()
        .map(this::find)
        .map(item -> item.getUnitPrice().multiply("kg".equalsIgnoreCase(item.getUnit()) ? qty : BigDecimal.ONE))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private LaundryCatalogItem find(Long id) {
    return repository.findById(id).orElseThrow(() -> new NotFoundException("LaundryCatalogItem", id));
  }

  private void apply(LaundryCatalogItem item, LaundryCatalogRequest request) {
    item.setStoreId(request.storeId());
    item.setName(request.name());
    item.setCategory(StringUtils.hasText(request.category()) ? request.category() : "LAUNDRY");
    item.setServiceType(StringUtils.hasText(request.serviceType()) ? request.serviceType() : "WASH");
    item.setUnitPrice(request.unitPrice() == null ? BigDecimal.ZERO : request.unitPrice());
    item.setMaxPrice(request.maxPrice());
    item.setUnit(StringUtils.hasText(request.unit()) ? request.unit() : "kg");
    item.setDescription(request.description());
    item.setImage(request.image());
    item.setAddon(Boolean.TRUE.equals(request.addon()));
    item.setMonthlyPackage(Boolean.TRUE.equals(request.monthlyPackage()));
    item.setEstimatedHours(request.estimatedHours());
    item.setStatus(StringUtils.hasText(request.status()) ? request.status() : "ACTIVE");
  }

  private LaundryCatalogResponse toResponse(LaundryCatalogItem item) {
    return new LaundryCatalogResponse(
        item.getId(),
        item.getStoreId(),
        item.getName(),
        item.getCategory(),
        item.getServiceType(),
        item.getUnitPrice(),
        item.getMaxPrice(),
        item.getUnit(),
        item.getDescription(),
        item.getImage(),
        item.getAddon(),
        item.getMonthlyPackage(),
        item.getEstimatedHours(),
        item.getStatus());
  }
}
