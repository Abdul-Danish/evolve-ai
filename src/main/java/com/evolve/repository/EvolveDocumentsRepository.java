package com.evolve.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.evolve.model.EvolveDocument;

@Repository
public interface EvolveDocumentsRepository extends JpaRepository<EvolveDocument, Long> {

}
