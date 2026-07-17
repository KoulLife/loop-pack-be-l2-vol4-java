package com.loopers.application.ranking;

import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.ranking.RankingEntry;
import com.loopers.domain.ranking.RankingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class RankingApplicationService {

    private final RankingRepository rankingRepository;
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;

    @Transactional(readOnly = true)
    public List<RankingInfo> getRankings(LocalDate date, int page, int size) {
        List<RankingEntry> entries = rankingRepository.getRankings(date, page, size);
        if (entries.isEmpty()) {
            return List.of();
        }

        List<Long> productIds = entries.stream().map(RankingEntry::productId).toList();
        Map<Long, ProductModel> productsById = productRepository.findAllByIds(productIds).stream()
            .collect(Collectors.toMap(ProductModel::getId, p -> p));

        List<Long> brandIds = productsById.values().stream().map(ProductModel::getBrandId).distinct().toList();
        Map<Long, BrandModel> brandsById = brandRepository.findAllByIds(brandIds);

        return entries.stream()
            .filter(entry -> productsById.containsKey(entry.productId()))
            .map(entry -> {
                ProductModel product = productsById.get(entry.productId());
                BrandModel brand = brandsById.get(product.getBrandId());
                return new RankingInfo(
                    entry.rank(),
                    product.getId(),
                    product.getName(),
                    brand != null ? brand.getName() : null,
                    product.getPrice(),
                    entry.score()
                );
            })
            .toList();
    }
}
