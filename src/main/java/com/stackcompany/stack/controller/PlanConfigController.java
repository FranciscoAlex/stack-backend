package com.stackcompany.stack.controller;

import com.stackcompany.stack.entity.PlanConfig;
import com.stackcompany.stack.repository.PlanConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/admin/plans")
@RequiredArgsConstructor
public class PlanConfigController {

    private final PlanConfigRepository repo;

    @GetMapping
    public ResponseEntity<List<PlanConfig>> getAll() {
        return ResponseEntity.ok(repo.findAll());
    }

    @PutMapping("/{planId}")
    public ResponseEntity<PlanConfig> update(@PathVariable String planId, @RequestBody PlanConfig body) {
        PlanConfig p = repo.findById(planId).orElseGet(() -> {
            body.setPlanId(planId);
            return body;
        });
        p.setName(body.getName());
        p.setStripePriceId(body.getStripePriceId());
        p.setDisplayPrice(body.getDisplayPrice());
        p.setOriginalPrice(body.getOriginalPrice());
        p.setDiscount(body.getDiscount());
        p.setPeriod(body.getPeriod());
        p.setPopular(body.isPopular());
        p.setExchangeRate(body.getExchangeRate());
        p.setFeatures(body.getFeatures());
        return ResponseEntity.ok(repo.save(p));
    }
}
