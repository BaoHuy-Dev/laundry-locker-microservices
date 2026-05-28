package com.huynqb.laundrylocker.laundry.repository;

import com.huynqb.laundrylocker.laundry.model.LaundryCatalogItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LaundryCatalogRepository extends JpaRepository<LaundryCatalogItem, Long> {

  List<LaundryCatalogItem> findByStoreId(Long storeId);

  List<LaundryCatalogItem> findByCategoryAndStatus(String category, String status);

  List<LaundryCatalogItem> findByStoreIdAndCategoryAndStatus(Long storeId, String category, String status);
}
