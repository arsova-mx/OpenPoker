package com.openpoker.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.openpoker.dto.VoteStatisticsDTO;
import com.openpoker.entity.Vote;
import com.openpoker.globalexception.EmptyListException;
import com.openpoker.repository.CardValueRepository;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class VoteStatisticsService {

    private final CardValueRepository cardValueRepository;

    public VoteStatisticsDTO calculateStatistics(List<Vote> votes){
        if(votes.isEmpty()){
            throw new EmptyListException("Lista de votos vacia");
        }
        
        double avgWeight= votes.stream()
            .mapToDouble(v -> v.getCardValue().getWeight())
            .filter(w -> w > 0)
            .average()
            .orElse(0.0);
        
    }




}
