package com.stackcompany.stack.controller;

import com.stackcompany.stack.entity.ShortVideo;
import com.stackcompany.stack.repository.ShortVideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/videos/admin")
@RequiredArgsConstructor
public class VideoController {

    private final ShortVideoRepository repo;

    @GetMapping("/{pageKey}")
    public ResponseEntity<List<ShortVideo>> getByPage(@PathVariable String pageKey) {
        return ResponseEntity.ok(repo.findByPageKeyOrderByDisplayOrderAsc(pageKey));
    }

    @PostMapping
    public ResponseEntity<ShortVideo> create(@RequestBody ShortVideo video) {
        return ResponseEntity.ok(repo.save(video));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShortVideo> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        ShortVideo v = repo.findById(id).orElseThrow(() -> new NoSuchElementException("Video not found: " + id));
        if (body.containsKey("title"))        v.setTitle((String) body.get("title"));
        if (body.containsKey("description"))  v.setDescription((String) body.get("description"));
        if (body.containsKey("videoUrl"))     v.setVideoUrl((String) body.get("videoUrl"));
        if (body.containsKey("thumbnailUrl")) v.setThumbnailUrl((String) body.get("thumbnailUrl"));
        if (body.containsKey("sourceType"))   v.setSourceType((String) body.get("sourceType"));
        if (body.containsKey("displayOrder")) v.setDisplayOrder(((Number) body.get("displayOrder")).intValue());
        if (body.containsKey("enabled"))      v.setEnabled((Boolean) body.get("enabled"));
        return ResponseEntity.ok(repo.save(v));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
