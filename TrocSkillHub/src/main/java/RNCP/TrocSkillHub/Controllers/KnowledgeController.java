package RNCP.TrocSkillHub.Controllers;

import RNCP.TrocSkillHub.DTOs.KnowledgeDTO;
import RNCP.TrocSkillHub.Services.ImplServices.KnowledgeService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/knowledges")
public class KnowledgeController {
    
    private final KnowledgeService knowledgeService;
    
    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }
    
    // GET /knowledges - List all knowledges
    @GetMapping
    public ResponseEntity<List<KnowledgeDTO>> getAllKnowledges() {
        List<KnowledgeDTO> knowledges = knowledgeService.getAllKnowledges();
        return ResponseEntity.ok(knowledges);
    }
    
    // GET /knowledges/{id} - Get a knowledge by id
    @GetMapping("/{id}")
    public ResponseEntity<KnowledgeDTO> getKnowledgeById(@PathVariable Long id) {
        KnowledgeDTO knowledge = knowledgeService.getKnowledgeById(id);
        return ResponseEntity.ok(knowledge);
    }
    
    // POST /knowledges - Create a new knowledge
    @PostMapping
    public ResponseEntity<KnowledgeDTO> createKnowledge(@Valid @RequestBody KnowledgeDTO knowledgeDTO) {
        KnowledgeDTO created = knowledgeService.createKnowledge(knowledgeDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    // PUT /knowledges/{id} - Update an existing knowledge
    @PutMapping("/{id}")
    public ResponseEntity<KnowledgeDTO> updateKnowledge(
            @PathVariable Long id, 
            @Valid @RequestBody KnowledgeDTO knowledgeDTO) {
        KnowledgeDTO updated = knowledgeService.updateKnowledge(id, knowledgeDTO);
        return ResponseEntity.ok(updated);
    }
    
    // DELETE /knowledges/{id} - Delete a knowledge
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteKnowledge(@PathVariable Long id) {
        knowledgeService.deleteKnowledge(id);
        return ResponseEntity.noContent().build();
    }
}