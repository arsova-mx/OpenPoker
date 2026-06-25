package com.openpoker.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.openpoker.entity.CardValue;


public interface CardValueRepository extends JpaRepository<CardValue, UUID>{
}
