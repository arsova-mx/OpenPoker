package com.openpoker.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.openpoker.dto.VoteStatisticsDTO;
import com.openpoker.entity.Vote;

@Service
public class VoteStatisticsService {

    public VoteStatisticsDTO calculateStatistics(List<Vote> votes) {
        if (votes == null || votes.isEmpty()) {
            return new VoteStatisticsDTO(0.0, 0.0, false, List.of());
        }

        // 1. Filtrar solo votos numéricos válidos (weight > 0)
        List<Vote> validVotes = votes.stream()
            .filter(v -> v.getCardValue() != null 
                      && v.getCardValue().getWeight() != null 
                      && v.getCardValue().getWeight() > 0)
            .toList();

        if (validVotes.isEmpty()) {
            return new VoteStatisticsDTO(0.0, 0.0, false, List.of());
        }

        int totalValid = validVotes.size();

        // 2. Promedio
        double avgWeight = validVotes.stream()
            .mapToDouble(v -> v.getCardValue().getWeight())
            .average()
            .orElse(0.0);

        // 3. Frecuencias y Consenso
        Map<Integer, Long> frequency = validVotes.stream()
            .collect(Collectors.groupingBy(v -> v.getCardValue().getWeight(), Collectors.counting()));

        long maxFrequency = frequency.values().stream()
            .mapToLong(Long::longValue)
            .max()
            .orElse(0L);

        double consensus = ((double) maxFrequency / totalValid) * 100.0;
        boolean isFullConsensus = maxFrequency == totalValid;

        // 4. Varianza y Desviación Estándar (solo si hay más de 1 voto para evitar desvíos raros)
        double variance = validVotes.stream()
            .mapToDouble(v -> v.getCardValue().getWeight())
            .map(w -> Math.pow(w - avgWeight, 2))
            .average()
            .orElse(0.0);

        double sdeviation = Math.sqrt(variance);

        // 5. Outliers (|peso - promedio| > 1 sigma)
        // Nota: Si sdeviation es 0 (ej. todos votaron lo mismo), no habrá outliers.
        List<UUID> outlierVoteIds = (sdeviation == 0) ? List.of() : validVotes.stream()
            .filter(v -> Math.abs(v.getCardValue().getWeight() - avgWeight) > sdeviation)
            .map(Vote::getId)
            .toList();

        return new VoteStatisticsDTO(avgWeight, consensus, isFullConsensus, outlierVoteIds);
    }
}
