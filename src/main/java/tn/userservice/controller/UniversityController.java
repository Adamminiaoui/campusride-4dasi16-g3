package tn.userservice.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tn.userservice.entity.University;
import tn.userservice.repository.UniversityRepository;

import java.util.List;

@RestController
@RequestMapping("/api/universities")
public class UniversityController {

    private final UniversityRepository repository;

    public UniversityController(UniversityRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<University> findAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public University findOne(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Université introuvable"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public University create(@Valid @RequestBody University body) {
        body.setId(null);
        return repository.save(body);
    }

    @PutMapping("/{id}")
    public University update(@PathVariable Long id, @Valid @RequestBody University body) {
        University existing = findOne(id);
        existing.setName(body.getName());
        existing.setCity(body.getCity());
        return repository.save(existing);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Université introuvable");
        }
        repository.deleteById(id);
    }
}