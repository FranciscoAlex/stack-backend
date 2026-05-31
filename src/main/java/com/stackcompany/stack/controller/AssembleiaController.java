package com.stackcompany.stack.controller;

import com.stackcompany.stack.entity.Assembleia;
import com.stackcompany.stack.repository.AssembleiaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/assembleias")
@RequiredArgsConstructor
public class AssembleiaController {

    private final AssembleiaRepository repo;

    @GetMapping
    public ResponseEntity<List<Assembleia>> getAll() {
        return ResponseEntity.ok(repo.findAll());
    }

    @PostMapping
    public ResponseEntity<Assembleia> create(@RequestBody Assembleia a) {
        return ResponseEntity.ok(repo.save(a));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Assembleia> update(@PathVariable Long id, @RequestBody Assembleia body) {
        Assembleia a = repo.findById(id).orElseThrow(() -> new NoSuchElementException("Assembleia not found: " + id));
        a.setEmpresa(body.getEmpresa());
        a.setTipoEmpresa(body.getTipoEmpresa());
        a.setDataAssembleia(body.getDataAssembleia());
        a.setHora(body.getHora());
        a.setLocal(body.getLocal());
        a.setLogoUrl(body.getLogoUrl());
        a.setNotas(body.getNotas());
        return ResponseEntity.ok(repo.save(a));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
