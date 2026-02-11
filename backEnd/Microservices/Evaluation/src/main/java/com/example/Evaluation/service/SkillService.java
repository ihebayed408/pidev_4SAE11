package com.example.Evaluation.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Evaluation.entity.Skill;
import com.example.Evaluation.repository.SkillRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;

    @Transactional(readOnly = true)
    public List<Skill> findAll() {
        return skillRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Skill findById(Long id) {
        return skillRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Skill not found with id: " + id));
    }

    @Transactional
    public Skill create(Skill skill) {
        if (skillRepository.findByName(skill.getName()).isPresent()) {
            throw new RuntimeException("Skill already exists with name: " + skill.getName());
        }
        return skillRepository.save(skill);
    }

    @Transactional
    public Skill update(Long id, Skill skillDetails) {
        Skill skill = findById(id);
        if (skillDetails.getName() != null && !skillDetails.getName().equals(skill.getName()) &&
                skillRepository.findByName(skillDetails.getName()).isPresent()) {
            throw new RuntimeException("Skill already exists with name: " + skillDetails.getName());
        }
        if (skillDetails.getName() != null) skill.setName(skillDetails.getName());
        if (skillDetails.getDomain() != null) skill.setDomain(skillDetails.getDomain());
        if (skillDetails.getDescription() != null) skill.setDescription(skillDetails.getDescription());
        return skillRepository.save(skill);
    }

    @Transactional
    public void delete(Long id) {
        if (!skillRepository.existsById(id)) {
            throw new RuntimeException("Skill not found with id: " + id);
        }
        skillRepository.deleteById(id);
    }
}
