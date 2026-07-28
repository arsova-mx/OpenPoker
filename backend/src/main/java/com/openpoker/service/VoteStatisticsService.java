package com.openpoker.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        
        Map<Integer,Long> frecuency = votes.stream()
            .collect(Collectors.groupingBy(v -> v.getCardValue().getWeight(),Collectors.counting()));

        Long maxFrecuency = frecuency.values().stream()
            .mapToLong(Long::longValue)
            .max()
            .orElse(0);
        
        Double consensus = ((double)maxFrecuency / votes.size()) * 100.0;

        
        
        
        

        
        
    }




}
